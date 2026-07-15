package com.patres.alina.uidesktop.ui.calendar;

import com.patres.alina.server.integration.GoogleCalendarEvent;
import com.patres.alina.server.integration.GoogleCalendarResult;

import java.time.Instant;
import java.util.List;

/** Immutable state published by the shared Google Calendar feed. */
public record GoogleCalendarSnapshot(
        boolean loading,
        GoogleCalendarResult latestResult,
        List<GoogleCalendarEvent> lastSuccessfulEvents,
        Instant lastSuccessfulAt
) {

    public GoogleCalendarSnapshot {
        lastSuccessfulEvents = lastSuccessfulEvents == null ? List.of() : List.copyOf(lastSuccessfulEvents);
    }

    public static GoogleCalendarSnapshot initialLoading() {
        return new GoogleCalendarSnapshot(true, null, List.of(), null);
    }

    public boolean hasSuccessfulSnapshot() {
        return lastSuccessfulAt != null;
    }
}
