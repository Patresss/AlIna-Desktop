package com.patres.alina.uidesktop.ui;

import com.patres.alina.server.integration.GoogleCalendarEvent;
import com.patres.alina.server.integration.GoogleCalendarResult;
import com.patres.alina.server.integration.GoogleCalendarService;
import com.patres.alina.uidesktop.backend.BackendApi;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Header bar leading component displaying the nearest calendar event with a live countdown.
 * Shows: "<truncated name> · pozostało X min" or "<truncated name> · za X min"
 * Events under 5 minutes get the "urgent" visual treatment.
 */
public class HeaderEventCountdown extends HBox {

    private static final int MAX_SUMMARY_CHARS = 48;
    private static final int URGENT_THRESHOLD_MINUTES = 5;

    private static final String STYLE_CONTAINER = "header-event-countdown";
    private static final String STYLE_SUMMARY = "header-event-countdown-summary";
    private static final String STYLE_TIME = "header-event-countdown-time";
    private static final String STYLE_URGENT = "header-event-countdown-urgent";

    private final Label summaryLabel = new Label();
    private final Label timeLabel = new Label();

    /** Cached events list – refreshed every dashboardCalendarRefreshSeconds */
    private volatile List<GoogleCalendarEvent> cachedEvents = List.of();

    private Timeline dataRefreshTimeline;
    private Timeline tickTimeline;

    public HeaderEventCountdown() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(6);
        getStyleClass().add(STYLE_CONTAINER);

        summaryLabel.getStyleClass().add(STYLE_SUMMARY);
        summaryLabel.setMinWidth(0);

        timeLabel.getStyleClass().add(STYLE_TIME);
        timeLabel.setMinWidth(0);

        getChildren().addAll(summaryLabel, timeLabel);

        // Start invisible until we have data
        setVisible(false);
        setManaged(false);

