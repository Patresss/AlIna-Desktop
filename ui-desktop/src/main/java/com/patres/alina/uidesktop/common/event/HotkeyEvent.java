/* SPDX-License-Identifier: MIT */

package com.patres.alina.uidesktop.common.event;

import com.patres.alina.common.event.Event;
import javafx.scene.input.KeyCodeCombination;

public final class HotkeyEvent extends Event {

    private final KeyCodeCombination keys;

    public HotkeyEvent(KeyCodeCombination keys) {
        this.keys = keys;
    }

    public KeyCodeCombination getKeys() {
        return keys;
    }

    @Override
    public String toString() {
        return "HotkeyEvent{"
            + "keys=" + keys
            + "} " + super.toString();
    }
}
