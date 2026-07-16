package com.patres.alina.uidesktop.ui.calendar;

import com.patres.alina.server.integration.GoogleCalendarEvent;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Chooses the most relevant running or upcoming event for the current day. */
public final class UpcomingCalendarEventSelector {

    private final Clock clock;

    public UpcomingCalendarEventSelector(final Clock clock) {
        this.clock = clock;
    }

    public Optional<Selection> select(final List<GoogleCalendarEvent> events) {
        if (events == null || events.isEmpty()) {
            return Optional.empty();
        }
        final Instant now = clock.instant();

        final Optional<Selection> current = events.stream()
                .filter(event -> !event.allDay())
                .map(event -> runningSelection(event, now))
                .flatMap(Optional::stream)
                .min(Comparator.comparingLong(Selection::minutes)
                        .thenComparing(selection -> parseInstant(
                                selection.event().rawEndDateTime()
                        ).orElse(Instant.MAX)));
        if (current.isPresent()) {
            return current;
        }

        return events.stream()
                .filter(event -> !event.allDay())
                .map(event -> upcomingSelection(event, now))
                .flatMap(Optional::stream)
                .min(Comparator.comparingLong(Selection::minutes)
                        .thenComparing(selection -> parseInstant(
                                selection.event().rawStartDateTime()
                        ).orElse(Instant.MAX)));
    }

    private Optional<Selection> runningSelection(final GoogleCalendarEvent event, final Instant now) {
        final Optional<Instant> start = parseInstant(event.rawStartDateTime());
        final Optional<Instant> end = parseInstant(event.rawEndDateTime());
        if (start.isEmpty() || end.isEmpty() || now.isBefore(start.get()) || !now.isBefore(end.get())) {
            return Optional.empty();
        }
        return Optional.of(new Selection(
                event,
                State.RUNNING,
                Math.max(0, ChronoUnit.MINUTES.between(now, end.get()))
        ));
    }

    private Optional<Selection> upcomingSelection(final GoogleCalendarEvent event, final Instant now) {
        final Optional<Instant> start = parseInstant(event.rawStartDateTime());
        final Optional<Instant> end = parseInstant(event.rawEndDateTime());
        if (start.isEmpty()
                || end.isEmpty()
                || !start.get().isBefore(end.get())
                || !now.isBefore(start.get())) {
            return Optional.empty();
        }
        return Optional.of(new Selection(
                event,
                State.UPCOMING,
                Math.max(0, ChronoUnit.MINUTES.between(now, start.get()))
        ));
    }

    private Optional<Instant> parseInstant(final String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(OffsetDateTime.parse(value).toInstant());
        } catch (final Exception ignored) {
            return Optional.empty();
        }
    }

    public enum State {
        RUNNING,
        UPCOMING
    }

    public record Selection(GoogleCalendarEvent event, State state, long minutes) {
    }
}
