package com.patres.alina.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.uidesktop.ui.util.OsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

final class GoogleCalendarCli {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarCli.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long COMMAND_TIMEOUT_SECONDS = 15;
    private static final String GCLOUD_EXECUTABLE = "gcloud";
    private static final String CURL_EXECUTABLE = "curl";
    private static final String GOOGLE_APPLICATION_CREDENTIALS_ENV = "GOOGLE_APPLICATION_CREDENTIALS";
    private static final String ADC_QUOTA_PROJECT_NODE = "quota_project_id";
    private static final String CALENDAR_EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final String EVENT_FIELDS = "items(summary,description,start,end,location,hangoutLink,conferenceData)";
    private static final String GCLOUD_MISSING_MESSAGE =
            "Google Calendar requires gcloud CLI for authentication. Install Google Cloud SDK and make sure the app can access it in PATH.";
    private static final String CURL_MISSING_MESSAGE =
            "Google Calendar requires curl for API requests. Install curl and make sure the app can access it in PATH.";
    private static final String CALENDAR_CLI_MISSING_MESSAGE =
            "Google Calendar tools not found. Make sure both gcloud and curl are installed and available to the app.";
    private static final String ACCESS_TOKEN_MISSING_MESSAGE =
            "Cannot obtain access token. Run 'gcloud auth application-default login --scopes=https://www.googleapis.com/auth/calendar.readonly,https://www.googleapis.com/auth/cloud-platform' to authenticate.";
    private static final String CALENDAR_AUTH_COMMAND =
            GCLOUD_EXECUTABLE + " auth application-default login --scopes=https://www.googleapis.com/auth/calendar.readonly,https://www.googleapis.com/auth/cloud-platform";

    private GoogleCalendarCli() {
    }

    static String getAccessToken() throws Exception {
        final CommandExecutionResult result = runCommand(List.of(
                GCLOUD_EXECUTABLE, "auth", "application-default", "print-access-token"
        ));
        if (!result.finished()) {
            logger.warn("GoogleCalendar: gcloud print-access-token timed out");
            return null;
        }
        if (result.exitCode() != 0) {
            logger.warn("GoogleCalendar: gcloud print-access-token failed with exit code {}: {}",
                    result.exitCode(), result.output());
            return null;
        }
        return result.output();
    }

    static String fetchTodayEventsJson(final String accessToken) throws Exception {
        final CommandExecutionResult result = runCommand(
                buildCalendarEventsCommand(),
                buildCalendarEventsCurlConfig(accessToken)
        );
        if (!result.finished()) {
            logger.warn("GoogleCalendar: curl command timed out");
            return null;
        }
        if (result.exitCode() != 0) {
            logger.warn("GoogleCalendar: curl command failed with exit code {}", result.exitCode());
        }
        return result.output();
    }

    static String accessTokenMissingMessage() {
        return ACCESS_TOKEN_MISSING_MESSAGE;
    }

    static String calendarAuthCommand() {
        return CALENDAR_AUTH_COMMAND;
    }

    static GoogleCalendarResult mapExceptionToResult(final Exception exception) {
        final String message = exception.getMessage() != null ? exception.getMessage() : "Unknown error";
        if (message.contains(CURL_EXECUTABLE)) {
            return GoogleCalendarResult.error(CURL_MISSING_MESSAGE);
        }
        if (message.contains(GCLOUD_EXECUTABLE)) {
            return GoogleCalendarResult.error(GCLOUD_MISSING_MESSAGE);
        }
        if (message.contains("No such file") || message.contains("not found")) {
            return GoogleCalendarResult.error(CALENDAR_CLI_MISSING_MESSAGE);
        }
        return GoogleCalendarResult.error(message);
    }

    private static List<String> buildCalendarEventsCommand() {
        return List.of(
                CURL_EXECUTABLE,
                "-sS",
                "--max-time", String.valueOf(COMMAND_TIMEOUT_SECONDS),
                "-G",
                CALENDAR_EVENTS_URL,
                "--config", "-"
        );
    }

    private static String buildCalendarEventsCurlConfig(final String accessToken) {
        final CalendarTimeRange range = buildTodayTimeRange();
        final StringBuilder config = new StringBuilder();
        appendCurlConfig(config, "header", "Authorization: Bearer " + accessToken);
        readQuotaProjectId().ifPresent(projectId ->
                appendCurlConfig(config, "header", "x-goog-user-project: " + projectId));
        appendCurlConfig(config, "data-urlencode", "timeMin=" + range.timeMin());
        appendCurlConfig(config, "data-urlencode", "timeMax=" + range.timeMax());
        appendCurlConfig(config, "data-urlencode", "singleEvents=true");
        appendCurlConfig(config, "data-urlencode", "orderBy=startTime");
        appendCurlConfig(config, "data-urlencode", "fields=" + EVENT_FIELDS);
        return config.toString();
    }

