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

        // Zacoemnotwane bo nie ma polskich znaków w TextArea
        try {
            GlobalScreen.registerNativeHook();
        } catch (NativeHookException e) {
            throw new RuntimeException(e);
        }
    }

}
