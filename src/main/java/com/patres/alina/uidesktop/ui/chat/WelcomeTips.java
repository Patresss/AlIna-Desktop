package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.uidesktop.ui.language.LanguageManager;

import java.util.Random;

/**
 * Provides a random localized "tip of the day" for the welcome screen.
 */
public final class WelcomeTips {

    private static final int TIP_COUNT = 17;
    private static final Random RANDOM = new Random();

    private WelcomeTips() {}

    public static String getRandom() {
        final int index = RANDOM.nextInt(TIP_COUNT) + 1;
        return LanguageManager.getLanguageString("welcome.tip." + index);
    }
}
