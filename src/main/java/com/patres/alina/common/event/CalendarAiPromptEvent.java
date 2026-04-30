package com.patres.alina.common.event;

/**
 * Published by the calendar widget when the user clicks the AI button
 * on a calendar event. The message contains the fully resolved prompt
 * (with event details substituted for $ARGUMENTS) and should be sent
 * to the chat as a user message.
 */
public final class CalendarAiPromptEvent extends Event {

    private final String message;

    public CalendarAiPromptEvent(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
