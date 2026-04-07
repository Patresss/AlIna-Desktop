package com.patres.alina.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for fetching today's Google Calendar events via the {@code gws} CLI.
 * <p>
 * Uses {@code gws calendar events list} with a token obtained from
 * {@code gcloud auth application-default print-access-token} to avoid the
 * 403 quota-project issue present in {@code gws calendar +agenda}.
 * </p>
 */
public final class GoogleCalendarService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final long COMMAND_TIMEOUT_SECONDS = 15;

    private GoogleCalendarService() {
        // utility class
    }

    /**
     * Fetches today's calendar events from Google Calendar using the gws CLI.
     *
     * @return result containing the events or error information
     */
    public static GoogleCalendarResult fetchTodayEvents() {
        try {
            // Step 1: Get access token from gcloud
            final String accessToken = getAccessToken();
            if (accessToken == null || accessToken.isBlank()) {
                return GoogleCalendarResult.authError(
                        "Cannot obtain access token. Run 'gcloud auth application-default login' to authenticate.");
            }

            // Step 2: Build the gws command for today's events
            final String today = LocalDate.now().toString(); // YYYY-MM-DD
            final ZoneId zoneId = ZoneId.systemDefault();
            final String offset = OffsetDateTime.now(zoneId).getOffset().toString(); // e.g. +02:00

            final String params = String.format(
                    "{\"calendarId\": \"primary\", \"timeMin\": \"%sT00:00:00%s\", \"timeMax\": \"%sT23:59:59%s\", " +
                    "\"singleEvents\": true, \"orderBy\": \"startTime\", " +
                    "\"fields\": \"items(summary,start,end,location,hangoutLink,conferenceData)\"}",
                    today, offset, today, offset
            );

            final String json = executeGwsCommand(accessToken, params);
            if (json == null || json.isBlank()) {
                return GoogleCalendarResult.error("Empty response from gws calendar events list");
            }

            // Step 3: Check for errors in the response
            final JsonNode root = OBJECT_MAPPER.readTree(json);
            if (root.has("error")) {
                final String errorMessage = root.path("error").path("message").asText("Unknown error");
                if (errorMessage.contains("401") || errorMessage.contains("invalid_grant")
                        || errorMessage.contains("Token has been expired")
                        || errorMessage.contains("UNAUTHENTICATED")) {
                    return GoogleCalendarResult.authError("Token expired or invalid. Re-authenticate to fix.");
                }
                if (errorMessage.contains("403") || errorMessage.contains("accessNotConfigured")
                        || errorMessage.contains("PERMISSION_DENIED")) {
                    return GoogleCalendarResult.authError(
                            "Access denied. Run 'gcloud auth application-default login' with calendar scope.");
                }
                return GoogleCalendarResult.error(errorMessage);
            }

            // Step 4: Parse events
            return GoogleCalendarResult.success(parseEvents(root));

        } catch (final Exception e) {
            logger.warn("Failed to fetch Google Calendar events", e);
            final String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
            if (msg.contains("gcloud") || msg.contains("not found") || msg.contains("No such file")) {
                return GoogleCalendarResult.authError("gcloud CLI not found. Install Google Cloud SDK.");
            }
            return GoogleCalendarResult.error(msg);
        }
    }

    /**
     * Refreshes the gcloud application-default credentials by launching
     * {@code gcloud auth application-default login} in the user's terminal.
     */
    public static void refreshAuth() {
        try {
            final ProcessBuilder pb = new ProcessBuilder(
                    "open", "-a", "Terminal",
                    "gcloud", "auth", "application-default", "login",
                    "--scopes=https://www.googleapis.com/auth/calendar.readonly,https://www.googleapis.com/auth/cloud-platform"
            );
            pb.start();
            logger.info("GoogleCalendar: launched gcloud auth login in Terminal");
        } catch (final Exception e) {
            logger.warn("GoogleCalendar: failed to launch auth refresh", e);
            // Fallback: try running directly
            try {
                new ProcessBuilder(
                        "gcloud", "auth", "application-default", "login",
                        "--scopes=https://www.googleapis.com/auth/calendar.readonly,https://www.googleapis.com/auth/cloud-platform"
                ).start();
            } catch (final Exception ex) {
                logger.warn("GoogleCalendar: fallback auth also failed", ex);
            }
        }
    }

    private static String getAccessToken() throws Exception {
        final ProcessBuilder pb = new ProcessBuilder("gcloud", "auth", "application-default", "print-access-token");
        pb.redirectErrorStream(true);
        final Process process = pb.start();

        final String output;
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().reduce("", (a, b) -> a + b).trim();
        }

        final boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return null;
        }

        if (process.exitValue() != 0) {
            logger.warn("GoogleCalendar: gcloud print-access-token failed with exit code {}: {}",
                    process.exitValue(), output);
            return null;
        }

        return output;
    }

    private static String executeGwsCommand(final String accessToken, final String params) throws Exception {
        final ProcessBuilder pb = new ProcessBuilder(
                "gws", "calendar", "events", "list",
                "--params", params,
                "--format", "json"
        );
        pb.environment().put("GOOGLE_WORKSPACE_CLI_TOKEN", accessToken);
        pb.redirectErrorStream(true);

        final Process process = pb.start();
        final String output;
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            final StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            output = sb.toString().trim();
        }

        final boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            logger.warn("GoogleCalendar: gws command timed out");
            return null;
        }

        if (process.exitValue() != 0) {
            logger.warn("GoogleCalendar: gws command failed with exit code {}", process.exitValue());
            // Still return output — it may contain a JSON error we can parse
        }

        return output;
    }

    private static List<GoogleCalendarEvent> parseEvents(final JsonNode root) {
        final JsonNode items = root.path("items");
        if (items.isMissingNode() || !items.isArray()) {
            return Collections.emptyList();
        }

        final List<GoogleCalendarEvent> events = new ArrayList<>();
        for (final JsonNode item : items) {
            final String summary = item.path("summary").asText("(No title)");
            final String location = item.path("location").asText("");
            final String hangoutLink = item.path("hangoutLink").asText("");
            final String conferenceUri = extractConferenceVideoUri(item.path("conferenceData"));

            final JsonNode start = item.path("start");
            final JsonNode end = item.path("end");

            final boolean allDay = start.has("date") && !start.has("dateTime");

            final String startTime;
            final String endTime;
            final String rawStartDateTime;
            final String rawEndDateTime;

            if (allDay) {
                startTime = "All day";
                endTime = "";
                rawStartDateTime = "";
                rawEndDateTime = "";
            } else {
                startTime = formatDateTime(start.path("dateTime").asText(""));
                endTime = formatDateTime(end.path("dateTime").asText(""));
                rawStartDateTime = start.path("dateTime").asText("");
                rawEndDateTime = end.path("dateTime").asText("");
            }

            events.add(new GoogleCalendarEvent(summary, startTime, endTime, location, allDay, hangoutLink, conferenceUri, rawStartDateTime, rawEndDateTime));
        }

        return Collections.unmodifiableList(events);
    }

    private static String formatDateTime(final String isoDateTime) {
        if (isoDateTime == null || isoDateTime.isBlank()) {
            return "";
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

    /**
     * Extracts the video conference URI from {@code conferenceData.entryPoints}
     * where {@code entryPointType} is {@code "video"}.
     * This covers Zoom, Teams, and other add-on conferencing solutions that do not
     * populate the top-level {@code hangoutLink} field.
     */
    private static String extractConferenceVideoUri(final JsonNode conferenceData) {
        if (conferenceData == null || conferenceData.isMissingNode()) {
            return "";
        }
        final JsonNode entryPoints = conferenceData.path("entryPoints");
        if (!entryPoints.isArray()) {
            return "";
        }
        for (final JsonNode entry : entryPoints) {
            if ("video".equals(entry.path("entryPointType").asText(""))) {
                final String uri = entry.path("uri").asText("");
                if (!uri.isBlank()) {
                    return uri;
                }
            }
        }
        return "";
    }
}
