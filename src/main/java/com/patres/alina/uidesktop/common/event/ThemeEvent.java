/* SPDX-License-Identifier: MIT */

package com.patres.alina.uidesktop.common.event;

import com.patres.alina.common.event.Event;

public final class ThemeEvent extends Event {

    public enum EventType {
        // theme can change both, base font size and colors
        THEME_CHANGE
        // font size or family only change
    }

    private final EventType eventType;

    public ThemeEvent(EventType eventType) {
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

    @Override
    public String toString() {
        return "ThemeEvent{"
            + "eventType=" + eventType
            + "} " + super.toString();
    }
}
