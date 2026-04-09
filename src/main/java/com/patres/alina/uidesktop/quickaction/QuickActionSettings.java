package com.patres.alina.uidesktop.quickaction;

import java.util.Map;
import java.util.HashMap;

public record QuickActionSettings(
        Map<String, Boolean> enabledActions
) {

    public QuickActionSettings() {
        this(createDefaults());
    }

    public QuickActionSettings(Map<String, Boolean> enabledActions) {
        this.enabledActions = enabledActions == null ? createDefaults() : new HashMap<>(enabledActions);
    }

    public boolean isEnabled(QuickActionType type) {
        return enabledActions.getOrDefault(type.getId(), true);
    }

    public QuickActionSettings withEnabled(QuickActionType type, boolean enabled) {
        var copy = new HashMap<>(enabledActions);
        copy.put(type.getId(), enabled);
        return new QuickActionSettings(copy);
    }

    private static Map<String, Boolean> createDefaults() {
        var defaults = new HashMap<String, Boolean>();
        for (QuickActionType type : QuickActionType.values()) {
            defaults.put(type.getId(), true);
        }
        return defaults;
    }
}
