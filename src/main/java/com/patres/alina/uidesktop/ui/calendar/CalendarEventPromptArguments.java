package com.patres.alina.uidesktop.ui.calendar;

import com.patres.alina.server.integration.GoogleCalendarAttachment;
import com.patres.alina.server.integration.GoogleCalendarAttendee;
import com.patres.alina.server.integration.GoogleCalendarEvent;

import java.util.List;
import java.util.Objects;

/** Builds the shared event context substituted into Calendar AI prompts. */
public final class CalendarEventPromptArguments {

    private CalendarEventPromptArguments() {
    }

    public static String format(final GoogleCalendarEvent event) {
        Objects.requireNonNull(event, "event");

        final StringBuilder arguments = new StringBuilder("Event: ").append(event.summary());
        arguments.append("\nTime: ");
        if (event.allDay()) {
            arguments.append("All day");
        } else {
            arguments.append(event.startTime()).append(" - ").append(event.endTime());
        }

        final List<String> attendees = event.attendees().stream()
                .filter(attendee -> !attendee.resource())
                .map(GoogleCalendarAttendee::label)
                .filter(label -> !label.isBlank())
                .toList();
        final List<String> rooms = event.attendees().stream()
                .filter(GoogleCalendarAttendee::resource)
                .map(GoogleCalendarAttendee::label)
                .filter(label -> !label.isBlank())
                .toList();

        appendValue(arguments, "Location", CalendarEventLinkResolver.displayLocation(event));
        CalendarEventLinkResolver.resolveJoinUrl(event)
                .ifPresent(url -> appendValue(arguments, "Meeting link", url));

        if (!attendees.isEmpty()) {
            appendValue(arguments, "Participants", String.join(", ", attendees));
        }
        if (!rooms.isEmpty()) {
            appendValue(arguments, "Rooms", String.join(", ", rooms));
        }

        appendValue(arguments, "Description", CalendarDescriptionText.toPlainText(event.description()));

        final List<String> attachments = event.attachments().stream()
                .map(CalendarEventPromptArguments::formatAttachment)
                .filter(value -> !value.isBlank())
                .toList();
        if (!attachments.isEmpty()) {
            arguments.append("\nAttachments:\n- ").append(String.join("\n- ", attachments));
        }
        return arguments.toString();
    }

    private static String formatAttachment(final GoogleCalendarAttachment attachment) {
        final String title = attachment.title();
        final String url = CalendarEventLinkResolver.safeHttpUrl(attachment.fileUrl()).orElse("");
        if (title.isBlank()) {
            return url;
        }
        return url.isBlank() ? title : title + ": " + url;
    }

    private static void appendValue(final StringBuilder target, final String label, final String value) {
        if (value != null && !value.isBlank()) {
            target.append('\n').append(label).append(": ").append(value.strip());
        }
    }
}
