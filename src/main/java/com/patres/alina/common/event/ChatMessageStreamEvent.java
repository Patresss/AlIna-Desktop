package com.patres.alina.common.event;

import com.patres.alina.common.event.Event;

public final class ChatMessageStreamEvent extends Event {

    public enum StreamEventType {
        TOKEN,      // A new token has arrived
        COMPLETE,   // The stream is complete
        ERROR       // An error occurred
    }

    private final String threadId;
    private final String token;
    private final StreamEventType eventType;
    private final String errorMessage;

    public ChatMessageStreamEvent(String threadId, String token) {
        this.threadId = threadId;
        this.token = token;
        this.eventType = StreamEventType.TOKEN;
        this.errorMessage = null;
    }

    public ChatMessageStreamEvent(String threadId, StreamEventType eventType) {
        this.threadId = threadId;
        this.token = null;
        this.eventType = eventType;
        this.errorMessage = null;
    }

    public ChatMessageStreamEvent(String threadId, StreamEventType eventType, String errorMessage) {
        this.threadId = threadId;
        this.token = null;
        this.eventType = eventType;
        this.errorMessage = errorMessage;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getToken() {
        return token;
    }

    public StreamEventType getEventType() {
        return eventType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}