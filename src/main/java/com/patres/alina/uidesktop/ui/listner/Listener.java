package com.patres.alina.uidesktop.ui.listner;


import com.github.kwhat.jnativehook.GlobalScreen;
import com.patres.alina.uidesktop.shortcuts.NativeLibraryExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Listener {

    private static final Logger logger = LoggerFactory.getLogger(Listener.class);
    private static volatile boolean nativeHookAvailable = false;

    static {
        init();
    }

    public static void init() {
        // Extract native libraries BEFORE loading GlobalScreen class
        NativeLibraryExtractor.extractNativeLibraries();

        // Suppress JNativeHook's verbose logging - use hardcoded package name
        // to avoid loading GlobalScreen class before native lib path is set
        java.util.logging.Logger jnhLogger = java.util.logging.Logger.getLogger("com.github.kwhat.jnativehook");
        jnhLogger.setLevel(java.util.logging.Level.WARNING);
        jnhLogger.setUseParentHandlers(false);

        // Rejestracja natywnych hooków może się nie udać w środowiskach z niestandardowym classloaderem (np. zainstalowana app)
        try {
            GlobalScreen.registerNativeHook();
            nativeHookAvailable = true;
            logger.info("GlobalScreen native hook registered successfully");
        } catch (Throwable e) {
            logger.error("GlobalScreen native hook disabled", e);
            nativeHookAvailable = false;
        }
    }

    public static boolean isNativeHookAvailable() {
        return nativeHookAvailable;
    }

}
