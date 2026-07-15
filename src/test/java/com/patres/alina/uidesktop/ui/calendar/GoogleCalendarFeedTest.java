package com.patres.alina.uidesktop.ui.calendar;

import com.patres.alina.server.integration.GoogleCalendarResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleCalendarFeedTest {

    private GoogleCalendarFeed feed;

    @AfterEach
    void closeFeed() {
        if (feed != null) {
            feed.close();
        }
    }

    @Test
    void coalescesRefreshesWhileARequestIsInFlight() throws Exception {
        final AtomicInteger fetchCount = new AtomicInteger();
        final CountDownLatch fetchStarted = new CountDownLatch(1);
        final CountDownLatch allowFinish = new CountDownLatch(1);
        feed = feed(() -> {
            fetchCount.incrementAndGet();
            fetchStarted.countDown();
            await(allowFinish);
            return GoogleCalendarResult.success(List.of());
        });

        feed.refreshNow();
        assertThat(fetchStarted.await(2, TimeUnit.SECONDS)).isTrue();
        feed.refreshNow();
        feed.refreshNow();
        allowFinish.countDown();
        waitUntil(() -> feed.snapshot().hasSuccessfulSnapshot());

        assertThat(fetchCount).hasValue(1);
    }

    @Test
    void retainsLastSuccessfulSnapshotAfterAnError() throws Exception {
        final AtomicInteger fetchCount = new AtomicInteger();
        final AtomicReference<GoogleCalendarSnapshot> latest = new AtomicReference<>();
        feed = feed(() -> fetchCount.getAndIncrement() == 0
                ? GoogleCalendarResult.success(List.of())
                : GoogleCalendarResult.error("offline"));
        feed.subscribe(latest::set);

        feed.refreshNow();
        waitUntil(() -> latest.get() != null && latest.get().hasSuccessfulSnapshot());
        final Instant successfulAt = latest.get().lastSuccessfulAt();

        feed.refreshNow();
        waitUntil(() -> latest.get().latestResult() != null
                && latest.get().latestResult().errorMessage().equals("offline"));

        assertThat(latest.get().lastSuccessfulAt()).isEqualTo(successfulAt);
        assertThat(latest.get().lastSuccessfulEvents()).isEmpty();
    }

    private GoogleCalendarFeed feed(final java.util.function.Supplier<GoogleCalendarResult> fetcher) {
        return new GoogleCalendarFeed(
                fetcher,
                Runnable::run,
                Executors.newSingleThreadScheduledExecutor(),
                Clock.fixed(Instant.parse("2026-07-15T10:30:00Z"), ZoneOffset.UTC)
        );
    }

    private static void await(final CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private static void waitUntil(final java.util.function.BooleanSupplier condition) throws Exception {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }
}
