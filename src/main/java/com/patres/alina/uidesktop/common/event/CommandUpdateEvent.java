package com.patres.alina.uidesktop.common.event;

import com.patres.alina.common.event.Event;

public final class CommandUpdateEvent extends Event {

    public enum EventType {
        COMMAND_ADDED,
        COMMAND_UPDATED,
        COMMAND_DELETED,
    }

    public CommandUpdateEvent(EventType eventType) {
        this.eventType = eventType;
    }

    private final EventType eventType;

}