    private static CalendarTimeRange buildTodayTimeRange() {
        final String today = LocalDate.now().toString();
        final String offset = OffsetDateTime.now(ZoneId.systemDefault()).getOffset().toString();
        return new CalendarTimeRange(
                today + "T00:00:00" + offset,
                today + "T23:59:59" + offset
        );
    }

    private static Optional<String> readQuotaProjectId() {
        final Optional<Path> credentialsPath = adcCredentialsPath();
        if (credentialsPath.isEmpty()) {
            return Optional.empty();
        }
        try {
            final JsonNode credentials = OBJECT_MAPPER.readTree(credentialsPath.get().toFile());
            final String quotaProjectId = credentials.path(ADC_QUOTA_PROJECT_NODE).asText("");
            if (quotaProjectId.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(quotaProjectId);
        } catch (final Exception e) {
            logger.debug("GoogleCalendar: cannot read ADC quota project", e);
            return Optional.empty();
        }
    }

    private static Optional<Path> adcCredentialsPath() {
        final String explicitPath = System.getenv(GOOGLE_APPLICATION_CREDENTIALS_ENV);
        if (explicitPath != null && !explicitPath.isBlank()) {
            final Path path = Path.of(explicitPath);
            if (Files.isRegularFile(path)) {
                return Optional.of(path);
            }
        }

        final String home = System.getProperty("user.home");
        if (home == null || home.isBlank()) {
            return Optional.empty();
        }
        final Path defaultPath = Path.of(home, ".config", "gcloud", "application_default_credentials.json");
        return Files.isRegularFile(defaultPath) ? Optional.of(defaultPath) : Optional.empty();
    }

    private static void appendCurlConfig(final StringBuilder config, final String option, final String value) {
        config.append(option)
                .append(" = \"")
                .append(escapeCurlConfigValue(value))
                .append("\"\n");
    }

    static void refreshAuth() {
        try {
            if (OsUtils.isMacOS()) {
                launchMacAuthInTerminal();
            } else {
                launchDirectAuthProcess();
            }
        } catch (final Exception e) {
            logger.warn("GoogleCalendar: failed to launch auth refresh", e);
            try {
                launchDirectAuthProcess();
            } catch (final Exception ex) {
                logger.warn("GoogleCalendar: fallback auth also failed", ex);
            }
        }
    }

    private static void launchMacAuthInTerminal() throws IOException {
        final String script = "tell application \"Terminal\" to activate\n"
                + "tell application \"Terminal\" to do script \"" + escapeAppleScript(CALENDAR_AUTH_COMMAND) + "\"";
        new ProcessBuilder("osascript", "-e", script).start();
        logger.info("GoogleCalendar: launched gcloud auth login in Terminal");
    }

    private static void launchDirectAuthProcess() throws IOException {
        new ProcessBuilder(
                GCLOUD_EXECUTABLE, "auth", "application-default", "login",
                "--scopes=https://www.googleapis.com/auth/calendar.readonly,https://www.googleapis.com/auth/cloud-platform"
        ).start();
        logger.info("GoogleCalendar: launched gcloud auth login process");
    }

    private static CommandExecutionResult runCommand(final List<String> command) throws Exception {
        return runCommand(command, null, null);
    }

    private static CommandExecutionResult runCommand(final List<String> command, final String stdIn) throws Exception {
        return runCommand(command, null, null, stdIn);
    }

    private static CommandExecutionResult runCommand(final List<String> command, final String envName, final String envValue)
            throws Exception {
        return runCommand(command, envName, envValue, null);
    }

    private static CommandExecutionResult runCommand(final List<String> command,
                                                    final String envName,
                                                    final String envValue,
                                                    final String stdIn)
            throws Exception {
        final ProcessBuilder pb = new ProcessBuilder(command);
        if (envName != null && envValue != null) {
            pb.environment().put(envName, envValue);
        }
        pb.redirectErrorStream(true);

        final Process process = pb.start();
        if (stdIn != null) {
            try (final OutputStreamWriter writer =
                         new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(stdIn);
            }
        }
        final String output = readProcessOutput(process);
        final boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new CommandExecutionResult(-1, output, false);
        }
        return new CommandExecutionResult(process.exitValue(), output, true);
    }

    private static String readProcessOutput(final Process process) throws IOException {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            final StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append('\n');
            }
            return output.toString().trim();
        }
    }

    private static String escapeAppleScript(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String escapeCurlConfigValue(final String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record CommandExecutionResult(int exitCode, String output, boolean finished) {
    }

    private record CalendarTimeRange(String timeMin, String timeMax) {
    }
}
