package com.patres.alina.server.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleCalendarResponseParserTest {

    @Test
    void parsesDescriptionAttendeesAndAttachments() throws Exception {
        final String json = """
                {
                  "items": [{
                    "summary": "Product discovery",
                    "description": "Agenda <b>Q3</b> https://meet.google.com/abc-defg-hij",
                    "start": {"dateTime": "2026-07-15T10:00:00+02:00"},
                    "end": {"dateTime": "2026-07-15T11:00:00+02:00"},
                    "location": "Sala Wisła",
                    "attendees": [
                      {"displayName": "Anna", "email": "anna@example.com"},
                      {"email": "marek@example.com"},
                      {"displayName": "Sala Wisła", "email": "room@example.com", "resource": true}
                    ],
                    "attachments": [{
                      "title": "Roadmap.pdf",
                      "fileUrl": "https://drive.google.com/file/d/123/view",
                      "mimeType": "application/pdf"
                    }]
                  }]
                }
                """;

        final GoogleCalendarEvent event = GoogleCalendarResponseParser.parse(json).events().getFirst();

        assertThat(event.description()).contains("Agenda", "Q3");
        assertThat(event.descriptionVideoUrl()).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(event.attendees())
                .containsExactly(
                        new GoogleCalendarAttendee("Anna", "anna@example.com", false),
                        new GoogleCalendarAttendee("", "marek@example.com", false),
                        new GoogleCalendarAttendee("Sala Wisła", "room@example.com", true)
                );
        assertThat(event.attachments()).containsExactly(new GoogleCalendarAttachment(
                "Roadmap.pdf",
                "https://drive.google.com/file/d/123/view",
                "application/pdf"
        ));
    }

    @Test
    void missingAndPartialNestedFieldsRemainSafe() throws Exception {
        final String json = """
                {
                  "items": [
                    {
                      "summary": "No details",
                      "start": {"date": "2026-07-15"},
                      "end": {"date": "2026-07-16"}
                    },
                    {
                      "summary": "Partial",
                      "start": {"dateTime": "2026-07-15T12:00:00Z"},
                      "end": {"dateTime": "2026-07-15T12:30:00Z"},
                      "attendees": [{}],
                      "attachments": [{}]
                    }
                  ]
                }
                """;

        final var events = GoogleCalendarResponseParser.parse(json).events();

        assertThat(events.get(0).attendees()).isEmpty();
        assertThat(events.get(0).attachments()).isEmpty();
        assertThat(events.get(0).description()).isEmpty();
        assertThat(events.get(1).attendees()).containsExactly(new GoogleCalendarAttendee("", "", false));
        assertThat(events.get(1).attachments()).containsExactly(new GoogleCalendarAttachment("", "", ""));
    }
}
