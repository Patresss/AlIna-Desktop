package com.patres.alina.uidesktop.ui.util;

import com.patres.alina.uidesktop.ui.language.LanguageManager;

/**
 * Available notification sound variants.
 * Each variant defines how the sound is synthesized programmatically.
 */
public enum NotificationSound {

    CHIME("settings.sound.type.chime"),
    BUBBLE("settings.sound.type.bubble"),
    DING("settings.sound.type.ding"),
    SOFT_BELL("settings.sound.type.softBell"),
    DROPLET("settings.sound.type.droplet"),
    SPARKLE("settings.sound.type.sparkle"),
    WARM_POP("settings.sound.type.warmPop"),
    GENTLE_RISE("settings.sound.type.gentleRise");

    private final String labelKey;

    NotificationSound(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getLabelKey() {
        return labelKey;
    }

    @Override
    public String toString() {
        return LanguageManager.getLanguageString(labelKey);
    }
}
