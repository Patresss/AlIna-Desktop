package com.patres.alina.uidesktop;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public class RetryLogic {

    private static final Logger logger = LoggerFactory.getLogger(RetryLogic.class);


    public static <T> T runWithRetry(final Supplier<T> method, final int maxNumberOfTries) {
        int numberOfTries = 0;
        while (++numberOfTries < maxNumberOfTries - 1) {
            try {
                return method.get();
            } catch (Exception e) {
                logger.warn("RetryLogic cannot process the method #" + numberOfTries + " max tries: maxNumberOfTries");
            }
        }
        return method.get();
    }

    public static void runWithRetry(final Runnable method, final int maxNumberOfTries) {
        int numberOfTries = 0;
        while (++numberOfTries < maxNumberOfTries - 1) {
            try {
                method.run();
                return;
            } catch (Exception e) {
                logger.warn("RetryLogic cannot process the method #" + numberOfTries + " max tries: maxNumberOfTries");
            }
        }
        method.run();
    }
}
