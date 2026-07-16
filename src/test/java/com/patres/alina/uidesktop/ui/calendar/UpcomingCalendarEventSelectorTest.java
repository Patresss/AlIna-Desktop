package com.patres.alina.uidesktop.ui.calendar;

import com.patres.alina.server.integration.GoogleCalendarEvent;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UpcomingCalendarEventSelectorTest {

    private static final Instant NOW = Instant.parse("2026-07-15T10:30:00Z");
    private final UpcomingCalendarEventSelector selector = new UpcomingCalendarEventSelector(
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void runningEventEndingSoonestWinsWhenEventsOverlap() {
        final var later = timed("Later", "2026-07-15T10:00:00Z", "2026-07-15T11:00:00Z");
        final var sooner = timed("Sooner", "2026-07-15T10:20:00Z", "2026-07-15T10:40:30Z");

        final var selection = selector.select(List.of(later, sooner)).orElseThrow();

        assertThat(selection.event()).isEqualTo(sooner);
        assertThat(selection.state()).isEqualTo(UpcomingCalendarEventSelector.State.RUNNING);
        assertThat(selection.minutes()).isEqualTo(10);
    }

    @Test
    void earliestFutureEventWinsAfterEndedEvents() {
        final var ended = timed("Ended", "2026-07-15T09:00:00Z", "2026-07-15T10:00:00Z");
        final var later = timed("Later", "2026-07-15T13:00:00Z", "2026-07-15T14:00:00Z");
        final var next = timed("Next", "2026-07-15T11:00:00Z", "2026-07-15T11:30:00Z");

        final var selection = selector.select(List.of(ended, later, next)).orElseThrow();

        assertThat(selection.event()).isEqualTo(next);
        assertThat(selection.state()).isEqualTo(UpcomingCalendarEventSelector.State.UPCOMING);
        assertThat(selection.minutes()).isEqualTo(30);
    }

    @Test
    void allDayEventsAreNeverSelected() {
        final var allDay = allDay("Holiday");
        final var future = timed("Call", "2026-07-15T11:00:00Z", "2026-07-15T11:30:00Z");

        assertThat(selector.select(List.of(allDay, future)).orElseThrow().event()).isEqualTo(future);
        assertThat(selector.select(List.of(allDay))).isEmpty();
    }

    @Test
    void malformedAndEmptyTimedEventsAreIgnored() {
        final var malformed = timed("Broken", "not-a-date", "also-broken");
        final var missingEnd = timed("No end", "2026-07-15T11:00:00Z", "");

        assertThat(selector.select(List.of(malformed, missingEnd))).isEmpty();
        assertThat(selector.select(List.of())).isEmpty();
    }

    private static GoogleCalendarEvent timed(final String summary, final String start, final String end) {
        return event(summary, false, start, end);
    }

    private static GoogleCalendarEvent allDay(final String summary) {
        return event(summary, true, "", "");
    }

    private static GoogleCalendarEvent event(final String summary,
                                             final boolean allDay,
                                             final String start,
                                             final String end) {
        return new GoogleCalendarEvent(
                summary, "", "", "", "", List.of(), List.of(), allDay,
                "", "", "", start, end
        );
    }
}
