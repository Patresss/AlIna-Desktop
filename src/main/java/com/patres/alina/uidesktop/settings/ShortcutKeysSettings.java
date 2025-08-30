package com.patres.alina.uidesktop.settings;

import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;

public record ShortcutKeysSettings(
        ShortcutKeys speechShortcutKeys,
        ShortcutKeys focusShortcutKeys
) {

    public ShortcutKeysSettings() {
        this(new ShortcutKeys(), new ShortcutKeys());
    }

    public ShortcutKeysSettings(ShortcutKeys speechShortcutKeys, ShortcutKeys focusShortcutKeys) {
        this.speechShortcutKeys = speechShortcutKeys == null ? new ShortcutKeys() : speechShortcutKeys;
        this.focusShortcutKeys = focusShortcutKeys == null ? new ShortcutKeys() : focusShortcutKeys;
    }

}