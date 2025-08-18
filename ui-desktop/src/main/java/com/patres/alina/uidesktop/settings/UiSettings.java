package com.patres.alina.uidesktop.settings;

public record UiSettings(
        String theme,
        String language,
        ShortcutKeysSettings shortcutKeysSettings
) {

    private static final String DEFAULT_THEME = "Primer Light";
    private static final String DEFAULT_LANGUAGE = "en";

    public UiSettings() {
        this(DEFAULT_THEME, DEFAULT_LANGUAGE, new ShortcutKeysSettings());
    }

    public UiSettings(String theme,
                      String language,
                      ShortcutKeysSettings shortcutKeysSettings) {
        this.theme = theme == null ? DEFAULT_THEME : theme; // TODO add supported theme not just exist
        this.language = language == null ? DEFAULT_LANGUAGE : language; // TODO add supported language not just exist
        this.shortcutKeysSettings = shortcutKeysSettings == null ? new ShortcutKeysSettings() : shortcutKeysSettings;
    }

}