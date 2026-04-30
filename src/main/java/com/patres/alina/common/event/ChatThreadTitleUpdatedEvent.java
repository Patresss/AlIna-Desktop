package com.patres.alina.common.event;

public class ChatThreadTitleUpdatedEvent extends Event {

    private final String threadId;
    private final String newTitle;

    public ChatThreadTitleUpdatedEvent(final String threadId, final String newTitle) {
        this.threadId = threadId;
        this.newTitle = newTitle;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getNewTitle() {
        return newTitle;
    }
}
