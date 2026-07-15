package com.patres.alina.uidesktop.ui.calendar;

import com.patres.alina.server.integration.GoogleCalendarResult;
import com.patres.alina.server.integration.GoogleCalendarService;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Single asynchronous source of today's Google Calendar events for all UI consumers. */
public final class GoogleCalendarFeed implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarFeed.class);

    private final Supplier<GoogleCalendarResult> fetcher;
    private final Executor callbackExecutor;
    private final ScheduledExecutorService scheduler;
    private final Clock clock;
    private final CopyOnWriteArrayList<Consumer<GoogleCalendarSnapshot>> subscribers =
            new CopyOnWriteArrayList<>();
    private final AtomicBoolean refreshInFlight = new AtomicBoolean();

    private volatile GoogleCalendarSnapshot snapshot = GoogleCalendarSnapshot.initialLoading();
    private ScheduledFuture<?> refreshTask;
    private boolean closed;

    public GoogleCalendarFeed() {
        this(
                GoogleCalendarService::fetchTodayEvents,
                Platform::runLater,
                Executors.newSingleThreadScheduledExecutor(task -> {
                    final Thread thread = new Thread(task, "google-calendar-feed");
                    thread.setDaemon(true);
                    return thread;
                }),
                Clock.systemUTC()
        );
    }

    GoogleCalendarFeed(final Supplier<GoogleCalendarResult> fetcher,
                       final Executor callbackExecutor,
                       final ScheduledExecutorService scheduler,
                       final Clock clock) {
        this.fetcher = fetcher;
        this.callbackExecutor = callbackExecutor;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    public synchronized void start(final int refreshIntervalSeconds) {
        ensureOpen();
        scheduleRefresh(refreshIntervalSeconds);
    }

    public synchronized void setRefreshIntervalSeconds(final int refreshIntervalSeconds) {
        ensureOpen();
        scheduleRefresh(refreshIntervalSeconds);
    }

    public void refreshNow() {
        synchronized (this) {
            ensureOpen();
        }
        if (refreshInFlight.compareAndSet(false, true)) {
            scheduler.execute(this::performRefresh);
        }
    }

    public Runnable subscribe(final Consumer<GoogleCalendarSnapshot> subscriber) {
        subscribers.add(subscriber);
        dispatch(subscriber, snapshot);
        return () -> subscribers.remove(subscriber);
    }

    public GoogleCalendarSnapshot snapshot() {
        return snapshot;
    }

    private synchronized void scheduleRefresh(final int requestedSeconds) {
        final int seconds = Math.max(1, requestedSeconds);
        if (refreshTask != null) {
            refreshTask.cancel(false);
        }
        refreshTask = scheduler.scheduleWithFixedDelay(
                this::refreshNow,
                0,
                seconds,
                TimeUnit.SECONDS
        );
    }

    private void performRefresh() {
        try {
            final GoogleCalendarResult result = fetcher.get();
            final GoogleCalendarSnapshot previous = snapshot;
            if (!result.authError() && result.errorMessage().isEmpty()) {
                snapshot = new GoogleCalendarSnapshot(
                        false,
                        result,
                        result.events(),
                        Instant.now(clock)
                );
            } else {
                snapshot = new GoogleCalendarSnapshot(
                        false,
                        result,
                        previous.lastSuccessfulEvents(),
                        previous.lastSuccessfulAt()
                );
            }
            notifySubscribers(snapshot);
        } catch (final Exception e) {
            logger.warn("Google Calendar feed refresh failed", e);
            final GoogleCalendarSnapshot previous = snapshot;
            snapshot = new GoogleCalendarSnapshot(
                    false,
                    GoogleCalendarResult.error(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()),
                    previous.lastSuccessfulEvents(),
                    previous.lastSuccessfulAt()
            );
            notifySubscribers(snapshot);
        } finally {
            refreshInFlight.set(false);
        }
    }

    private void notifySubscribers(final GoogleCalendarSnapshot value) {
        callbackExecutor.execute(() -> {
            for (final Consumer<GoogleCalendarSnapshot> subscriber : List.copyOf(subscribers)) {
                try {
                    subscriber.accept(value);
                } catch (final Exception e) {
                    logger.warn("Google Calendar feed subscriber failed", e);
                }
            }
        });
    }

    private void dispatch(final Consumer<GoogleCalendarSnapshot> subscriber,
                          final GoogleCalendarSnapshot value) {
        callbackExecutor.execute(() -> {
            try {
                subscriber.accept(value);
            } catch (final Exception e) {
                logger.warn("Google Calendar feed subscriber failed", e);
            }
        });
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("Google Calendar feed is closed");
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        if (refreshTask != null) {
            refreshTask.cancel(true);
        }
        subscribers.clear();
        scheduler.shutdownNow();
    }
}
