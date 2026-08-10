package com.patres.alina.uidesktop.mascot;

import java.util.regex.Pattern;

final class MascotTerminalText {

    private static final Pattern AUTOMATIC_THREAD_TITLE = Pattern.compile(
            "^\\d{4}-\\d{2}-\\d{2} \\(\\d{2}:\\d{2}:\\d{2}\\)$"
    );

    private MascotTerminalText() {
    }

    static String detail(final MascotNotification notification) {
        if (notification == null) {
            return "";
        }
        final String message = normalize(notification.message());
        if (notification.type() == MascotNotificationType.ERROR && !message.isBlank()) {
            return message;
        }
        final String title = normalize(notification.title());
        return title.isBlank() || isAutomaticThreadTitle(title) ? "" : title;
    }

    static boolean isAutomaticThreadTitle(final String title) {
        return AUTOMATIC_THREAD_TITLE.matcher(normalize(title)).matches();
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }
}
