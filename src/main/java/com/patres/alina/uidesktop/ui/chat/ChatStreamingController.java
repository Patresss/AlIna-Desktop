package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.AgentInteractionResolvedEvent;
import com.patres.alina.common.interaction.AgentInteractionResponse;
import com.patres.alina.common.interaction.AgentInteractionResolutionModel;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.message.TodoItem;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.util.FxThreadRunner;
import com.patres.alina.uidesktop.ui.util.NotificationSoundPlayer;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.patres.alina.common.message.ChatMessageRole.ASSISTANT;

public class ChatStreamingController {

    private static final Logger logger = LoggerFactory.getLogger(ChatStreamingController.class);
    private static final String ICON_STOP = "fth-square";
    private static final String ICON_REGENERATE = "fth-refresh-cw";
    private static final String CHAT_INPUT_STATUS_STYLE_CLASS = "chat-text-area-status";

    private final Browser browser;
    private final Button streamControlButton;
    private final List<Node> actionNodes;
    private final TextArea chatTextArea;
    private final ChatStatusPrompt statusPrompt;
    private final String chatThreadId;

    private volatile boolean streamingStarted;
    private volatile boolean ignoreIncomingTokens;
    private volatile StreamControlMode streamControlMode = StreamControlMode.REGENERATE;
    private volatile boolean regenerating;
    private volatile boolean replaceExistingAssistantMessageOnStart;
    private volatile boolean hasAnyUserMessages;
    private volatile Instant streamingStartedAt = Instant.EPOCH;
    private volatile String latestReasoningContent = "";
    private volatile String latestCommentaryContent = "";
    private volatile boolean backgroundMode;
    private final List<String> activityLabels = new ArrayList<>();

    private enum StreamControlMode {
        STOP,
        REGENERATE
    }

    public ChatStreamingController(Browser browser,
                                   Button streamControlButton,
                                   List<Node> actionNodes,
                                   TextArea chatTextArea,
                                   ChatStatusPrompt statusPrompt,
                                   String chatThreadId,
                                   boolean hasAnyUserMessages) {
        this.browser = browser;
        this.streamControlButton = streamControlButton;
        this.actionNodes = actionNodes;
        this.chatTextArea = chatTextArea;
        this.statusPrompt = statusPrompt;
        this.chatThreadId = chatThreadId;
        this.hasAnyUserMessages = hasAnyUserMessages;
    }

    public void initialize() {
        FxThreadRunner.run(() -> {
            setStreamControlMode(StreamControlMode.REGENERATE);
        });
    }

    public void markUserMessageSent() {
        FxThreadRunner.run(() -> {
            hasAnyUserMessages = true;
            if (streamControlMode == StreamControlMode.REGENERATE) {
                setStreamControlMode(StreamControlMode.REGENERATE);
            }
        });
    }

    public void beginStreaming(boolean isRegeneration) {
        beginStreaming(isRegeneration, false);
    }

    public void beginStreaming(boolean isRegeneration, boolean backgroundMode) {
        FxThreadRunner.runAndWait(() -> {
            this.backgroundMode = backgroundMode;
            beginStreamingUiState(isRegeneration);
            browser.showLoader();
        });
    }

    public void handleStartError() {
        FxThreadRunner.run(() -> {
            browser.hideLoader();
            final boolean wasBackground = backgroundMode;
            endStreamingUiState();
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.stream.error"));
            if (!wasBackground) {
                chatTextArea.clear();
                setChatInputReady();
                chatTextArea.requestFocus();
            }
        });
    }

    public void streamControlFromUi() {
        if (streamControlMode == StreamControlMode.STOP) {
            stopStreaming();
            return;
        }
        regenerateLastResponse();
    }

    public void handleStreamEvent(ChatMessageStreamEvent event) {
        if (!event.getThreadId().equals(chatThreadId)) {
            return;
        }

        switch (event.getEventType()) {
            case TOKEN -> handleTokenEvent(event);
            case REASONING -> handleReasoningEvent(event);
            case COMMENTARY -> handleCommentaryEvent(event);
            case ACTIVITY -> handleActivityEvent(event);
            case TODO_UPDATE -> handleTodoUpdateEvent(event);
            case AGENT_INTERACTION -> handleAgentInteractionEvent(event);
            case COMPLETE -> handleCompleteEvent(event);
            case CANCELLED -> handleCancelledEvent();
            case ERROR -> handleErrorEvent(event);
        }
    }

