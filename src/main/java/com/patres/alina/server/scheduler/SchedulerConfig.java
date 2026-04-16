package com.patres.alina.server.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Root config object persisted at ~/.config/AlIna/scheduler/tasks.json
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SchedulerConfig(
        List<ScheduledTask> tasks
) {

    public static SchedulerConfig empty() {
        return new SchedulerConfig(List.of());
    }
}
