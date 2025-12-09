package com.patres.alina.uidesktop.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SystemClipboard {

    private static final Logger logger = LoggerFactory.getLogger(SystemClipboard.class);
    private static final int SHORTCUT_HOLD_MS = 220;
    private static final int SHORTCUT_HOLD_MS_STRONG = 350;
    private static final int CLIPBOARD_CHANGE_TIMEOUT_MS = 800;
    private static final int CLIPBOARD_POLL_INTERVAL_MS = 20;

    private static final ShortcutExecutor SHORTCUT_EXECUTOR = OsUtils.isMacOS()
            ? new MacShortcutExecutor()
            : new RobotShortcutExecutor();

    public static String copySelectedValue() {

        try {

            // Try twice to tolerate short OS delays when copying from other apps
            for (int attempt = 1; attempt <= 2; attempt++) {
                try {
                    int holdMs = attempt == 1 ? SHORTCUT_HOLD_MS : SHORTCUT_HOLD_MS_STRONG;
                    String result = tryToCopySelectedValue(holdMs);
                    if (result != null) {
                        logger.info("Successfully copied selected text: '{}' ({} chars)", result.substring(0, Math.min(100, result.length())), result.length());
                        return result;
                    }
                    logger.warn("Copy attempt {} returned empty result", attempt);
                } catch (Exception e) {
                    logger.warn("Copy attempt {} failed: {}", attempt, e.getMessage());
                }
            }
            logger.error("Failed to copy selected text");
            return "";
        } catch (Exception e) {
            logger.error("Cannot copy selected values", e);
            return "";
        }
    }

    private static String tryToCopySelectedValue(int holdMs) {
        final Clipboard clipboard = getSystemClipboard();
        final String previousValueFromClipboard = getQuietly(clipboard);
        logger.info("Previous clipboard value: '{}'", preview(previousValueFromClipboard));

        logger.info("Sending Command+C to copy selected text...");
        if (!SHORTCUT_EXECUTOR.sendCopy(holdMs)) {
            logger.error("Cannot send copy shortcut");
            return null;
        }

        final String selectedText = waitForClipboardChange(clipboard, previousValueFromClipboard);
        logger.info("Selected text after copy: '{}'", preview(selectedText));
        return selectedText;
    }

    public static void copy(String text) {
        Clipboard clipboard = getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }

    public static void copyAndPaste(String text) {
        Clipboard clipboard = getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
        waitUntilClipboardEquals(clipboard, text);
        try {
            SHORTCUT_EXECUTOR.sendPaste(SHORTCUT_HOLD_MS);
        } catch (Exception e) {
            logger.error("Cannot send paste shortcut", e);
        }
    }

    public static void paste() {
        SHORTCUT_EXECUTOR.sendPaste(SHORTCUT_HOLD_MS);
    }

    public static String get() throws Exception {
        Clipboard systemClipboard = getSystemClipboard();
        DataFlavor dataFlavor = DataFlavor.stringFlavor;

        if (systemClipboard.isDataFlavorAvailable(dataFlavor)) {
            Object text = systemClipboard.getData(dataFlavor);
            return (String) text;
        }

        return null;
    }

    private static Clipboard getSystemClipboard() {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        return defaultToolkit.getSystemClipboard();
    }

    private static String waitForClipboardChange(Clipboard clipboard, String previousContent) {
        final CountDownLatch updated = new CountDownLatch(1);
        final FlavorListener listener = e -> updated.countDown();
        clipboard.addFlavorListener(listener);
        try {
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CLIPBOARD_CHANGE_TIMEOUT_MS);
            String current = getQuietly(clipboard);
            boolean changeDetected = false;

            while (System.nanoTime() < deadline) {
                if (updated.await(CLIPBOARD_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)) {
                    changeDetected = true;
                    current = getQuietly(clipboard);
                    if (current != null) {
                        return current;
                    }
                }

                current = getQuietly(clipboard);
                if (clipboardChanged(previousContent, current)) {
                    changeDetected = true;
                    return current;
                }
            }
            return changeDetected ? current : null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            clipboard.removeFlavorListener(listener);
        }
    }

    private static void waitUntilClipboardEquals(Clipboard clipboard, String expected) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CLIPBOARD_CHANGE_TIMEOUT_MS);
        while (System.nanoTime() < deadline) {
            String current = getQuietly(clipboard);
            if (Objects.equals(expected, current)) {
                return;
            }
            try {
                Thread.sleep(CLIPBOARD_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static boolean clipboardChanged(String previousContent, String currentContent) {
        if (currentContent == null) {
            return false;
        }
        if (previousContent == null) {
            return true;
        }
        return !Objects.equals(previousContent, currentContent);
    }

    private static String getQuietly(Clipboard clipboard) {
        try {
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                Object text = clipboard.getData(DataFlavor.stringFlavor);
                return (String) text;
            }
        } catch (Exception e) {
            logger.warn("Cannot read clipboard", e);
        }
        return null;
    }

    private static String preview(String value) {
        if (value == null) {
            return "null";
        }
        return value.substring(0, Math.min(100, value.length()));
    }
}
