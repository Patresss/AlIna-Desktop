package com.patres.alina.common.event;

import com.patres.alina.common.event.Event;
import com.patres.alina.common.message.TodoItem;

import java.util.List;

public final class ChatMessageStreamEvent extends Event {

    public enum StreamEventType {
        TOKEN,      // A new token has arrived
        REASONING,  // A thinking/reasoning update arrived
        COMMENTARY, // A non-final commentary update arrived
        ACTIVITY,   // A tool or MCP activity is in progress
        TODO_UPDATE,// The AI updated its todo list (via TodoWrite tool)
        PERMISSION_REQUEST, // A tool or bash command requires user approval
        COMPLETE,   // The stream is complete
        CANCELLED,  // The stream was cancelled by the user
        ERROR       // An error occurred
    }

    public enum ActivityType {
        TOOL,
        MCP,
        SKILL
    }

    public enum PermissionType {
        TOOL,
        MCP,
        BASH
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
    private final PermissionType permissionType;
    private final String permissionRequestId;
    private final String permissionValue;
    private final String permissionTitle;
    private final String permissionMessage;
    private final String permissionConfigPath;
    private final String permissionMatchedRule;
    private final List<TodoItem> todoItems;
    private final String modelUsed;
    private final String agentUsed;
    private final long tokensOutput;
    private final double cost;

    public ChatMessageStreamEvent(String threadId, String token) {
        this.threadId = threadId;
        this.token = token;
        this.eventType = StreamEventType.TOKEN;
        this.errorMessage = null;
        this.reasoningContent = null;
        this.commentaryContent = null;
        this.activityType = null;
        this.activityName = null;
        this.activityDetail = null;
        this.permissionType = null;
        this.permissionRequestId = null;
        this.permissionValue = null;
        this.permissionTitle = null;
        this.permissionMessage = null;
        this.permissionConfigPath = null;
        this.permissionMatchedRule = null;
        this.todoItems = null;
        this.modelUsed = null;
        this.agentUsed = null;
        this.tokensOutput = 0;
        this.cost = 0.0;
    }

    public ChatMessageStreamEvent(String threadId, StreamEventType eventType) {
        this.threadId = threadId;
        this.token = null;
        this.eventType = eventType;
        this.errorMessage = null;
        this.reasoningContent = null;
        this.commentaryContent = null;
        this.activityType = null;
        this.activityName = null;
        this.activityDetail = null;
        this.permissionType = null;
        this.permissionRequestId = null;
        this.permissionValue = null;
        this.permissionTitle = null;
        this.permissionMessage = null;
        this.permissionConfigPath = null;
        this.permissionMatchedRule = null;
        this.todoItems = null;
        this.modelUsed = null;
        this.agentUsed = null;
        this.tokensOutput = 0;
        this.cost = 0.0;
    }

    public ChatMessageStreamEvent(String threadId, StreamEventType eventType, String errorMessage) {
        this.threadId = threadId;
        this.token = null;
        this.eventType = eventType;
        this.errorMessage = errorMessage;
        this.reasoningContent = null;
        this.commentaryContent = null;
        this.activityType = null;
        this.activityName = null;
        this.activityDetail = null;
        this.permissionType = null;
        this.permissionRequestId = null;
        this.permissionValue = null;
        this.permissionTitle = null;
        this.permissionMessage = null;
        this.permissionConfigPath = null;
        this.permissionMatchedRule = null;
        this.todoItems = null;
        this.modelUsed = null;
        this.agentUsed = null;
        this.tokensOutput = 0;
        this.cost = 0.0;
    }

    public ChatMessageStreamEvent(String threadId,
                                  ActivityType activityType,
                                  String activityName,
                                  String activityDetail) {
        this.threadId = threadId;
        this.token = null;
        this.eventType = StreamEventType.ACTIVITY;
        this.errorMessage = null;
        this.reasoningContent = null;
        this.commentaryContent = null;
        this.activityType = activityType;
        this.activityName = activityName;
        this.activityDetail = activityDetail;
        this.permissionType = null;
        this.permissionRequestId = null;
        this.permissionValue = null;
        this.permissionTitle = null;
        this.permissionMessage = null;
        this.permissionConfigPath = null;
        this.permissionMatchedRule = null;
        this.todoItems = null;
        this.modelUsed = null;
        this.agentUsed = null;
        this.tokensOutput = 0;
        this.cost = 0.0;
    }

