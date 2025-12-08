package com.patres.alina.uidesktop.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class MacShortcutExecutor implements ShortcutExecutor {

    private static final Logger logger = LoggerFactory.getLogger(MacShortcutExecutor.class);

    private static final int APPLESCRIPT_SETTLE_MS = 60;
    private static final int APPLESCRIPT_TIMEOUT_MS = 800;
    private static final String KEY_COPY = "c";
    private static final String KEY_PASTE = "v";

    private final ShortcutExecutor fallback = new RobotShortcutExecutor();

    @Override
    public boolean sendCopy(int holdMs) {
        if (sendMacShortcut(KEY_COPY)) {
            return true;
        }
        logger.warn("Falling back to Robot for copy");
        return fallback.sendCopy(holdMs);
    }

    @Override
    public boolean sendPaste(int holdMs) {
        if (sendMacShortcut(KEY_PASTE)) {
            return true;
        }
        logger.warn("Falling back to Robot for paste");
        return fallback.sendPaste(holdMs);
    }

    private boolean sendMacShortcut(String key) {
        Process process = null;
        try {
            String script = "tell application \"System Events\" to keystroke \"" + key + "\" using {command down}";
            process = new ProcessBuilder("osascript", "-e", script).start();
            boolean finished = process.waitFor(APPLESCRIPT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.warn("AppleScript timed out for key {}", key);
                return false;
            }
            if (process.exitValue() != 0) {
                logger.warn("AppleScript failed for key {} with exit {}", key, process.exitValue());
                return false;
            }
            Thread.sleep(APPLESCRIPT_SETTLE_MS);
            return true;
        } catch (Exception e) {
            logger.warn("AppleScript failed for key {}: {}", key, e.getMessage());
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
