package com.patres.alina.common.message;

/**
 * Represents a single todo item from OpenCode's TodoWrite tool.
 *
 * @param content  brief description of the task
 * @param status   current status: pending, in_progress, completed, cancelled
 * @param priority priority level: high, medium, low
 */
public record TodoItem(
        String content,
        String status,
        String priority
) {

    public boolean isPending() {
        return "pending".equalsIgnoreCase(status);
    }

    public boolean isInProgress() {
        return "in_progress".equalsIgnoreCase(status);
    }

    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "cancelled".equalsIgnoreCase(status);
    }
}
