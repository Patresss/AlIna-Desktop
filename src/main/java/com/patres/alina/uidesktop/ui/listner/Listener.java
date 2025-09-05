package com.patres.alina.uidesktop.ui.listner;


import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;

import java.util.logging.Level;
import java.util.logging.Logger;


public class Listener {

    static {
        init();
    }

    public static void init() {
        final Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.WARNING);
        logger.setUseParentHandlers(false);

        // Rejestracja natywnych hooków może się nie udać w środowiskach z niestandardowym classloaderem (np. zainstalowana app)
        try {
            GlobalScreen.registerNativeHook();
        } catch (Throwable e) {
            logger.log(Level.WARNING, "GlobalScreen native hook disabled: " + e.getMessage());
        }
    }

}
