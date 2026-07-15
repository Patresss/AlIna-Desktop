package com.patres.alina.uidesktop.ui.calendar;

import com.patres.alina.server.integration.GoogleCalendarEvent;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Resolves and validates external links exposed by Calendar cards. */
public final class CalendarEventLinkResolver {

    private CalendarEventLinkResolver() {
    }

    public static Optional<String> resolveJoinUrl(final GoogleCalendarEvent event) {
        final List<String> candidates = List.of(
                event.hangoutLink(),
                event.conferenceUri(),
                event.descriptionVideoUrl(),
                firstLocationPart(event.location())
        );
        return candidates.stream()
                .map(CalendarEventLinkResolver::safeHttpUrl)
                .flatMap(Optional::stream)
                .findFirst();
    }

    public static Optional<String> safeHttpUrl(final String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            final URI uri = URI.create(value.strip());
            final String scheme = uri.getScheme();
            if (scheme == null || uri.getHost() == null) {
                return Optional.empty();
            }
            final String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            if (!normalizedScheme.equals("http") && !normalizedScheme.equals("https")) {
                return Optional.empty();
            }
            return Optional.of(uri.toString());
        } catch (final IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static String firstLocationPart(final String location) {
        if (location == null || location.isBlank()) {
            return "";
        }
        return location.split(",", 2)[0].strip();
    }
}
