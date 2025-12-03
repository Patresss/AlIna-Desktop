package com.patres.alina.uidesktop.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

import static java.awt.event.KeyEvent.*;

public class SystemClipboard {

    private static final Logger logger = LoggerFactory.getLogger(SystemClipboard.class);

    public static String copySelectedValue() {

        try {

            // Try multiple times with delays between attempts for macOS
                try {
                    String result = tryToCopySelectedValue();
                    if (result != null && !result.trim().isEmpty()) {
                        logger.info("Successfully copied selected text: '{}' ({} chars)", result.substring(0, Math.min(100, result.length())), result.length());
                        return result;
                    }
                } catch (Exception e) {
                    logger.warn("Attempt failed: {}", e.getMessage());

                }
            logger.error("Failed to copy selected text");
            return "";
        } catch (Exception e) {
            logger.error("Cannot copy selected values", e);
            return "";
        }
    }

    private static String tryToCopySelectedValue() {
        try {
            String previousValueFromClipboard = SystemClipboard.get();
            logger.info("Previous clipboard value: '{}'", previousValueFromClipboard != null ? previousValueFromClipboard.substring(0, Math.min(100, previousValueFromClipboard.length())) : "null");


            logger.info("Sending Command+C to copy selected text...");

            SystemClipboard.copy();

            String selectedText = SystemClipboard.get();
            logger.info("Selected text after copy: '{}'", selectedText != null ? selectedText.substring(0, Math.min(100, selectedText.length())) : "null");

            // Restore previous clipboard value
            // How are you
//            if (previousValueFromClipboard != null) {
//                SystemClipboard.copy(previousValueFromClipboard);
//                logger.info("Restored previous clipboard value");
//            }

            // CRITICAL: Wait for the original application to stabilize after Control+Q was pressed
            // This gives time for the selection to remain active
            return selectedText;
        } catch (Exception e) {
            logger.error("Error in tryToCopySelectedValue", e);
            throw new RuntimeException(e);
        }
    }

    public static void copy(String text) {
        Clipboard clipboard = getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }

    public static void copy() {
        try {
            logger.info("Sending copy command (Cmd+C on macOS, Ctrl+C on others)");

            Robot robot = new Robot();
            int controlKey = isMacOS() ? VK_META : VK_CONTROL;
            robot.keyPress(controlKey);
            robot.keyPress(VK_C);
            Thread.sleep(1000);
            robot.keyRelease(VK_C);
            robot.keyRelease(controlKey);
            logger.info("Copy command sent, waiting for clipboard update");
        } catch (AWTException | InterruptedException e) {
            e.printStackTrace();
            // todo
        }

    }

    public static void paste() throws AWTException {
        Robot robot = new Robot();

        int controlKey = isMacOS() ? VK_META : VK_CONTROL;
        robot.keyPress(controlKey);
        robot.keyPress(VK_V);
        robot.keyRelease(VK_V);
        robot.keyRelease(controlKey);
    }

    private static boolean isMacOS() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
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
}