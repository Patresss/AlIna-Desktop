package com.patres.alina.server.event;

import com.patres.alina.common.event.Event;
import com.patres.alina.common.settings.AssistantSettings;

public final class AssistantSettingsUpdatedEvent extends Event {

    private final AssistantSettings settings;

    public AssistantSettingsUpdatedEvent(final AssistantSettings settings) {
        this.settings = settings;
    }

    public AssistantSettings getSettings() {
        return settings;
    }
}