    public ChatMessageStreamEvent(String threadId,
                                  PermissionType permissionType,
                                  String permissionRequestId,
                                  String permissionValue,
                                  String permissionTitle,
                                  String permissionMessage,
                                  String permissionConfigPath,
                                  String permissionMatchedRule) {
        this.threadId = threadId;
        this.token = null;
        this.eventType = StreamEventType.PERMISSION_REQUEST;
        this.errorMessage = null;
        this.reasoningContent = null;
        this.commentaryContent = null;
        this.activityType = null;
        this.activityName = null;
        this.activityDetail = null;
        this.permissionType = permissionType;
        this.permissionRequestId = permissionRequestId;
        this.permissionValue = permissionValue;
        this.permissionTitle = permissionTitle;
        this.permissionMessage = permissionMessage;
        this.permissionConfigPath = permissionConfigPath;
        this.permissionMatchedRule = permissionMatchedRule;
        this.todoItems = null;
        this.modelUsed = null;
        this.agentUsed = null;
        this.tokensOutput = 0;
        this.cost = 0.0;
    }

    public ChatMessageStreamEvent(final String threadId, final String reasoningContent, final boolean reasoning) {
        this.threadId = threadId;
        this.token = null;
        this.eventType = StreamEventType.REASONING;
        this.errorMessage = null;
        this.reasoningContent = reasoningContent;
        this.commentaryContent = null;
        this.activityType = null;
        this.activityName = null;
        this.activityDetail = null;
        this.permissionType = null;
        this.permissionRequestId = null;
        this.permissionValue = null;
        this.permissionTitle = null;
        this.permissionMessage = null;
        this.permissionConfigPath = null;
        this.permissionMatchedRule = null;
        this.todoItems = null;
        this.modelUsed = null;
        this.agentUsed = null;
        this.tokensOutput = 0;
        this.cost = 0.0;
    }

    public static ChatMessageStreamEvent commentary(final String threadId, final String commentaryContent) {
        return new ChatMessageStreamEvent(threadId, commentaryContent, StreamEventType.COMMENTARY);
    }

    public static ChatMessageStreamEvent complete(final String threadId,
                                                     final String modelUsed,
                                                     final String agentUsed,
                                                     final long tokensOutput,
                                                     final double cost) {
        return new ChatMessageStreamEvent(threadId, StreamEventType.COMPLETE, modelUsed, agentUsed, tokensOutput, cost);
    }

    public static ChatMessageStreamEvent todoUpdate(final String threadId, final List<TodoItem> todoItems) {
        return new ChatMessageStreamEvent(threadId, todoItems);
    }

    private ChatMessageStreamEvent(final String threadId, final List<TodoItem> todoItems) {
        this.threadId = threadId;
        this.token = null;
        this.eventType = StreamEventType.TODO_UPDATE;
        this.errorMessage = null;
        this.reasoningContent = null;
        this.commentaryContent = null;
        this.activityType = null;
        this.activityName = null;
        this.activityDetail = null;
        this.permissionType = null;
        this.permissionRequestId = null;
        this.permissionValue = null;
        this.permissionTitle = null;
        this.permissionMessage = null;
        this.permissionConfigPath = null;
        this.permissionMatchedRule = null;
        this.todoItems = todoItems == null ? List.of() : List.copyOf(todoItems);
        this.modelUsed = null;
        this.agentUsed = null;
        this.tokensOutput = 0;
        this.cost = 0.0;
    }

    private ChatMessageStreamEvent(final String threadId,
                                   final String commentaryContent,
                                   final StreamEventType commentaryType) {
        this.threadId = threadId;
        this.token = null;
        this.eventType = commentaryType;
        this.errorMessage = null;
        this.reasoningContent = null;
        this.commentaryContent = commentaryContent;
        this.activityType = null;
        this.activityName = null;
        this.activityDetail = null;
        this.permissionType = null;
        this.permissionRequestId = null;
        this.permissionValue = null;
        this.permissionTitle = null;
        this.permissionMessage = null;
        this.permissionConfigPath = null;
        this.permissionMatchedRule = null;
        this.todoItems = null;
        this.modelUsed = null;
        this.agentUsed = null;
        this.tokensOutput = 0;
        this.cost = 0.0;
    }

    private ChatMessageStreamEvent(final String threadId,
                                   final StreamEventType eventType,
                                   final String modelUsed,
                                   final String agentUsed,
                                   final long tokensOutput,
                                   final double cost) {
        this.threadId = threadId;
        this.token = null;
        this.eventType = eventType;
        this.errorMessage = null;
        this.reasoningContent = null;
        this.commentaryContent = null;
        this.activityType = null;
        this.activityName = null;
        this.activityDetail = null;
        this.permissionType = null;
        this.permissionRequestId = null;
        this.permissionValue = null;
        this.permissionTitle = null;
        this.permissionMessage = null;
        this.permissionConfigPath = null;
        this.permissionMatchedRule = null;
        this.todoItems = null;
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

    public PermissionType getPermissionType() {
        return permissionType;
    }

    public String getPermissionRequestId() {
        return permissionRequestId;
    }

    public String getPermissionValue() {
        return permissionValue;
    }

    public String getPermissionTitle() {
        return permissionTitle;
    }

    public String getPermissionMessage() {
        return permissionMessage;
    }

    public String getPermissionConfigPath() {
        return permissionConfigPath;
    }

    public String getPermissionMatchedRule() {
        return permissionMatchedRule;
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
