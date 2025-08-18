package com.patres.alina.uidesktop.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

import static com.patres.alina.uidesktop.RetryLogic.runWithRetry;
import static java.awt.event.KeyEvent.*;

public class SystemClipboard {

    private static final Logger logger = LoggerFactory.getLogger(SystemClipboard.class);

    public static String copySelectedValue() {
        try {
            return runWithRetry(SystemClipboard::tryToCopySelectedValue, 3);
        } catch (Exception e) {
            logger.error("Cannot copy selected values", e);
            return "";
        }
    }

    private static String tryToCopySelectedValue() {
        try {
            String previousValueFromClipboard = SystemClipboard.get();
            SystemClipboard.copy();
            String selectedText = SystemClipboard.get();
            SystemClipboard.copy(previousValueFromClipboard);
            return selectedText;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void copy(String text) {
        Clipboard clipboard = getSystemClipboard();
        clipboard.setContents(new StringSelection(text), null);
    }

    public static void copy() throws AWTException, InterruptedException {
        Robot robot = new Robot();

//        int controlKey = IS_OS_MAC ? VK_META : VK_CONTROL;
        int controlKey = VK_CONTROL;
        robot.keyPress(controlKey);
        robot.keyPress(VK_C);
        robot.keyRelease(VK_C);
        robot.keyRelease(controlKey);
        Thread.sleep(100L);
    }

    public static void paste() throws AWTException {
        Robot robot = new Robot();

//        int controlKey = IS_OS_MAC ? VK_META : VK_CONTROL;
        int controlKey = VK_CONTROL;
        robot.keyPress(controlKey);
        robot.keyPress(VK_V);
        robot.keyRelease(controlKey);
        robot.keyRelease(VK_V);
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