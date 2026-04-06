package com.patres.alina.uidesktop.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Sends native OS notifications without stealing focus.
 * On macOS uses osascript; on other platforms logs a message (no-op).
 */
public final class SystemNotification {

    private static final Logger logger = LoggerFactory.getLogger(SystemNotification.class);
    private static final int TIMEOUT_SECONDS = 3;

    private SystemNotification() {
    }

    /**
     * Sends a notification asynchronously. Never blocks or steals focus.
     */
    public static void send(String title, String message) {
        CompletableFuture.runAsync(() -> {
            try {
                if (OsUtils.isMacOS()) {
                    sendMacNotification(title, message);
                } else {
                    logger.info("Notification: [{}] {}", title, message);
                }
            } catch (Exception e) {
                logger.debug("Failed to send notification: {}", e.getMessage());
            }
        });
    }

    private static void sendMacNotification(String title, String message) {
        String escapedTitle = escapeAppleScript(title);
        String escapedMessage = escapeAppleScript(message);
        String script = "display notification \"" + escapedMessage + "\" with title \"" + escapedTitle + "\"";

        try {
            Process process = new ProcessBuilder("osascript", "-e", script)
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.debug("Notification script timed out");
            } else if (process.exitValue() != 0) {
                String output = new String(process.getInputStream().readAllBytes()).trim();
                logger.debug("Notification script failed (exit {}): {}", process.exitValue(), output);
            } else {
                logger.debug("macOS notification sent: [{}] {}", title, message);
            }
        } catch (Exception e) {
            logger.debug("Failed to send macOS notification: {}", e.getMessage());
        }
    }

    private static String escapeAppleScript(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
