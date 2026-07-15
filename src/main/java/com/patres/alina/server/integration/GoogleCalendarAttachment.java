package com.patres.alina.server.integration;

/** Metadata for one attachment linked from a Google Calendar event. */
public record GoogleCalendarAttachment(String title, String fileUrl, String mimeType) {

    public GoogleCalendarAttachment {
        title = normalize(title);
        fileUrl = normalize(fileUrl);
        mimeType = normalize(mimeType);
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.strip();
    }
}
