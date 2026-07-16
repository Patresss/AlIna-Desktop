package com.patres.alina.uidesktop.ui.calendar;

import com.patres.alina.server.integration.GoogleCalendarAttachment;
import com.patres.alina.server.integration.GoogleCalendarAttendee;
import com.patres.alina.server.integration.GoogleCalendarEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarEventPromptArgumentsTest {

    @Test
    void includesEveryAvailableMeetingDetail() {
        final GoogleCalendarEvent event = event(
                "Planowanie Q3",
                "Sala Atlas",
                "<p>Omów cele &amp; ryzyka</p>",
                List.of(
                        new GoogleCalendarAttendee("Anna", "anna@example.com", false),
                        new GoogleCalendarAttendee("", "jan@example.com", false),
                        new GoogleCalendarAttendee("Sala Atlas", "room@example.com", true)
                ),
                List.of(
                        new GoogleCalendarAttachment("Agenda", "https://example.com/agenda", "text/plain"),
                        new GoogleCalendarAttachment("Notatki", "javascript:alert(1)", "text/plain")
                )
        );

        assertThat(CalendarEventPromptArguments.format(event)).isEqualTo("""
                Event: Planowanie Q3
                Time: 09:30 - 10:00
                Meeting link: https://meet.google.com/abc-defg-hij
                Participants: Anna, jan@example.com
                Rooms: Sala Atlas
                Description: Omów cele & ryzyka
                Attachments:
                - Agenda: https://example.com/agenda
                - Notatki""");
    }

    @Test
    void omitsMissingOptionalDetailsForAllDayEvent() {
        final GoogleCalendarEvent event = new GoogleCalendarEvent(
                "Dzień skupienia", "", "", "", "", List.of(), List.of(), true,
                "", "", "", "", ""
        );

        assertThat(CalendarEventPromptArguments.format(event)).isEqualTo("""
                Event: Dzień skupienia
                Time: All day""");
    }

    @Test
    void treatsUrlOnlyLocationAsMeetingLinkInsteadOfPhysicalLocation() {
        final GoogleCalendarEvent event = new GoogleCalendarEvent(
                "Demo", "09:30", "10:00", "https://zoom.us/j/123", "",
                List.of(), List.of(), false, "", "", "",
                "2026-07-15T09:30:00+02:00", "2026-07-15T10:00:00+02:00"
        );

        assertThat(CalendarEventPromptArguments.format(event))
                .contains("Meeting link: https://zoom.us/j/123")
                .doesNotContain("Location:");
    }

    @Test
    void keepsPhysicalLocationWhenNoRoomResourceExists() {
        final GoogleCalendarEvent event = new GoogleCalendarEvent(
                "Demo", "09:30", "10:00", "Sala Atlas", "",
                List.of(), List.of(), false, "", "", "",
                "2026-07-15T09:30:00+02:00", "2026-07-15T10:00:00+02:00"
        );

        assertThat(CalendarEventPromptArguments.format(event)).contains("Location: Sala Atlas");
    }

    private GoogleCalendarEvent event(final String summary,
                                      final String location,
                                      final String description,
                                      final List<GoogleCalendarAttendee> attendees,
                                      final List<GoogleCalendarAttachment> attachments) {
        return new GoogleCalendarEvent(
                summary,
                "09:30",
                "10:00",
                location,
                description,
                attendees,
                attachments,
                false,
                "https://meet.google.com/abc-defg-hij",
                "",
                "",
                "2026-07-15T09:30:00+02:00",
                "2026-07-15T10:00:00+02:00"
        );
    }
}
