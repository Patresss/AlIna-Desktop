package com.patres.alina.server.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

/**
 * A single scheduled task that triggers a prompt on a cron schedule.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScheduledTask(
        String id,
        String name,
        String cron,
        String prompt,
        boolean enabled,
        ExecutionMode executionMode,
        String model,
        LocalDateTime lastRun,
        LocalDateTime nextRun
) {

    public ScheduledTask withLastRun(LocalDateTime lastRun) {
        return new ScheduledTask(id, name, cron, prompt, enabled, executionMode, model, lastRun, nextRun);
    }

    public ScheduledTask withNextRun(LocalDateTime nextRun) {
        return new ScheduledTask(id, name, cron, prompt, enabled, executionMode, model, lastRun, nextRun);
    }

    public ScheduledTask withEnabled(boolean enabled) {
        return new ScheduledTask(id, name, cron, prompt, enabled, executionMode, model, lastRun, nextRun);
    }

    /**
     * Creates a new task with a generated UUID id and sensible defaults.
     */
    public static ScheduledTask createNew(String name, String cron, String prompt, ExecutionMode executionMode, String model) {
        return new ScheduledTask(
                java.util.UUID.randomUUID().toString(),
                name,
                cron,
                prompt,
                true,
                executionMode,
                model,
                null,
                null
        );
    }
}
