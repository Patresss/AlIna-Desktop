package com.patres.alina.uidesktop.common.event;

import com.patres.alina.common.event.Event;

public final class IntegrationUpdateEvent extends Event {

    public enum EventType {
        INTEGRATION_ADDED,
        INTEGRATION_UPDATED,
        INTEGRATION_DELETED,
    }

    public IntegrationUpdateEvent(EventType eventType) {
        this.eventType = eventType;
    }

    private final EventType eventType;

}