    public void handleAgentInteractionResolved(final AgentInteractionResolvedEvent event) {
        if (!event.getThreadId().equals(chatThreadId)) {
            return;
        }
        FxThreadRunner.run(() -> {
            browser.resolveAgentInteraction(
                    event.getRequestId(),
                    LanguageManager.getLanguageString("chat.interaction.resolvedExternally")
            );
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.stream.connecting"));
        });
    }

    private void handleCommentaryEvent(final ChatMessageStreamEvent event) {
        final String content = event.getCommentaryContent();
        if (content == null || content.isBlank()) {
            return;
        }
        final boolean startsNewCommentaryMessage = latestCommentaryContent != null
                && !latestCommentaryContent.isBlank()
                && !content.equals(latestCommentaryContent)
                && !content.startsWith(latestCommentaryContent);
        latestCommentaryContent = content;
        FxThreadRunner.run(() -> {
            browser.finalizeAssistantActivity();
            browser.hideLoader();
            if (startsNewCommentaryMessage) {
                browser.finalizeAssistantCommentary();
            }
            browser.showAssistantCommentary(LanguageManager.getLanguageString("chat.commentary.title"), content);
            updateComposerProcessStatus();
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.commentary.status"));
        });
    }

    private void handleReasoningEvent(final ChatMessageStreamEvent event) {
        final String content = event.getReasoningContent();
        if (content == null || content.isBlank()) {
            return;
        }
        final boolean startsNewReasoningMessage = latestReasoningContent != null
                && !latestReasoningContent.isBlank()
                && !content.equals(latestReasoningContent)
                && !content.startsWith(latestReasoningContent);
        latestReasoningContent = content;
        FxThreadRunner.run(() -> {
            browser.finalizeAssistantActivity();
            browser.hideLoader();
            if (startsNewReasoningMessage) {
                browser.finalizeAssistantReasoning();
            }
            browser.showAssistantReasoning(LanguageManager.getLanguageString("chat.reasoning.title"), content);
            updateComposerProcessStatus();
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.reasoning.status"));
        });
    }

    private void handleTokenEvent(ChatMessageStreamEvent event) {
        if (ignoreIncomingTokens) {
            return;
        }
        if (!streamingStarted) {
            streamingStarted = true;
            FxThreadRunner.run(() -> {
                browser.finalizeAssistantActivity();
                browser.finalizeAssistantReasoning();
                browser.finalizeAssistantCommentary();
                browser.hideLoader();
                browser.startStreamingAssistantMessage(replaceExistingAssistantMessageOnStart);
                replaceExistingAssistantMessageOnStart = false;
                statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.stream.streaming"));
            });
        }
        FxThreadRunner.run(() -> browser.appendToStreamingMessage(event.getToken()));
    }

    private void handleActivityEvent(final ChatMessageStreamEvent event) {
        final String label = formatActivityLabel(event);
        final String detail = event.getActivityDetail() == null ? "" : event.getActivityDetail();
        synchronized (activityLabels) {
            activityLabels.add(label);
        }
        FxThreadRunner.run(() -> {
            if (streamingStarted) {
                browser.finishStreamingMessage();
                streamingStarted = false;
            }
            browser.finalizeAssistantReasoning();
            browser.finalizeAssistantCommentary();
            browser.showAssistantActivity(label, detail);
            browser.showLoader();
            updateComposerProcessStatus();
            statusPrompt.showStatusPrompt(label);
        });
    }

    private void handleTodoUpdateEvent(final ChatMessageStreamEvent event) {
        final List<TodoItem> items = event.getTodoItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        FxThreadRunner.run(() -> {
            browser.showTodoList(items, LanguageManager.getLanguageString("chat.todo.title"));
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.todo.status"));
        });
    }

    private void handleAgentInteractionEvent(final ChatMessageStreamEvent event) {
        final var interaction = event.getAgentInteraction();
        if (interaction == null) {
            return;
        }
        FxThreadRunner.run(() -> {
            if (streamingStarted) {
                browser.finishStreamingMessage();
                streamingStarted = false;
            }
            browser.finalizeAssistantActivity();
            browser.finalizeAssistantReasoning();
            browser.finalizeAssistantCommentary();
            browser.hideLoader();
            setStreamControlMode(StreamControlMode.STOP);
            if (!backgroundMode) {
                actionNodes.forEach(node -> node.setDisable(true));
                setChatInputBusy();
            }
            browser.showAgentInteraction(interaction, interactionLabels());
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.interaction.pending"));
        });
    }

