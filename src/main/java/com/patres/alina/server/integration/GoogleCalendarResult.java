package com.patres.alina.server.integration;

import java.util.List;

/**
 * Result of fetching today's Google Calendar events.
 *
 * @param events       the list of calendar events for today
 * @param authError    true when the fetch failed due to an authentication / token error
 * @param errorMessage human-readable error message (empty when no error)
 */
public record GoogleCalendarResult(
        List<GoogleCalendarEvent> events,
        boolean authError,
        String errorMessage
) {

    public static GoogleCalendarResult success(final List<GoogleCalendarEvent> events) {
        return new GoogleCalendarResult(events, false, "");
    }

    public static GoogleCalendarResult authError(final String message) {
        return new GoogleCalendarResult(List.of(), true, message);
    }

    public static GoogleCalendarResult error(final String message) {
        return new GoogleCalendarResult(List.of(), false, message);
    }
}