        startDataRefresh();
        startTickTimer();
    }

    // ── Data refresh (same interval as calendar widget) ──────────

    private void startDataRefresh() {
        fetchAsync();

        final int seconds = BackendApi.getWorkspaceSettings().dashboardCalendarRefreshSeconds();
        dataRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(seconds), e -> fetchAsync())
        );
        dataRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        dataRefreshTimeline.play();
    }

    private void fetchAsync() {
        Thread.startVirtualThread(() -> {
            final GoogleCalendarResult result = GoogleCalendarService.fetchTodayEvents();
            if (!result.authError() && result.errorMessage().isEmpty()) {
                cachedEvents = result.events();
            } else {
                cachedEvents = List.of();
            }
            Platform.runLater(this::tick);
        });
    }

    // ── Tick every 30 seconds to update the countdown label ──────

    private void startTickTimer() {
        tickTimeline = new Timeline(
                new KeyFrame(Duration.seconds(30), e -> tick())
        );
        tickTimeline.setCycleCount(Animation.INDEFINITE);
        tickTimeline.play();
    }

    // ── Core display logic ───────────────────────────────────────

    private void tick() {
        final Optional<EventInfo> best = findBestEvent();
        if (best.isEmpty()) {
            setVisible(false);
            setManaged(false);
            return;
        }

        final EventInfo info = best.get();
        final String truncated = truncate(info.event.summary());
        summaryLabel.setText(truncated);

        final boolean urgent;
        final String timeText;

        if (info.remainingMinutes >= 0) {
            // Event is currently running
            urgent = info.remainingMinutes < URGENT_THRESHOLD_MINUTES;
            timeText = formatRemaining(info.remainingMinutes);
        } else {
            // Event hasn't started yet
            urgent = info.minutesUntilStart < URGENT_THRESHOLD_MINUTES;
            timeText = formatUpcoming(info.minutesUntilStart);
        }

        timeLabel.setText(timeText);

        if (urgent) {
            if (!summaryLabel.getStyleClass().contains(STYLE_URGENT)) {
                summaryLabel.getStyleClass().add(STYLE_URGENT);
            }
            if (!timeLabel.getStyleClass().contains(STYLE_URGENT)) {
                timeLabel.getStyleClass().add(STYLE_URGENT);
            }
        } else {
            summaryLabel.getStyleClass().remove(STYLE_URGENT);
            timeLabel.getStyleClass().remove(STYLE_URGENT);
        }

        setVisible(true);
        setManaged(true);
    }

    // ── Event selection ──────────────────────────────────────────

    private Optional<EventInfo> findBestEvent() {
        final List<GoogleCalendarEvent> events = cachedEvents;
        if (events == null || events.isEmpty()) {
            return Optional.empty();
        }

        // First priority: currently running events (pick the one ending soonest)
        final Optional<EventInfo> current = events.stream()
                .filter(e -> !e.allDay())
                .map(e -> new EventInfo(e, getRemainingMinutes(e), getMinutesUntilStart(e)))
                .filter(info -> info.remainingMinutes >= 0)
                .min(Comparator.comparingLong(info -> info.remainingMinutes));

        if (current.isPresent()) {
            return current;
        }

        // Second priority: next upcoming event (any future event today, no cap)
        return events.stream()
                .filter(e -> !e.allDay())
                .map(e -> new EventInfo(e, getRemainingMinutes(e), getMinutesUntilStart(e)))
                .filter(info -> info.minutesUntilStart >= 0)
                .min(Comparator.comparingLong(info -> info.minutesUntilStart));
    }

    // ── Time calculations ────────────────────────────────────────

    private long getRemainingMinutes(final GoogleCalendarEvent event) {
        if (event.allDay()) return -1;
        final String rawStart = event.rawStartDateTime();
        final String rawEnd = event.rawEndDateTime();
        if (isBlank(rawStart) || isBlank(rawEnd)) return -1;
        try {
            final OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
            final OffsetDateTime start = OffsetDateTime.parse(rawStart);
            final OffsetDateTime end = OffsetDateTime.parse(rawEnd);
            if (now.isBefore(start) || !now.isBefore(end)) return -1;
            return ChronoUnit.MINUTES.between(now, end);
        } catch (final Exception e) {
            return -1;
        }
    }

    private long getMinutesUntilStart(final GoogleCalendarEvent event) {
        if (event.allDay()) return -1;
        final String rawStart = event.rawStartDateTime();
        if (isBlank(rawStart)) return -1;
        try {
            final OffsetDateTime now = OffsetDateTime.now(ZoneId.systemDefault());
            final OffsetDateTime start = OffsetDateTime.parse(rawStart);
            if (!now.isBefore(start)) return -1;
            return ChronoUnit.MINUTES.between(now, start);
        } catch (final Exception e) {
            return -1;
        }
    }

    // ── Text helpers ─────────────────────────────────────────────

    private String truncate(final String text) {
        if (text == null) return "";
        if (text.length() <= MAX_SUMMARY_CHARS) return text;
        return text.substring(0, MAX_SUMMARY_CHARS) + "…";
    }

    private String formatRemaining(final long minutes) {
        if (minutes <= 0) return "· kończy się";
        return "· pozostało " + formatDuration(minutes);
    }

    private String formatUpcoming(final long minutes) {
        if (minutes <= 0) return "· za chwilę";
        return "· za " + formatDuration(minutes);
    }

    /** Formats minutes as "Xh Ymin" when >= 60, otherwise "X min" (or "1 min"). */
    private String formatDuration(final long totalMinutes) {
        if (totalMinutes < 60) {
            return totalMinutes + " min";
        }
        final long hours = totalMinutes / 60;
        final long mins = totalMinutes % 60;
        if (mins == 0) {
            return hours + "h";
        }
        return hours + "h " + mins + " min";
    }

    private static boolean isBlank(final String s) {
        return s == null || s.isBlank();
    }

    // ── Internal DTO ─────────────────────────────────────────────

    private record EventInfo(GoogleCalendarEvent event, long remainingMinutes, long minutesUntilStart) {}
}