    private void handleCompleteEvent(final ChatMessageStreamEvent event) {
        NotificationSoundPlayer.playIfEnabled();
        FxThreadRunner.run(() -> {
            browser.hideLoader();
            browser.finalizeAssistantActivity();
            browser.finalizeAssistantReasoning();
            browser.finalizeAssistantCommentary();
            browser.finalizeTodoList();
            if (regenerating) {
                browser.discardRegenerationBackup();
            }
            browser.finishStreamingMessage();
            attachMessageFooter(event.getModelUsed(), event.getAgentUsed(), event.getTokensTotal(), event.getCost());
            final boolean wasBackground = backgroundMode;
            endStreamingUiState();
            if (!wasBackground) {
                chatTextArea.clear();
                setChatInputReady();
                chatTextArea.requestFocus();
            }
        });
    }

    private void handleCancelledEvent() {
        FxThreadRunner.run(() -> {
            browser.clearAssistantActivity();
            browser.clearAssistantCommentary();
            browser.clearAssistantReasoning();
            browser.clearTodoList();
            browser.hideLoader();
            if (regenerating) {
                browser.restoreRegenerationTarget();
            } else {
                browser.finishStreamingMessage();
            }
            final boolean wasBackground = backgroundMode;
            endStreamingUiState();
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.stream.cancelled"));
            if (!wasBackground) {
                chatTextArea.clear();
                setChatInputReady();
                chatTextArea.requestFocus();
            }
        });
    }

    private void handleErrorEvent(ChatMessageStreamEvent event) {
        String errorMessage = event.getErrorMessage();
        String errorLabel = LanguageManager.getLanguageString("chat.stream.error");
        String errorContent = (errorMessage == null || errorMessage.isBlank())
                ? errorLabel
                : errorLabel + ": " + errorMessage;
        logger.error("Streaming error: {}", errorMessage);
        FxThreadRunner.run(() -> {
            browser.clearAssistantActivity();
            browser.clearAssistantCommentary();
            browser.clearAssistantReasoning();
            browser.clearTodoList();
            browser.hideLoader();
            if (regenerating) {
                browser.restoreRegenerationTarget();
            } else {
                browser.finishStreamingMessage();
            }
            browser.addContent(errorContent, ASSISTANT, ChatMessageStyleType.DANGER);
            final boolean wasBackground = backgroundMode;
            endStreamingUiState();
            if (!wasBackground) {
                chatTextArea.clear();
                setChatInputReady();
                chatTextArea.requestFocus();
            }
            statusPrompt.showStatusPrompt(errorLabel);
        });
    }

    private void stopStreaming() {
        ignoreIncomingTokens = true;
        FxThreadRunner.run(() -> {
            browser.clearAssistantActivity();
            streamControlButton.setDisable(true);
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.stream.cancelling"));
        });
        Thread.startVirtualThread(() -> BackendApi.cancelChatMessagesStream(chatThreadId));
    }

    private void regenerateLastResponse() {
        Thread.startVirtualThread(() -> {
            try {
                beginStreaming(true);
                BackendApi.regenerateLastAssistantResponse(chatThreadId);
            } catch (Exception e) {
                logger.error("Error starting regenerate streaming", e);
                String errorLabel = LanguageManager.getLanguageString("chat.stream.error");
                String errorMessage = e.getMessage();
                String errorContent = (errorMessage == null || errorMessage.isBlank())
                        ? errorLabel
                        : errorLabel + ": " + errorMessage;
                FxThreadRunner.run(() -> {
                    browser.hideLoader();
                    endStreamingUiState();
                    statusPrompt.showStatusPrompt(errorLabel);
                    browser.addContent(errorContent, ASSISTANT, ChatMessageStyleType.DANGER);
                    chatTextArea.clear();
                    chatTextArea.requestFocus();
                });
            }
        });
    }

