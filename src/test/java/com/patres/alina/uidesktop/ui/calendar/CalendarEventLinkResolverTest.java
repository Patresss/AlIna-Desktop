package com.patres.alina.uidesktop.ui.calendar;

import com.patres.alina.server.integration.GoogleCalendarEvent;
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

    private static GoogleCalendarEvent event(final String hangoutLink, final String location) {
        return new GoogleCalendarEvent(
                "Event", "", "", location, "", List.of(), List.of(), false,
                hangoutLink, "", "", "2026-07-15T10:00:00Z", "2026-07-15T11:00:00Z"
        );
    }
}
