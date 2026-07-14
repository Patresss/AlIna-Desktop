package com.patres.alina.common.event;

import com.patres.alina.common.interaction.AgentInteractionRequest;
import com.patres.alina.common.message.TodoItem;

import java.util.List;

public final class ChatMessageStreamEvent extends Event {

    public enum StreamEventType {
        TOKEN,
        REASONING,
        COMMENTARY,
        ACTIVITY,
        TODO_UPDATE,
        AGENT_INTERACTION,
        COMPLETE,
        CANCELLED,
        ERROR
    }

    public enum ActivityType {
        TOOL,
        MCP,
        SKILL
    }

    private final String threadId;
    private final String token;
    private final StreamEventType eventType;
    private final String errorMessage;
    private final String reasoningContent;
    private final String commentaryContent;
    private final ActivityType activityType;
    private final String activityName;
    private final String activityDetail;
    private final AgentInteractionRequest agentInteraction;
    private final List<TodoItem> todoItems;
    private final String modelUsed;
    private final String agentUsed;
    private final long tokensOutput;
    private final double cost;

    public ChatMessageStreamEvent(final String threadId, final String token) {
        this(threadId, token, StreamEventType.TOKEN, null, null, null, null, null, null, null, null, null, null, 0, 0.0);
    }

    public ChatMessageStreamEvent(final String threadId, final StreamEventType eventType) {
        this(threadId, null, eventType, null, null, null, null, null, null, null, null, null, null, 0, 0.0);
    }

    public ChatMessageStreamEvent(final String threadId,
                                  final StreamEventType eventType,
                                  final String errorMessage) {
        this(threadId, null, eventType, errorMessage, null, null, null, null, null, null, null, null, null, 0, 0.0);
    }

    public ChatMessageStreamEvent(final String threadId,
                                  final ActivityType activityType,
                                  final String activityName,
                                  final String activityDetail) {
        this(threadId, null, StreamEventType.ACTIVITY, null, null, null, activityType, activityName, activityDetail,
                null, null, null, null, 0, 0.0);
    }

    public ChatMessageStreamEvent(final String threadId,
                                  final String reasoningContent,
                                  final boolean reasoning) {
        this(threadId, null, StreamEventType.REASONING, null, reasoningContent, null, null, null, null,
                null, null, null, null, 0, 0.0);
    }

    public static ChatMessageStreamEvent commentary(final String threadId, final String commentaryContent) {
        return new ChatMessageStreamEvent(threadId, null, StreamEventType.COMMENTARY, null, null, commentaryContent,
                null, null, null, null, null, null, null, 0, 0.0);
    }

    public static ChatMessageStreamEvent interaction(final String threadId,
                                                     final AgentInteractionRequest interaction) {
        return new ChatMessageStreamEvent(threadId, null, StreamEventType.AGENT_INTERACTION, null, null, null,
                null, null, null, interaction, null, null, null, 0, 0.0);
    }

    public static ChatMessageStreamEvent complete(final String threadId,
                                                  final String modelUsed,
                                                  final String agentUsed,
                                                  final long tokensOutput,
                                                  final double cost) {
        return new ChatMessageStreamEvent(threadId, null, StreamEventType.COMPLETE, null, null, null,
                null, null, null, null, null, modelUsed, agentUsed, tokensOutput, cost);
    }

    public static ChatMessageStreamEvent todoUpdate(final String threadId, final List<TodoItem> todoItems) {
        return new ChatMessageStreamEvent(threadId, null, StreamEventType.TODO_UPDATE, null, null, null,
                null, null, null, null, todoItems == null ? List.of() : List.copyOf(todoItems),
                null, null, 0, 0.0);
    }

    private ChatMessageStreamEvent(final String threadId,
                                   final String token,
                                   final StreamEventType eventType,
                                   final String errorMessage,
                                   final String reasoningContent,
                                   final String commentaryContent,
                                   final ActivityType activityType,
                                   final String activityName,
                                   final String activityDetail,
                                   final AgentInteractionRequest agentInteraction,
                                   final List<TodoItem> todoItems,
                                   final String modelUsed,
                                   final String agentUsed,
                                   final long tokensOutput,
                                   final double cost) {
        this.threadId = threadId;
        this.token = token;
        this.eventType = eventType;
        this.errorMessage = errorMessage;
        this.reasoningContent = reasoningContent;
        this.commentaryContent = commentaryContent;
        this.activityType = activityType;
        this.activityName = activityName;
        this.activityDetail = activityDetail;
        this.agentInteraction = agentInteraction;
        this.todoItems = todoItems;
        this.modelUsed = modelUsed;
        this.agentUsed = agentUsed;
        this.tokensOutput = tokensOutput;
        this.cost = cost;
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

    public String getReasoningContent() {
        return reasoningContent;
    }

    public String getCommentaryContent() {
        return commentaryContent;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public String getActivityName() {
        return activityName;
    }

    public String getActivityDetail() {
        return activityDetail;
    }

    public AgentInteractionRequest getAgentInteraction() {
        return agentInteraction;
    }

    public List<TodoItem> getTodoItems() {
        return todoItems;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public String getAgentUsed() {
        return agentUsed;
    }

    public long getTokensTotal() {
        return tokensOutput;
    }

    public double getCost() {
        return cost;
    }
}