    public void submitAgentInteraction(final String requestId, final AgentInteractionResponse response) {
        FxThreadRunner.run(() -> browser.markAgentInteractionPending(
                requestId,
                LanguageManager.getLanguageString("chat.interaction.processing")
        ));

        Thread.startVirtualThread(() -> {
            try {
                final var resolution = BackendApi.resolveAgentInteraction(requestId, response);
                if (resolution.status() == AgentInteractionResolutionModel.Status.MISSING) {
                    final String message = resolutionMessage(
                            resolution.message(),
                            "chat.interaction.missing"
                    );
                    FxThreadRunner.run(() -> {
                        browser.resolveAgentInteraction(requestId, message);
                        statusPrompt.showStatusPrompt(message);
                    });
                    return;
                }
                if (resolution.status() == AgentInteractionResolutionModel.Status.ERROR) {
                    final String message = resolutionMessage(
                            resolution.message(),
                            "chat.interaction.error",
                            ""
                    );
                    FxThreadRunner.run(() -> {
                        browser.failAgentInteraction(requestId, message);
                        statusPrompt.showStatusPrompt(message);
                    });
                    return;
                }
                final String message = resolutionMessage(
                        resolution.message(),
                        resolution.accepted() ? "chat.interaction.submitted" : "chat.interaction.declined"
                );
                FxThreadRunner.run(() -> {
                    browser.resolveAgentInteraction(requestId, message);
                    statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.stream.connecting"));
                });

                if (resolution.autoContinues()) {
                    return;
                }

                beginStreaming(false);
                BackendApi.retryLastUserMessage(chatThreadId);
            } catch (Exception e) {
                logger.error("Error resolving agent interaction {}", requestId, e);
                final String message = LanguageManager.getLanguageString("chat.interaction.error", e.getMessage());
                FxThreadRunner.run(() -> {
                    browser.failAgentInteraction(requestId, message);
                    statusPrompt.showStatusPrompt(message);
                });
            }
        });
    }

    private String resolutionMessage(final String message, final String fallbackKey, final Object... fallbackArguments) {
        return message == null || message.isBlank()
                ? LanguageManager.getLanguageString(fallbackKey, fallbackArguments)
                : message;
    }

    private void beginStreamingUiState(final boolean isRegeneration) {
        ignoreIncomingTokens = false;
        streamingStarted = false;
        regenerating = isRegeneration;
        replaceExistingAssistantMessageOnStart = isRegeneration && browser.prepareRegenerationTarget();
        resetProcessState();
        browser.clearAssistantActivity();
        browser.clearAssistantCommentary();
        browser.clearAssistantReasoning();
        browser.clearTodoList();
        setStreamControlMode(StreamControlMode.STOP);
        if (!backgroundMode) {
            actionNodes.forEach(node -> node.setDisable(true));
            setChatInputBusy();
            chatTextArea.setText(LanguageManager.getLanguageString("chat.stream.connecting"));
        }
        statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.stream.connecting"));
    }

    private void endStreamingUiState() {
        setStreamControlMode(StreamControlMode.REGENERATE);
        if (!backgroundMode) {
            actionNodes.forEach(node -> node.setDisable(false));
        }
        statusPrompt.clearStatusPrompt();
        ignoreIncomingTokens = false;
        streamingStarted = false;
        regenerating = false;
        replaceExistingAssistantMessageOnStart = false;
        streamingStartedAt = Instant.EPOCH;
        backgroundMode = false;
    }

    private void setChatInputBusy() {
        chatTextArea.setEditable(false);
        chatTextArea.setMouseTransparent(true);
        chatTextArea.setFocusTraversable(false);
        if (!chatTextArea.getStyleClass().contains(CHAT_INPUT_STATUS_STYLE_CLASS)) {
            chatTextArea.getStyleClass().add(CHAT_INPUT_STATUS_STYLE_CLASS);
        }
    }

    private void setChatInputReady() {
        chatTextArea.setEditable(true);
        chatTextArea.setMouseTransparent(false);
        chatTextArea.setFocusTraversable(true);
        chatTextArea.getStyleClass().remove(CHAT_INPUT_STATUS_STYLE_CLASS);
    }

    private void resetProcessState() {
        latestReasoningContent = "";
        latestCommentaryContent = "";
        synchronized (activityLabels) {
            activityLabels.clear();
        }
        streamingStartedAt = Instant.now();
    }

