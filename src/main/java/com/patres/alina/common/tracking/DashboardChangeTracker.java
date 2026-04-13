package com.patres.alina.common.tracking;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.patres.alina.common.event.ChatNotificationEvent;
import com.patres.alina.common.event.Event;
import com.patres.alina.common.storage.AppPaths;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * Orchestrates dashboard change tracking across all sections.
 * <p>
 * On each widget refresh the widget calls
 * {@link #trackChanges(DashboardSection, List, Function)} with its current items.
 * The tracker compares them against the previously saved snapshot, publishes a single
 * {@link ChatNotificationEvent} per section if anything changed, and persists the
 * updated snapshot to {@code system/dashboard-state.json}.
 * <p>
 * Thread-safe: multiple widgets may call {@code trackChanges} concurrently from
 * their own virtual threads.
 */
public final class DashboardChangeTracker {

    private static final Logger logger = LoggerFactory.getLogger(DashboardChangeTracker.class);
    private static final String STATE_FILE = "system/dashboard-state.json";

    private static final DashboardChangeTracker INSTANCE = new DashboardChangeTracker();
    private static final ObjectMapper MAPPER = createMapper();

    private final Object lock = new Object();
    private DashboardStateSnapshot currentSnapshot;
    private boolean loaded = false;

    private DashboardChangeTracker() {
    }

    public static DashboardChangeTracker getInstance() {
        return INSTANCE;
    }

    /**
     * Tracks changes for a dashboard section.
     * <p>
     * Converts the raw domain items to {@link TrackableItem} using the supplied adapter,
     * detects changes against the last snapshot, publishes a notification if changes exist
     * and notifications are enabled, and saves the updated snapshot.
     * <p>
     * The snapshot is <em>only</em> updated when {@code fetchError} is {@code false}.
     * When a fetch error occurs the previous snapshot is preserved so that items which
     * were visible before the error are not treated as "new" once connectivity is restored.
     *
     * @param section              the dashboard section being refreshed
     * @param items                the current list of domain-specific items (e.g. {@code JiraIssue})
     * @param adapter              converts a domain item to a {@link TrackableItem}
     * @param notificationsEnabled whether to actually publish a chat notification on change
     * @param fetchError           {@code true} when the upstream API call failed; snapshot is
     *                             not updated in this case to prevent false "new item" notifications
     * @param <T>                  the domain item type
     */
    public <T> void trackChanges(
            final DashboardSection section,
            final List<T> items,
            final Function<T, TrackableItem> adapter,
            final boolean notificationsEnabled,
            final boolean fetchError) {

        if (fetchError) {
            logger.warn("Dashboard section {} fetch failed — skipping snapshot update to avoid false notifications",
                    section.id());
            return;
        }

        final List<TrackableItem> currentItems = items.stream()
                .map(adapter)
                .toList();

        synchronized (lock) {
            ensureLoaded();

            final List<TrackableItem> previousItems = currentSnapshot.sections()
                    .getOrDefault(section.id(), Collections.emptyList());

            final ChangeReport report = ChangeDetector.detect(previousItems, currentItems);

            if (report.hasChanges() && notificationsEnabled) {
                final String message = ChangeNotificationFormatter.format(section, report);
                logger.info("Dashboard changes detected in {}: {} added, {} modified, {} removed",
                        section.id(), report.added().size(), report.modified().size(), report.removed().size());
                Event.publish(new ChatNotificationEvent(message));
            }

            // Update snapshot with the new state for this section
            currentSnapshot.sections().put(section.id(), currentItems);
            persistSnapshot();
        }
    }

    private void ensureLoaded() {
        if (!loaded) {
            currentSnapshot = loadSnapshot();
            loaded = true;
        }
    }

    private DashboardStateSnapshot loadSnapshot() {
        try {
            final Path path = AppPaths.resolve(STATE_FILE);
            final File file = path.toFile();
            if (file.exists()) {
                final String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                final DashboardStateSnapshot snapshot = MAPPER.readValue(json, DashboardStateSnapshot.class);
                logger.info("Dashboard state snapshot loaded from {}", file.getAbsolutePath());
                return snapshot;
            }
        } catch (final Exception e) {
            logger.warn("Failed to load dashboard state snapshot, starting fresh", e);
        }
        return new DashboardStateSnapshot();
    }

    private void persistSnapshot() {
        try {
            final Path path = AppPaths.resolve(STATE_FILE);
            final File file = path.toFile();
            FileUtils.createParentDirectories(file);
            final String json = MAPPER.writeValueAsString(currentSnapshot);
            FileUtils.write(file, json, StandardCharsets.UTF_8);
            logger.debug("Dashboard state snapshot saved to {}", file.getAbsolutePath());
        } catch (final Exception e) {
            logger.warn("Failed to persist dashboard state snapshot", e);
        }
    }

    private static ObjectMapper createMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}
