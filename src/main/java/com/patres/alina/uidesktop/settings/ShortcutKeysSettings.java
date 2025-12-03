package com.patres.alina.uidesktop.settings;

import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;

import java.util.Arrays;

public record ShortcutKeysSettings(
        ShortcutKeys speechShortcutKeys,
        ShortcutKeys focusShortcutKeys,
        ShortcutKeys contextMenuShortcutKeys
) {

    public ShortcutKeysSettings() {
        this(new ShortcutKeys(), new ShortcutKeys(), getDefaultContextMenuShortcut());
    }

    public ShortcutKeysSettings(ShortcutKeys speechShortcutKeys, ShortcutKeys focusShortcutKeys, ShortcutKeys contextMenuShortcutKeys) {
        this.speechShortcutKeys = speechShortcutKeys == null ? new ShortcutKeys() : speechShortcutKeys;
        this.focusShortcutKeys = focusShortcutKeys == null ? new ShortcutKeys() : focusShortcutKeys;
        this.contextMenuShortcutKeys = contextMenuShortcutKeys == null ? getDefaultContextMenuShortcut() : contextMenuShortcutKeys;
    }

    private static ShortcutKeys getDefaultContextMenuShortcut() {
        return new ShortcutKeys(Arrays.asList(KeyboardKey.CONTROL, null, null), KeyboardKey.Q); // TODO
    }

}