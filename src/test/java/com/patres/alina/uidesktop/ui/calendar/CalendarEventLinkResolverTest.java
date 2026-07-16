package com.patres.alina.uidesktop.ui.calendar;

import com.patres.alina.server.integration.GoogleCalendarEvent;
import com.patres.alina.server.integration.GoogleCalendarAttendee;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarEventLinkResolverTest {

    @Test
    void prefersConferenceLinksAndAcceptsSafeLocationUrl() {
        assertThat(CalendarEventLinkResolver.resolveJoinUrl(event(
                "https://meet.google.com/abc-defg-hij",
                "https://zoom.us/j/123"
        ))).contains("https://meet.google.com/abc-defg-hij");

        assertThat(CalendarEventLinkResolver.resolveJoinUrl(event(
                "",
                "https://zoom.us/j/123, Sala online"
        ))).contains("https://zoom.us/j/123");
    }

    @Test
    void rejectsUnsafeAndMalformedSchemes() {
        assertThat(CalendarEventLinkResolver.safeHttpUrl("javascript:alert(1)")).isEmpty();
        assertThat(CalendarEventLinkResolver.safeHttpUrl("file:///tmp/secret")).isEmpty();
        assertThat(CalendarEventLinkResolver.safeHttpUrl("not a url")).isEmpty();
    }

    @Test
    void separatesOnlineAndPhysicalLocationsForDisplay() {
        assertThat(CalendarEventLinkResolver.displayLocation("https://zoom.us/j/123")).isEmpty();
        assertThat(CalendarEventLinkResolver.displayLocation("https://zoom.us/j/123, Sala Atlas"))
                .isEqualTo("Sala Atlas");
        assertThat(CalendarEventLinkResolver.displayLocation("Warszawa, Sala Atlas"))
                .isEqualTo("Warszawa, Sala Atlas");
    }

    @Test
    void roomResourcesSuppressDuplicatedPhysicalLocation() {
        final GoogleCalendarEvent event = new GoogleCalendarEvent(
                "Event", "09:00", "10:00", "Warszawa, Sala Atlas", "",
                List.of(new GoogleCalendarAttendee("Warszawa, Sala Atlas", "room@example.com", true)),
                List.of(), false, "", "", "",
                "2026-07-15T09:00:00Z", "2026-07-15T10:00:00Z"
        );

        assertThat(CalendarEventLinkResolver.displayLocation(event)).isEmpty();
    }

    private static GoogleCalendarEvent event(final String hangoutLink, final String location) {
        return new GoogleCalendarEvent(
                "Event", "", "", location, "", List.of(), List.of(), false,
                hangoutLink, "", "", "2026-07-15T10:00:00Z", "2026-07-15T11:00:00Z"
        );
    }
}
