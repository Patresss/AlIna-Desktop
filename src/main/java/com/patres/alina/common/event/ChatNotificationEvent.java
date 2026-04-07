package com.patres.alina.common.event;

import com.patres.alina.common.message.ChatMessageStyleType;

/**
 * Generic notification event displayed in the chat window.
 * Can be published by any widget (calendar, Jira, GitHub, etc.)
 * to show an informational message in the chat.
 */
public final class ChatNotificationEvent extends Event {

    private final String message;
    private final ChatMessageStyleType styleType;

    public ChatNotificationEvent(final String message) {
        this(message, ChatMessageStyleType.INFO);
    }

    public ChatNotificationEvent(final String message, final ChatMessageStyleType styleType) {
        this.message = message;
        this.styleType = styleType;
    }

    public String getMessage() {
        return message;
    }

    public ChatMessageStyleType getStyleType() {
        return styleType;
    }
}
