package com.patres.alina.server.integration;

import com.patres.alina.uidesktop.ui.util.OsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class GoogleCalendarCli {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarCli.class);
    private static final long COMMAND_TIMEOUT_SECONDS = 15;
    private static final String GCLOUD_EXECUTABLE = "gcloud";
    private static final String GWS_EXECUTABLE = "gws";
    private static final String GOOGLE_WORKSPACE_TOKEN_ENV = "GOOGLE_WORKSPACE_CLI_TOKEN";
    private static final String PRIMARY_CALENDAR_ID = "primary";
    private static final String EVENT_FIELDS = "items(summary,description,start,end,location,hangoutLink,conferenceData)";
    private static final String GCLOUD_MISSING_MESSAGE =
            "Google Calendar requires gcloud CLI for authentication. Install Google Cloud SDK and make sure the app can access it in PATH.";
    private static final String GWS_MISSING_MESSAGE =
            "Google Calendar requires gws CLI. Install gws and make sure the app can access it in PATH.";
    private static final String CALENDAR_CLI_MISSING_MESSAGE =
            "Google Calendar CLI not found. Make sure both gws and gcloud are installed and available to the app.";
    private static final String ACCESS_TOKEN_MISSING_MESSAGE =
            "Cannot obtain access token. Run 'gcloud auth application-default login' to authenticate.";
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
        final CommandExecutionResult result = runCommand(List.of(
                GWS_EXECUTABLE, "calendar", "events", "list",
                "--params", buildTodayEventsParams(),
                "--format", "json"
        ), GOOGLE_WORKSPACE_TOKEN_ENV, accessToken);
        if (!result.finished()) {
            logger.warn("GoogleCalendar: gws command timed out");
            return null;
        }
        if (result.exitCode() != 0) {
            logger.warn("GoogleCalendar: gws command failed with exit code {}", result.exitCode());
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
        if (message.contains(GWS_EXECUTABLE)) {
            return GoogleCalendarResult.error(GWS_MISSING_MESSAGE);
        }
        if (message.contains(GCLOUD_EXECUTABLE)) {
            return GoogleCalendarResult.error(GCLOUD_MISSING_MESSAGE);
        }
        if (message.contains("No such file") || message.contains("not found")) {
            return GoogleCalendarResult.error(CALENDAR_CLI_MISSING_MESSAGE);
        }
        return GoogleCalendarResult.error(message);
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

    private static String buildTodayEventsParams() {
        final String today = LocalDate.now().toString();
        final String offset = OffsetDateTime.now(ZoneId.systemDefault()).getOffset().toString();
        return String.format(
                "{\"calendarId\": \"%s\", \"timeMin\": \"%sT00:00:00%s\", \"timeMax\": \"%sT23:59:59%s\", "
                        + "\"singleEvents\": true, \"orderBy\": \"startTime\", "
                        + "\"fields\": \"%s\"}",
                PRIMARY_CALENDAR_ID, today, offset, today, offset, EVENT_FIELDS
        );
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

    private static CommandExecutionResult runCommand(final List<String> command, final String envName, final String envValue)
            throws Exception {
        final ProcessBuilder pb = new ProcessBuilder(command);
        if (envName != null && envValue != null) {
            pb.environment().put(envName, envValue);
        }
        pb.redirectErrorStream(true);

        final Process process = pb.start();
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

    private record CommandExecutionResult(int exitCode, String output, boolean finished) {
    }
}
