package com.patres.alina.uidesktop.shortcuts;

import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;

import java.util.Set;

public record ShortcutAction(Set<KeyboardKey> keys, Runnable action) {

    public ShortcutAction(KeyboardKey key, Runnable action) {
        this(Set.of(key), action);
    }

    public boolean isValid() {
        return action() != null
                && keys() != null
                && !keys().isEmpty();
    }
}
