package com.patres.alina.uidesktop.ui.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

import static java.awt.event.KeyEvent.VK_ALT;
import static java.awt.event.KeyEvent.VK_CONTROL;
import static java.awt.event.KeyEvent.VK_C;
import static java.awt.event.KeyEvent.VK_META;
import static java.awt.event.KeyEvent.VK_SHIFT;
import static java.awt.event.KeyEvent.VK_V;

public class RobotShortcutExecutor implements ShortcutExecutor {

    private static final Logger logger = LoggerFactory.getLogger(RobotShortcutExecutor.class);

    @Override
    public boolean sendCopy() {
        return sendShortcut(VK_C);
    }

    @Override
    public boolean sendPaste() {
        return sendShortcut(VK_V);
    }


    private boolean sendShortcut(int keyCode) {
        try {
            Robot robot = createRobot();
            normalizeModifiers(robot);

            int controlKey = OsUtils.isMacOS() ? VK_META : VK_CONTROL;
            robot.keyPress(controlKey);
            robot.delay(PRE_KEY_DELAY_MS);
            robot.keyPress(keyCode);
            robot.delay(DEFAULT_HOLD_MS);
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

}
