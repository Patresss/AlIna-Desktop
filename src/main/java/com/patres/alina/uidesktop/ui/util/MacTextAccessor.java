package com.patres.alina.uidesktop.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Reads selected text and manages app focus on macOS using the Accessibility API
 * via AppleScript. This avoids clipboard-based copy/paste race conditions by using
 * the AXSelectedText attribute directly.
 */
public class MacTextAccessor {

    private static final Logger logger = LoggerFactory.getLogger(MacTextAccessor.class);
    private static final int APPLESCRIPT_TIMEOUT_SECONDS = 3;

    /**
     * Holds the captured context: selected text and the name of the source application.
     */
    public record CapturedContext(String selectedText, String sourceAppName) {

        public boolean hasText() {
            return selectedText != null && !selectedText.isBlank();
        }
    }

    /**
     * Captures the currently selected text and source app name in a single AppleScript call.
     * Uses macOS Accessibility API (AXSelectedText) - no clipboard involved.
     * Must be called while the source app is still frontmost (before showing any window).
     *
     * Falls back to clipboard-based copy if AXSelectedText is not available.
     */
    public static CapturedContext captureContext() {
        String[] result = captureViaAccessibility();
        String appName = result[0];
        String selectedText = result[1];

        logger.info("Frontmost app: {}", appName);

        if (selectedText != null) {
            logger.info("Got selected text via AXSelectedText ({} chars): '{}'",
                    selectedText.length(), preview(selectedText));
            return new CapturedContext(selectedText, appName);
        }

        logger.info("AXSelectedText not available, falling back to clipboard copy");
        String clipboardText = SystemClipboard.copySelectedValue();
        return new CapturedContext(clipboardText, appName);
    }

    /**
     * Gets both the frontmost app name and the selected text in a single AppleScript call.
     * Returns [appName, selectedText] where selectedText may be null.
     */
    private static String[] captureViaAccessibility() {
        // Single script that gets both app name and selected text.
        // Uses AXFocusedUIElement attribute to find the focused element,
        // then reads AXSelectedText from it.
        String script = String.join("\n",
                "tell application \"System Events\"",
                "  set frontProc to first application process whose frontmost is true",
                "  set appName to name of frontProc",
                "  set selText to \"\"",
                "  try",
                "    set focusedElem to value of attribute \"AXFocusedUIElement\" of frontProc",
                "    set selText to value of attribute \"AXSelectedText\" of focusedElem",
                "  end try",
                "  return appName & \"|<DELIM>|\" & selText",
                "end tell"
        );

        String output = runAppleScript(script);
        if (output == null) {
            return new String[]{null, null};
        }

        int delimIdx = output.indexOf("|<DELIM>|");
        if (delimIdx < 0) {
            // Delimiter not found — entire output is the app name, no text captured
            return new String[]{output, null};
        }

        String appName = output.substring(0, delimIdx);
        String text = output.substring(delimIdx + 9); // length of "|<DELIM>|"
        if (text.isEmpty()) {
            return new String[]{appName, null};
        }
        return new String[]{appName, text};
    }

    /**
     * Activates the specified application and waits briefly for it to become frontmost.
     */
    public static boolean activateApp(String appName) {
        if (appName == null || appName.isBlank()) {
            return false;
        }
        String escaped = appName.replace("\\", "\\\\").replace("\"", "\\\"");
        String script = "tell application \"" + escaped + "\" to activate\n"
                + "delay 0.15\n"
                + "return \"OK\"";
        String result = runAppleScript(script);
        boolean success = "OK".equals(result);
        if (success) {
            logger.info("Activated app: {}", appName);
        } else {
            logger.warn("Failed to activate app: {}", appName);
        }
        return success;
    }

    /**
     * Runs an AppleScript via stdin to avoid quoting issues with -e flag.
     */
    private static String runAppleScript(String script) {
        Process process = null;
        try {
            process = new ProcessBuilder("osascript", "-")
                    .redirectErrorStream(false)
                    .start();
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(script.getBytes(StandardCharsets.UTF_8));
            }
            boolean finished = process.waitFor(APPLESCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.warn("AppleScript timed out");
                return null;
            }
            if (process.exitValue() != 0) {
                String err = new String(process.getErrorStream().readAllBytes()).trim();
                logger.warn("AppleScript error (exit {}): {}", process.exitValue(), err);
                return null;
            }
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            logger.warn("AppleScript execution failed: {}", e.getMessage());
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static String preview(String value) {
        if (value == null) return "null";
        return value.substring(0, Math.min(100, value.length()));
    }
}
