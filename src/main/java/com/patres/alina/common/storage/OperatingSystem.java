package com.patres.alina.common.storage;

import java.util.Locale;
import java.util.function.Predicate;

public enum OperatingSystem {
    MAC(name -> containsAny(name, "mac")),
    WINDOWS(name -> containsAny(name, "win")),
    LINUX(name -> containsAny(name, "nux", "nix", "aix", "linux")),
    OTHER(name -> true);

    private final Predicate<String> matcher;

    OperatingSystem(final Predicate<String> matcher) {
        this.matcher = matcher;
    }

    public static OperatingSystem detect(final String osName) {
        final String normalized = (osName == null ? "" : osName).toLowerCase(Locale.ROOT);
        for (final OperatingSystem os : values()) {
            if (os.matcher.test(normalized)) {
                return os;
            }
        }
        return OTHER;
    }

    private static boolean containsAny(final String haystack, final String... needles) {
        if (haystack == null) return false;
        for (final String n : needles) {
            if (haystack.contains(n)) return true;
        }
        return false;
    }
}

