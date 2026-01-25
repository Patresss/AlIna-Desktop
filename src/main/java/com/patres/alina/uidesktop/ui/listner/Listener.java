package com.patres.alina.uidesktop.ui.listner;


import com.github.kwhat.jnativehook.GlobalScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.logging.Level;


public class Listener {

    private static final Logger logger = LoggerFactory.getLogger(Listener.class);
    private static volatile boolean nativeHookAvailable = false;

    static {
        init();
    }

    public static void init() {
        // Suppress JNativeHook's verbose logging
        java.util.logging.Logger jnhLogger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
        jnhLogger.setLevel(Level.WARNING);
        jnhLogger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
            nativeHookAvailable = true;
            logger.info("GlobalScreen native hook registered successfully");
        } catch (Throwable e) {
            logger.warn("GlobalScreen native hook disabled: {}", e.getMessage());
            nativeHookAvailable = false;
        }
    }

    public static boolean isNativeHookAvailable() {
        return nativeHookAvailable;
    }

}
