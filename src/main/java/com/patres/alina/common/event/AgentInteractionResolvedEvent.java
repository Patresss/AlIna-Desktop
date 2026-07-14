package com.patres.alina.common.event;

public final class AgentInteractionResolvedEvent extends Event {

    private final String threadId;
    private final String requestId;

    public AgentInteractionResolvedEvent(final String threadId, final String requestId) {
        this.threadId = threadId;
        this.requestId = requestId;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getRequestId() {
        return requestId;
    }
}