    private void updateComposerProcessStatus() {
        if (backgroundMode) {
            return;
        }
        final StringBuilder status = new StringBuilder();
        if (latestReasoningContent != null && !latestReasoningContent.isBlank()) {
            status.append(LanguageManager.getLanguageString("chat.reasoning.title"))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append(latestReasoningContent.trim());
        }
        if (latestCommentaryContent != null && !latestCommentaryContent.isBlank()) {
            if (!status.isEmpty()) {
                status.append(System.lineSeparator()).append(System.lineSeparator());
            }
            status.append(LanguageManager.getLanguageString("chat.commentary.title"))
                    .append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append(latestCommentaryContent.trim());
        }

        final List<String> activitiesSnapshot;
        synchronized (activityLabels) {
            activitiesSnapshot = List.copyOf(activityLabels);
        }
        if (!activitiesSnapshot.isEmpty()) {
            if (!status.isEmpty()) {
                status.append(System.lineSeparator()).append(System.lineSeparator());
            }
            status.append("Tools")
                    .append(System.lineSeparator())
                    .append(System.lineSeparator())
                    .append(activitiesSnapshot.stream()
                            .map(label -> "• " + label)
                            .collect(Collectors.joining(System.lineSeparator())));
        }

        chatTextArea.setText(status.isEmpty()
                ? LanguageManager.getLanguageString("chat.stream.connecting")
                : status.toString());
        chatTextArea.positionCaret(0);
    }

    private void attachProcessPanelIfNeeded() {
        if ((latestReasoningContent == null || latestReasoningContent.isBlank())
                && (latestCommentaryContent == null || latestCommentaryContent.isBlank())) {
            return;
        }

        final String summary = buildProcessSummary();
        browser.attachProcessPanelToLastAssistantMessage(
                summary,
                LanguageManager.getLanguageString("chat.reasoning.title"),
                latestReasoningContent,
                LanguageManager.getLanguageString("chat.commentary.title"),
                latestCommentaryContent,
                ""
        );
    }

    private String buildProcessSummary() {
        final List<String> parts = new ArrayList<>();
        if (latestReasoningContent != null && !latestReasoningContent.isBlank()) {
            parts.add(LanguageManager.getLanguageString("chat.reasoning.title"));
        }
        if (latestCommentaryContent != null && !latestCommentaryContent.isBlank()) {
            parts.add(LanguageManager.getLanguageString("chat.commentary.title"));
        }
        if (streamingStartedAt != null && !Instant.EPOCH.equals(streamingStartedAt)) {
            parts.add(Duration.between(streamingStartedAt, Instant.now()).toSeconds() + "s");
        }
        return String.join(" · ", parts);
    }

    private void attachMessageFooter(final String modelUsed,
                                        final String agentUsed,
                                        final long tokensTotal,
                                        final double cost) {
        if (modelUsed == null || modelUsed.isBlank()) {
            return;
        }
        final List<String> parts = new ArrayList<>();

        if (latestReasoningContent != null && !latestReasoningContent.isBlank()) {
            parts.add(LanguageManager.getLanguageString("chat.reasoning.title"));
        }
        if (latestCommentaryContent != null && !latestCommentaryContent.isBlank()) {
            parts.add(LanguageManager.getLanguageString("chat.commentary.title"));
        }

        if (agentUsed != null && !agentUsed.isBlank()) {
            parts.add(agentUsed);
        }

        parts.add(formatModelDisplay(modelUsed));

        if (tokensTotal > 0) {
            parts.add(tokensTotal + " tokens");
        }

        if (cost > 0.0) {
            parts.add("$" + formatCost(cost));
        }

        final long durationSeconds = (streamingStartedAt != null && !Instant.EPOCH.equals(streamingStartedAt))
                ? Duration.between(streamingStartedAt, Instant.now()).toSeconds()
                : 0;
        parts.add(durationSeconds + "s");

        final String footerText = String.join(" · ", parts);
        browser.attachMessageFooter(footerText);
    }

    private String formatCost(final double cost) {
        if (cost >= 0.01) {
            return String.format("%.2f", cost);
        }
        if (cost >= 0.001) {
            return String.format("%.3f", cost);
        }
        return String.format("%.4f", cost);
    }

    private String formatModelDisplay(final String modelIdentifier) {
        if (modelIdentifier == null || modelIdentifier.isBlank()) {
            return "unknown";
        }
        final int slashIndex = modelIdentifier.lastIndexOf('/');
        return slashIndex >= 0 ? modelIdentifier.substring(slashIndex + 1) : modelIdentifier;
    }

