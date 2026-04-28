package com.patres.alina.server.integration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GoogleCalendarResponseParser {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarResponseParser.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final String TOKEN_INVALID_MESSAGE = "Token expired or invalid. Re-authenticate to fix.";
    private static final String CALENDAR_SCOPE_REQUIRED_MESSAGE =
            "Google Calendar access needs Calendar scope. Run '%s' and try again.";
    private static final String GWS_WRONG_BINARY_HINT =
            "The 'gws' command on PATH is not the Google Workspace CLI. Install the correct gws CLI or adjust PATH so AlIna uses the Google calendar tool.";
    private static final String EMPTY_VALUE = "";
    private static final String ALL_DAY_LABEL = "All day";
    private static final String NO_TITLE_LABEL = "(No title)";
    private static final String UNKNOWN_ERROR_LABEL = "Unknown error";
    private static final String ERROR_NODE = "error";
    private static final String ERROR_MESSAGE_NODE = "message";
    private static final String ITEMS_NODE = "items";
    private static final String START_NODE = "start";
    private static final String END_NODE = "end";
    private static final String DATE_NODE = "date";
    private static final String DATE_TIME_NODE = "dateTime";
    private static final String SUMMARY_NODE = "summary";
    private static final String DESCRIPTION_NODE = "description";
    private static final String LOCATION_NODE = "location";
    private static final String HANGOUT_LINK_NODE = "hangoutLink";
    private static final String CONFERENCE_DATA_NODE = "conferenceData";
    private static final String ENTRY_POINTS_NODE = "entryPoints";
    private static final String ENTRY_POINT_TYPE_NODE = "entryPointType";
    private static final String URI_NODE = "uri";
    private static final String VIDEO_ENTRY_POINT_TYPE = "video";
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
    private static final List<String> AUTH_ERROR_MARKERS = List.of(
            "401",
            "invalid_grant",
            "Token has been expired",
            "UNAUTHENTICATED"
    );
    private static final List<String> SCOPE_ERROR_MARKERS = List.of(
            "403",
            "accessNotConfigured",
            "PERMISSION_DENIED",
            "insufficient authentication scope",
            "ACCESS_TOKEN_SCOPE_INSUFFICIENT",
            "Request had insufficient authentication scopes"
    );
    private static final List<String> WRONG_GWS_OUTPUT_MARKERS = List.of(
            "Not in a workspace",
            "helper to manage workspaces",
            "git repositories"
    );
    private static final Pattern VIDEO_URL_PATTERN = Pattern.compile(
            "https?://(?:"
                    + "[\\w.-]*zoom\\.us/[^\\s\"<>]+"
                    + "|teams\\.microsoft\\.com/[^\\s\"<>]+"
                    + "|[\\w.-]*webex\\.com/[^\\s\"<>]+"
                    + "|meet\\.google\\.com/[^\\s\"<>]+"
                    + ")",
            Pattern.CASE_INSENSITIVE
    );

    private GoogleCalendarResponseParser() {
    }

    static GoogleCalendarResult parse(final String json) throws JsonProcessingException {
        final JsonNode root = parseJsonResponse(json);
        final GoogleCalendarResult errorResult = mapApiError(root);
        if (errorResult != null) {
            return errorResult;
        }
        return GoogleCalendarResult.success(parseEvents(root));
    }

    private static JsonNode parseJsonResponse(final String json) throws JsonProcessingException {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (final JsonProcessingException e) {
            throw new JsonProcessingException(buildNonJsonErrorMessage(json), e) {
            };
        }
    }

    private static String buildNonJsonErrorMessage(final String output) {
        final String trimmedOutput = output.trim();
        final String normalizedOutput = normalizeWhitespace(output);
        if (containsAny(normalizedOutput, WRONG_GWS_OUTPUT_MARKERS)) {
            return GWS_WRONG_BINARY_HINT;
        }
        return "gws returned non-JSON output:\n" + trimmedOutput;
    }

    private static GoogleCalendarResult mapApiError(final JsonNode root) {
        if (!root.has(ERROR_NODE)) {
            return null;
        }

        final String errorMessage = root.path(ERROR_NODE).path(ERROR_MESSAGE_NODE).asText(UNKNOWN_ERROR_LABEL);
        if (containsAny(errorMessage, AUTH_ERROR_MARKERS)) {
            return GoogleCalendarResult.authError(TOKEN_INVALID_MESSAGE);
        }
        if (containsAny(errorMessage, SCOPE_ERROR_MARKERS)) {
            return GoogleCalendarResult.authError(
                    CALENDAR_SCOPE_REQUIRED_MESSAGE.formatted(GoogleCalendarCli.calendarAuthCommand()));
        }
        return GoogleCalendarResult.error(errorMessage);
    }

    private static List<GoogleCalendarEvent> parseEvents(final JsonNode root) {
        final JsonNode items = root.path(ITEMS_NODE);
        if (items.isMissingNode() || !items.isArray()) {
            return Collections.emptyList();
        }

        final List<GoogleCalendarEvent> events = new ArrayList<>();
        for (final JsonNode item : items) {
            events.add(buildEvent(item));
        }
        return Collections.unmodifiableList(events);
    }

    private static GoogleCalendarEvent buildEvent(final JsonNode item) {
        final String summary = item.path(SUMMARY_NODE).asText(NO_TITLE_LABEL);
        final String location = item.path(LOCATION_NODE).asText(EMPTY_VALUE);
        final String hangoutLink = item.path(HANGOUT_LINK_NODE).asText(EMPTY_VALUE);
        final String conferenceUri = extractConferenceVideoUri(item.path(CONFERENCE_DATA_NODE));
        final String description = item.path(DESCRIPTION_NODE).asText(EMPTY_VALUE);
        final String descriptionVideoUrl = extractVideoUrlFromDescription(description);

        final JsonNode start = item.path(START_NODE);
        final JsonNode end = item.path(END_NODE);
        final boolean allDay = start.has(DATE_NODE) && !start.has(DATE_TIME_NODE);
        final EventTimeDetails timeDetails = allDay
                ? EventTimeDetails.allDay()
                : EventTimeDetails.timed(
                start.path(DATE_TIME_NODE).asText(EMPTY_VALUE),
                end.path(DATE_TIME_NODE).asText(EMPTY_VALUE)
        );

        return new GoogleCalendarEvent(
                summary,
                timeDetails.startTime(),
                timeDetails.endTime(),
                location,
                allDay,
                hangoutLink,
                conferenceUri,
                descriptionVideoUrl,
                timeDetails.rawStartDateTime(),
                timeDetails.rawEndDateTime()
        );
    }

    private static String formatDateTime(final String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) {
            return EMPTY_VALUE;
        }
        try {
            final OffsetDateTime odt = OffsetDateTime.parse(isoDateTime);
            final LocalDateTime ldt = odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            return ldt.format(TIME_FORMATTER);
        } catch (final Exception e) {
            logger.debug("GoogleCalendar: failed to parse dateTime: {}", isoDateTime, e);
            return isoDateTime;
        }
    }

    private static String extractConferenceVideoUri(final JsonNode conferenceData) {
        if (conferenceData == null || conferenceData.isMissingNode()) {
            return EMPTY_VALUE;
        }
        final JsonNode entryPoints = conferenceData.path(ENTRY_POINTS_NODE);
        if (!entryPoints.isArray()) {
            return EMPTY_VALUE;
        }
        for (final JsonNode entry : entryPoints) {
            if (VIDEO_ENTRY_POINT_TYPE.equals(entry.path(ENTRY_POINT_TYPE_NODE).asText(EMPTY_VALUE))) {
                final String uri = entry.path(URI_NODE).asText(EMPTY_VALUE);
                if (!uri.isBlank()) {
                    return uri;
                }
            }
        }
        return EMPTY_VALUE;
    }

    private static String extractVideoUrlFromDescription(final String description) {
        if (description == null || description.isBlank()) {
            return EMPTY_VALUE;
        }
        final Matcher matcher = VIDEO_URL_PATTERN.matcher(description);
        if (matcher.find()) {
            return matcher.group();
        }
        return EMPTY_VALUE;
    }

    private static String normalizeWhitespace(final String value) {
        return WHITESPACE_PATTERN.matcher(value).replaceAll(" ").trim();
    }

    private static boolean containsAny(final String value, final List<String> markers) {
        for (final String marker : markers) {
            if (value.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private record EventTimeDetails(String startTime, String endTime, String rawStartDateTime, String rawEndDateTime) {

        private static EventTimeDetails allDay() {
            return new EventTimeDetails(ALL_DAY_LABEL, EMPTY_VALUE, EMPTY_VALUE, EMPTY_VALUE);
        }

        private static EventTimeDetails timed(final String rawStartDateTime, final String rawEndDateTime) {
            return new EventTimeDetails(
                    formatDateTime(rawStartDateTime),
                    formatDateTime(rawEndDateTime),
                    rawStartDateTime,
                    rawEndDateTime
            );
        }
    }
}
