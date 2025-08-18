package com.patres.alina.uidesktop.util;

public final class StringUtils {

    public static String truncateStringToDisplay(final String str, final int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 1) + "…";
    }

}
