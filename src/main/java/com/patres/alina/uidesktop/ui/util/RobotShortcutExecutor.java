package com.patres.alina.uidesktop.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

import static java.awt.event.KeyEvent.*;

public class RobotShortcutExecutor implements ShortcutExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RobotShortcutExecutor.class);

    private static final int STEP_DELAY_MS = 60;
    private static final int PRE_KEY_DELAY_MS = 80;

    @Override
    public boolean sendCopy(int holdMs) {
        return sendShortcut(VK_C, holdMs);
    }

    @Override
    public boolean sendPaste(int holdMs) {
        return sendShortcut(VK_V, holdMs);
    }

    private boolean sendShortcut(int keyCode, int holdMs) {
        try {
            Robot robot = createRobot();
            normalizeModifiers(robot);

            int controlKey = isMacOS() ? VK_META : VK_CONTROL;
            robot.keyPress(controlKey);
            robot.delay(PRE_KEY_DELAY_MS);
            robot.keyPress(keyCode);
            robot.delay(holdMs);
            robot.keyRelease(keyCode);
            robot.delay(STEP_DELAY_MS);
            robot.keyRelease(controlKey);
            robot.waitForIdle();
            return true;
        } catch (Exception e) {
            logger.error("Robot shortcut failed", e);
            return false;
        }
    }

    private Robot createRobot() throws AWTException {
        Robot robot = new Robot();
        robot.setAutoWaitForIdle(true);
        robot.setAutoDelay(STEP_DELAY_MS);
        return robot;
    }

    private void normalizeModifiers(Robot robot) {
        releaseIfPressed(robot, VK_SHIFT);
        releaseIfPressed(robot, VK_ALT);
        releaseIfPressed(robot, VK_META);
        releaseIfPressed(robot, VK_CONTROL);
        robot.delay(STEP_DELAY_MS);
    }

    private void releaseIfPressed(Robot robot, int keyCode) {
        try {
            robot.keyRelease(keyCode);
        } catch (IllegalArgumentException ignored) {
            // ignore invalid keyCode
        }
    }

    private boolean isMacOS() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }
}
