package com.patres.alina.uidesktop.ui.util;

public final class OsUtils {

    private static final boolean IS_MAC = detectMac();

    private OsUtils() {
    }

    public static boolean isMacOS() {
        return IS_MAC;
    }

    private static boolean detectMac() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }
}
