package com.patres.alina.uidesktop.settings;

import com.patres.alina.uidesktop.ui.util.NotificationSound;

public record UiSettings(
        String theme,
        String language,
        ShortcutKeysSettings shortcutKeysSettings,
        Boolean soundNotificationEnabled,
        String notificationSoundType
) {

    private static final String DEFAULT_THEME = "Primer Light";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final boolean DEFAULT_SOUND_NOTIFICATION_ENABLED = true;
    private static final NotificationSound DEFAULT_NOTIFICATION_SOUND = NotificationSound.CHIME;

    public UiSettings() {
        this(DEFAULT_THEME, DEFAULT_LANGUAGE, new ShortcutKeysSettings(), DEFAULT_SOUND_NOTIFICATION_ENABLED, DEFAULT_NOTIFICATION_SOUND.name());
    }

    public UiSettings(String theme,
                      String language,
                      ShortcutKeysSettings shortcutKeysSettings,
                      Boolean soundNotificationEnabled,
                      String notificationSoundType) {
        this.theme = theme == null ? DEFAULT_THEME : theme; // TODO add supported theme not just exist
        this.language = language == null ? DEFAULT_LANGUAGE : language; // TODO add supported language not just exist
        this.shortcutKeysSettings = shortcutKeysSettings == null ? new ShortcutKeysSettings() : shortcutKeysSettings;
        this.soundNotificationEnabled = soundNotificationEnabled == null ? DEFAULT_SOUND_NOTIFICATION_ENABLED : soundNotificationEnabled;
        this.notificationSoundType = notificationSoundType;
    }

    public boolean isSoundNotificationEnabled() {
        return soundNotificationEnabled != null ? soundNotificationEnabled : DEFAULT_SOUND_NOTIFICATION_ENABLED;
    }

    public NotificationSound resolveNotificationSound() {
        if (notificationSoundType == null || notificationSoundType.isBlank()) {
            return DEFAULT_NOTIFICATION_SOUND;
        }
        try {
            return NotificationSound.valueOf(notificationSoundType);
        } catch (IllegalArgumentException e) {
            return DEFAULT_NOTIFICATION_SOUND;
        }
    }

}