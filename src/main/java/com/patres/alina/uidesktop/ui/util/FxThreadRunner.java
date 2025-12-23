package com.patres.alina.uidesktop.ui.util;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class FxThreadRunner {

    private static final Logger logger = LoggerFactory.getLogger(FxThreadRunner.class);

    private FxThreadRunner() {
    }

    public static void run(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        Platform.runLater(action);
    }

    public static void runAndWait(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                logger.warn("Timed out waiting for JavaFX thread to apply UI state");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