    private String buildToolsHtml(final List<String> activitiesSnapshot) {
        if (activitiesSnapshot.isEmpty()) {
            return "";
        }
        return "<ul>" + activitiesSnapshot.stream()
                .map(this::compressToolLabel)
                .map(this::escapeHtml)
                .map(item -> "<li>" + item + "</li>")
                .collect(Collectors.joining()) + "</ul>";
    }

    private String compressToolLabel(final String label) {
        if (label == null || label.isBlank()) {
            return "";
        }
        return label.replace("OpenCode: ", "").replace("Tool: ", "").replace("Skill: ", "skill: ");
    }

    private String escapeHtml(final String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String formatActivityLabel(final ChatMessageStreamEvent event) {
        final String activityName = event.getActivityName() == null ? "tool" : event.getActivityName();
        if (event.getActivityType() == ChatMessageStreamEvent.ActivityType.SKILL) {
            return LanguageManager.getLanguageString("chat.stream.activity.skill", activityName);
        }
        if (event.getActivityType() == ChatMessageStreamEvent.ActivityType.MCP) {
            final String detail = event.getActivityDetail();
            if (detail != null && !detail.isBlank() && !"MCP".equalsIgnoreCase(detail)) {
                return LanguageManager.getLanguageString("chat.stream.activity.mcp.server", detail, activityName);
            }
            return LanguageManager.getLanguageString("chat.stream.activity.mcp", activityName);
        }
        return LanguageManager.getLanguageString("chat.stream.activity.tool", activityName);
    }

    private Map<String, String> interactionLabels() {
        return Map.ofEntries(
                Map.entry("badgeApproval", LanguageManager.getLanguageString("chat.interaction.badge.approval")),
                Map.entry("badgeQuestion", LanguageManager.getLanguageString("chat.interaction.badge.question")),
                Map.entry("badgeForm", LanguageManager.getLanguageString("chat.interaction.badge.form")),
                Map.entry("badgeLink", LanguageManager.getLanguageString("chat.interaction.badge.link")),
                Map.entry("approveOnce", LanguageManager.getLanguageString("chat.permission.approve")),
                Map.entry("approveSession", LanguageManager.getLanguageString("chat.permission.approveSession")),
                Map.entry("approveAlways", LanguageManager.getLanguageString("chat.permission.approveAlways")),
                Map.entry("deny", LanguageManager.getLanguageString("chat.permission.deny")),
                Map.entry("submit", LanguageManager.getLanguageString("chat.interaction.submit")),
                Map.entry("decline", LanguageManager.getLanguageString("chat.interaction.decline")),
                Map.entry("cancel", LanguageManager.getLanguageString("chat.interaction.cancel")),
                Map.entry("open", LanguageManager.getLanguageString("chat.interaction.open")),
                Map.entry("confirm", LanguageManager.getLanguageString("chat.interaction.confirm")),
                Map.entry("other", LanguageManager.getLanguageString("chat.interaction.other")),
                Map.entry("required", LanguageManager.getLanguageString("chat.interaction.required")),
                Map.entry("unsupported", LanguageManager.getLanguageString("chat.interaction.unsupported"))
        );
    }

    private void setStreamControlMode(final StreamControlMode mode) {
        streamControlMode = mode;

        if (streamControlButton == null) {
            return;
        }

        if (mode == StreamControlMode.STOP) {
            streamControlButton.setDisable(false);
            streamControlButton.setGraphic(new FontIcon(ICON_STOP));
            Tooltip.install(streamControlButton, new Tooltip(LanguageManager.getLanguageString("chat.button.stop")));
            return;
        }

        streamControlButton.setGraphic(new FontIcon(ICON_REGENERATE));
        streamControlButton.setDisable(!hasAnyUserMessages);
        Tooltip.install(streamControlButton, new Tooltip(LanguageManager.getLanguageString("chat.button.regenerate")));
    }

    public void refreshStreamControlTooltip() {
        if (streamControlButton == null) {
            return;
        }
        final String key = streamControlMode == StreamControlMode.STOP ? "chat.button.stop" : "chat.button.regenerate";
        Tooltip.install(streamControlButton, new Tooltip(LanguageManager.getLanguageString(key)));
    }
}
