package com.patres.alina.uidesktop.settings;

import com.patres.alina.uidesktop.ui.util.NotificationSound;

public record UiSettings(
        String theme,
        String language,
        ShortcutKeysSettings shortcutKeysSettings,
        Boolean soundNotificationEnabled,
        String notificationSoundType,
        Boolean showExpandButton,
        Integer expandWidth,
        Boolean autoSplitOnExpand,
        Boolean mascotNotificationsEnabled
) {

    private static final String DEFAULT_THEME = "Calm Command Center";
    private static final String DEFAULT_LANGUAGE = "en";
    private static final boolean DEFAULT_SOUND_NOTIFICATION_ENABLED = true;
    private static final NotificationSound DEFAULT_NOTIFICATION_SOUND = NotificationSound.CHIME;
    private static final boolean DEFAULT_SHOW_EXPAND_BUTTON = true;
    private static final int DEFAULT_EXPAND_WIDTH = 1000;
    private static final boolean DEFAULT_AUTO_SPLIT_ON_EXPAND = false;
    private static final boolean DEFAULT_MASCOT_NOTIFICATIONS_ENABLED = true;

    public UiSettings() {
        this(DEFAULT_THEME, DEFAULT_LANGUAGE, new ShortcutKeysSettings(),
                DEFAULT_SOUND_NOTIFICATION_ENABLED, DEFAULT_NOTIFICATION_SOUND.name(),
                DEFAULT_SHOW_EXPAND_BUTTON, DEFAULT_EXPAND_WIDTH, DEFAULT_AUTO_SPLIT_ON_EXPAND,
                DEFAULT_MASCOT_NOTIFICATIONS_ENABLED);
    }

    public UiSettings(String theme,
                      String language,
                      ShortcutKeysSettings shortcutKeysSettings,
                      Boolean soundNotificationEnabled,
                      String notificationSoundType,
                      Boolean showExpandButton,
                      Integer expandWidth,
                      Boolean autoSplitOnExpand,
                      Boolean mascotNotificationsEnabled) {
        this.theme = theme == null ? DEFAULT_THEME : theme;
        this.language = language == null ? DEFAULT_LANGUAGE : language;
        this.shortcutKeysSettings = shortcutKeysSettings == null ? new ShortcutKeysSettings() : shortcutKeysSettings;
        this.soundNotificationEnabled = soundNotificationEnabled == null ? DEFAULT_SOUND_NOTIFICATION_ENABLED : soundNotificationEnabled;
        this.notificationSoundType = notificationSoundType;
        this.showExpandButton = showExpandButton == null ? DEFAULT_SHOW_EXPAND_BUTTON : showExpandButton;
        this.expandWidth = expandWidth == null ? DEFAULT_EXPAND_WIDTH : expandWidth;
        this.autoSplitOnExpand = autoSplitOnExpand == null ? DEFAULT_AUTO_SPLIT_ON_EXPAND : autoSplitOnExpand;
        this.mascotNotificationsEnabled = mascotNotificationsEnabled == null
                ? DEFAULT_MASCOT_NOTIFICATIONS_ENABLED
                : mascotNotificationsEnabled;
    }

    public boolean isSoundNotificationEnabled() {
        return soundNotificationEnabled != null ? soundNotificationEnabled : DEFAULT_SOUND_NOTIFICATION_ENABLED;
    }

    public boolean isShowExpandButton() {
        return showExpandButton != null ? showExpandButton : DEFAULT_SHOW_EXPAND_BUTTON;
    }

    public boolean isAutoSplitOnExpand() {
        return autoSplitOnExpand != null ? autoSplitOnExpand : DEFAULT_AUTO_SPLIT_ON_EXPAND;
    }

    public boolean isMascotNotificationsEnabled() {
        return mascotNotificationsEnabled != null
                ? mascotNotificationsEnabled
                : DEFAULT_MASCOT_NOTIFICATIONS_ENABLED;
    }

    public int resolveExpandWidth() {
        return expandWidth != null && expandWidth > 0 ? expandWidth : DEFAULT_EXPAND_WIDTH;
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
