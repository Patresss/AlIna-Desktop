package com.patres.alina.server.scheduler;

/**
 * Determines how a scheduled task's result is displayed in the UI.
 */
public enum ExecutionMode {

    /**
     * Send the prompt in the currently active tab.
     */
    CURRENT_TAB,

    /**
     * Create a new tab, activate it, and send the prompt there.
     */
    NEW_TAB,

    /**
     * Run the prompt in a background session without creating a visible tab.
     */
    BACKGROUND
}
