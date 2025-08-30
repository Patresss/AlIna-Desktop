package com.patres.alina.uidesktop.common.event;

import com.patres.alina.common.event.Event;

public final class PluginUpdateEvent extends Event {

    public enum EventType {
        PLUGIN_ADDED,
        PLUGIN_UPDATED,
        PLUGIN_DELETED,
    }

    public PluginUpdateEvent(EventType eventType) {
        this.eventType = eventType;
    }

    private final EventType eventType;

}
