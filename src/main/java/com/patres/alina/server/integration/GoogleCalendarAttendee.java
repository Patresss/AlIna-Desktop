package com.patres.alina.server.integration;

/** Display information for one Google Calendar event attendee. */
public record GoogleCalendarAttendee(String displayName, String email) {

    public GoogleCalendarAttendee {
        displayName = normalize(displayName);
        email = normalize(email);
    }

    public String label() {
        return displayName.isBlank() ? email : displayName;
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.strip();
    }
}
