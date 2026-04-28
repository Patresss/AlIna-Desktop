package com.patres.alina.server.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for fetching today's Google Calendar events via the {@code gws} CLI.
 */
public final class GoogleCalendarService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final String EMPTY_RESPONSE_MESSAGE = "Empty response from gws calendar events list";

    private GoogleCalendarService() {
        // utility class
    }

    public static GoogleCalendarResult fetchTodayEvents() {
        try {
            final String accessToken = GoogleCalendarCli.getAccessToken();
            if (accessToken == null || accessToken.isBlank()) {
                return GoogleCalendarResult.authError(GoogleCalendarCli.accessTokenMissingMessage());
            }

            final String json = GoogleCalendarCli.fetchTodayEventsJson(accessToken);
            if (json == null || json.isBlank()) {
                return GoogleCalendarResult.error(EMPTY_RESPONSE_MESSAGE);
            }

            return GoogleCalendarResponseParser.parse(json);
        } catch (final Exception e) {
            logger.warn("Failed to fetch Google Calendar events", e);
            return GoogleCalendarCli.mapExceptionToResult(e);
        }
    }

    public static void refreshAuth() {
        GoogleCalendarCli.refreshAuth();
    }
}
