package com.patres.alina.uidesktop.shortcuts.key;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShortcutKeys {

    private final List<KeyboardKey> modeKeys;
    private final KeyboardKey executeKey;
    private final Set<KeyboardKey> allKeys;


    public ShortcutKeys() {
        this.modeKeys = new ArrayList<>();
        this.executeKey = null;
        this.allKeys = new HashSet<>();
    }

    public ShortcutKeys(List<KeyboardKey> modeKeys, KeyboardKey executeKey) {
        this.modeKeys = modeKeys;
        this.executeKey = executeKey;
        this.allKeys = Stream.concat(
                        modeKeys.stream(),
                        Stream.of(executeKey))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public List<KeyboardKey> getModeKeys() {
        return modeKeys;
    }

    public KeyboardKey getExecuteKey() {
        return executeKey;
    }

    public Set<KeyboardKey> getAllKeys() {
        return allKeys;
    }

}
