package com.patres.alina.server.integration;

/**
 * Represents a single Google Calendar event.
 *
 * @param summary              event title
 * @param startTime            start time formatted for display (e.g. "09:30" or "All day")
 * @param endTime              end time formatted for display (e.g. "10:00" or "")
 * @param location             meeting location or URL (may be empty)
 * @param allDay               true when the event spans the entire day
 * @param hangoutLink          Google Meet / Hangout link (may be empty)
 * @param conferenceUri        video conference URI from conferenceData (e.g. Zoom link; may be empty)
 * @param descriptionVideoUrl  video conference URL extracted from the event description (may be empty)
 * @param rawStartDateTime     raw ISO start dateTime for "current event" detection (empty for all-day)
 * @param rawEndDateTime       raw ISO end dateTime for filtering (empty for all-day events)
 */
public record GoogleCalendarEvent(
        String summary,
        String startTime,
        String endTime,
        String location,
        boolean allDay,
        String hangoutLink,
        String conferenceUri,
        String descriptionVideoUrl,
        String rawStartDateTime,
        String rawEndDateTime
) {}
