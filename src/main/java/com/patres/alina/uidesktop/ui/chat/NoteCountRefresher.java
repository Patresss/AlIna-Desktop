package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.util.FxThreadRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically fetches the Obsidian note count and pushes it to the
 * welcome-screen particle logo via {@link Browser#updateNoteCount}.
 * <p>
 * Lifecycle: call {@link #start()} once after the browser is ready,
 * and {@link #stop()} when the owning ChatWindow is disposed.
 */
final class NoteCountRefresher {

    private static final Logger logger = LoggerFactory.getLogger(NoteCountRefresher.class);
    private static final long REFRESH_INTERVAL_MINUTES = 1;

    private final Browser browser;
    private ScheduledExecutorService scheduler;

    NoteCountRefresher(final Browser browser) {
        this.browser = browser;
    }

    /**
     * Fetches the note count immediately, then schedules periodic refreshes.
     */
    void start() {
        refresh();
        if (scheduler != null) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = Thread.ofVirtual().unstarted(r);
            t.setName("note-count-refresher");
            return t;
        });
        scheduler.scheduleAtFixedRate(this::refresh, REFRESH_INTERVAL_MINUTES, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Stops the periodic refresh and releases the scheduler thread.
     */
    void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    private void refresh() {
        try {
            final long count = BackendApi.getNoteCount();
            final String label = LanguageManager.getLanguageString("welcome.notes");
            FxThreadRunner.run(() -> browser.updateNoteCount(count, label));
        } catch (final Exception e) {
            logger.warn("Failed to refresh note count", e);
        }
    }
}
