package com.patres.alina.server.scheduler;

import com.patres.alina.common.event.Event;

/**
 * Published when a scheduled task fires and needs to execute a prompt.
 * The UI layer subscribes to this event to handle execution mode logic.
 */
public final class SchedulerTaskTriggeredEvent extends Event {

    private final ScheduledTask task;

    public SchedulerTaskTriggeredEvent(ScheduledTask task) {
        this.task = task;
    }

    public ScheduledTask getTask() {
        return task;
    }
}
