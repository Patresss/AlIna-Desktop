package com.patres.alina.uidesktop.ui.listner;


import com.github.kwhat.jnativehook.GlobalScreen;
import com.patres.alina.uidesktop.shortcuts.NativeLibraryExtractor;

import java.util.logging.Level;
import java.util.logging.Logger;


public class Listener {

    private static volatile boolean nativeHookAvailable = false;

    static {
        init();
    }

    public static void init() {
        // Extract native libraries BEFORE loading GlobalScreen class
        NativeLibraryExtractor.extractNativeLibraries();

        final Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        // Rejestracja natywnych hooków może się nie udać w środowiskach z niestandardowym classloaderem (np. zainstalowana app)
        try {
            GlobalScreen.registerNativeHook();
            nativeHookAvailable = true;
        } catch (Throwable e) {
            logger.log(Level.WARNING, "GlobalScreen native hook disabled: " + e.getMessage());
            nativeHookAvailable = false;
        }
    }

    public static boolean isNativeHookAvailable() {
        return nativeHookAvailable;
    }

}
