package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.message.TodoItem;
import com.patres.alina.common.permission.PermissionApprovalAction;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.util.FxThreadRunner;
import com.patres.alina.uidesktop.ui.util.NotificationSoundPlayer;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    private final HBox messageInputRow;
    private final VBox permissionComposerPane;
    private final Label permissionComposerTitleLabel;
    private final Label permissionComposerMessageLabel;
    private final Label permissionComposerStatusLabel;
    private final Button permissionApproveButton;
    private final Button permissionApproveAlwaysButton;
    private final Button permissionDenyButton;
    private final ChatStatusPrompt statusPrompt;
    private final String chatThreadId;

    private volatile boolean streamingStarted;
    private volatile boolean ignoreIncomingTokens;
    private volatile StreamControlMode streamControlMode = StreamControlMode.REGENERATE;
    private volatile boolean regenerating;
    private volatile boolean replaceExistingAssistantMessageOnStart;
    private volatile boolean hasAnyUserMessages;
    private volatile String activePermissionRequestId;
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
                                   HBox messageInputRow,
                                   VBox permissionComposerPane,
                                   Label permissionComposerTitleLabel,
                                   Label permissionComposerMessageLabel,
                                   Label permissionComposerStatusLabel,
                                   Button permissionApproveButton,
                                   Button permissionApproveAlwaysButton,
                                   Button permissionDenyButton,
                                   ChatStatusPrompt statusPrompt,
                                   String chatThreadId,
                                   boolean hasAnyUserMessages) {
        this.browser = browser;
        this.streamControlButton = streamControlButton;
        this.actionNodes = actionNodes;
        this.chatTextArea = chatTextArea;
        this.messageInputRow = messageInputRow;
        this.permissionComposerPane = permissionComposerPane;
        this.permissionComposerTitleLabel = permissionComposerTitleLabel;
        this.permissionComposerMessageLabel = permissionComposerMessageLabel;
        this.permissionComposerStatusLabel = permissionComposerStatusLabel;
        this.permissionApproveButton = permissionApproveButton;
        this.permissionApproveAlwaysButton = permissionApproveAlwaysButton;
        this.permissionDenyButton = permissionDenyButton;
        this.statusPrompt = statusPrompt;
        this.chatThreadId = chatThreadId;
        this.hasAnyUserMessages = hasAnyUserMessages;
    }

    public void initialize() {
        FxThreadRunner.run(() -> {
            setStreamControlMode(StreamControlMode.REGENERATE);
            hidePermissionComposer();
            permissionApproveButton.setOnAction(_ -> submitPermissionAction(PermissionApprovalAction.APPROVE_ONCE));
            permissionApproveAlwaysButton.setOnAction(_ -> submitPermissionAction(PermissionApprovalAction.APPROVE_ALWAYS));
            permissionDenyButton.setOnAction(_ -> submitPermissionAction(PermissionApprovalAction.DENY));
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
            case PERMISSION_REQUEST -> handlePermissionRequestEvent(event);
            case COMPLETE -> handleCompleteEvent(event);
            case CANCELLED -> handleCancelledEvent();
            case ERROR -> handleErrorEvent(event);
        }
    }

    private void handleCommentaryEvent(final ChatMessageStreamEvent event) {
        final String content = event.getCommentaryContent();
        if (content == null || content.isBlank()) {
            return;
        }
        latestCommentaryContent = content;
        FxThreadRunner.run(() -> {
            updateComposerProcessStatus();
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.commentary.status"));
        });
    }

    private void handleReasoningEvent(final ChatMessageStreamEvent event) {
        final String content = event.getReasoningContent();
        if (content == null || content.isBlank()) {
            return;
        }
        latestReasoningContent = content;
        FxThreadRunner.run(() -> {
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
            browser.showAssistantActivity(label, detail);
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

    private void handlePermissionRequestEvent(final ChatMessageStreamEvent event) {
        final String title = formatPermissionTitle(event);
        final String message = formatPermissionMessage(event);
        FxThreadRunner.run(() -> {
            browser.hideLoader();
            endStreamingUiState();
            showPermissionComposer(event.getPermissionRequestId(), title, message);
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.permission.pending"));
        });
    }

    private void handleCompleteEvent(final ChatMessageStreamEvent event) {
        NotificationSoundPlayer.playIfEnabled();
        FxThreadRunner.run(() -> {
            browser.hideLoader();
            hidePermissionComposer();
            browser.finalizeAssistantActivity();
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
            hidePermissionComposer();
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
            hidePermissionComposer();
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

    private void submitPermissionAction(final PermissionApprovalAction action) {
        final String requestId = activePermissionRequestId;
        if (requestId == null || requestId.isBlank()) {
            return;
        }

        FxThreadRunner.run(() -> browser.markAssistantPermissionRequestPending(
                requestId,
                LanguageManager.getLanguageString("chat.permission.processing")
        ));

        FxThreadRunner.run(() -> markPermissionComposerPending(LanguageManager.getLanguageString("chat.permission.processing")));

        Thread.startVirtualThread(() -> {
            try {
                final var resolution = BackendApi.resolvePermissionRequest(requestId, action);
                if (!resolution.found()) {
                    FxThreadRunner.run(() -> browser.resolveAssistantPermissionRequest(
                            requestId,
                            resolution.message() == null
                                    ? LanguageManager.getLanguageString("chat.permission.missing")
                                    : resolution.message()
                    ));
                    FxThreadRunner.run(() -> {
                        hidePermissionComposer();
                        endStreamingUiState();
                        chatTextArea.clear();
                        setChatInputReady();
                        chatTextArea.requestFocus();
                        statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.permission.missing"));
                    });
                    return;
                }

                if (action == PermissionApprovalAction.DENY || !resolution.approved()) {
                    FxThreadRunner.run(() -> {
                        browser.resolveAssistantPermissionRequest(
                                requestId,
                                resolution.message() == null
                                        ? LanguageManager.getLanguageString("chat.permission.denied")
                                        : resolution.message()
                        );
                        hidePermissionComposer();
                        endStreamingUiState();
                        chatTextArea.clear();
                        setChatInputReady();
                        chatTextArea.requestFocus();
                        statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.permission.denied"));
                    });
                    return;
                }

                final String approvedText = resolution.persisted()
                        ? LanguageManager.getLanguageString("chat.permission.approvedAlways")
                        : LanguageManager.getLanguageString("chat.permission.approvedOnce");
                final String approvalMessage = resolution.message() == null || resolution.message().isBlank()
                        ? approvedText
                        : resolution.message();

                FxThreadRunner.run(() -> {
                    browser.resolveAssistantPermissionRequest(requestId, approvalMessage);
                    hidePermissionComposer();
                    statusPrompt.showStatusPrompt(
                            resolution.autoContinues()
                                    ? LanguageManager.getLanguageString("chat.stream.connecting")
                                    : LanguageManager.getLanguageString("chat.stream.connecting")
                    );
                });

                if (resolution.autoContinues()) {
                    return;
                }

                beginStreaming(false);
                BackendApi.retryLastUserMessage(chatThreadId);
            } catch (Exception e) {
                logger.error("Error resolving permission request {}", requestId, e);
                FxThreadRunner.run(() -> {
                    browser.resolveAssistantPermissionRequest(
                            requestId,
                            LanguageManager.getLanguageString("chat.permission.error", e.getMessage())
                    );
                    hidePermissionComposer();
                    endStreamingUiState();
                    chatTextArea.clear();
                    setChatInputReady();
                    chatTextArea.requestFocus();
                    statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.permission.error", e.getMessage()));
                });
            }
        });
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
        hidePermissionComposer();
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
        return label.replace("OpenCode: ", "").replace("Skill: ", "skill: ");
    }

    private String escapeHtml(final String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private void showPermissionComposer(final String requestId, final String title, final String message) {
        activePermissionRequestId = requestId;
        permissionComposerTitleLabel.setText(title);
        permissionComposerMessageLabel.setText(message);
        permissionComposerStatusLabel.setText("");
        permissionApproveButton.setText(LanguageManager.getLanguageString("chat.permission.approve"));
        permissionApproveAlwaysButton.setText(LanguageManager.getLanguageString("chat.permission.approveAlways"));
        permissionDenyButton.setText(LanguageManager.getLanguageString("chat.permission.deny"));
        permissionApproveButton.setDisable(false);
        permissionApproveAlwaysButton.setDisable(false);
        permissionDenyButton.setDisable(false);
        messageInputRow.setManaged(false);
        messageInputRow.setVisible(false);
        permissionComposerPane.setManaged(true);
        permissionComposerPane.setVisible(true);
    }

    private void markPermissionComposerPending(final String statusText) {
        permissionApproveButton.setDisable(true);
        permissionApproveAlwaysButton.setDisable(true);
        permissionDenyButton.setDisable(true);
        permissionComposerStatusLabel.setText(statusText == null ? "" : statusText);
    }

    private void hidePermissionComposer() {
        activePermissionRequestId = null;
        permissionComposerPane.setVisible(false);
        permissionComposerPane.setManaged(false);
        messageInputRow.setVisible(true);
        messageInputRow.setManaged(true);
        permissionComposerStatusLabel.setText("");
        permissionComposerMessageLabel.setText("");
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

    private String formatPermissionTitle(final ChatMessageStreamEvent event) {
        return switch (event.getPermissionType()) {
            case MCP -> LanguageManager.getLanguageString("chat.permission.title.mcp", event.getPermissionValue());
            case BASH -> LanguageManager.getLanguageString("chat.permission.title.bash", event.getPermissionValue());
            case TOOL -> LanguageManager.getLanguageString("chat.permission.title.tool", event.getPermissionValue());
        };
    }

    private String formatPermissionMessage(final ChatMessageStreamEvent event) {
        final String reason = event.getPermissionMessage() == null || event.getPermissionMessage().isBlank()
                ? LanguageManager.getLanguageString("chat.permission.reason.unknown")
                : event.getPermissionMessage();
        final String policy = event.getPermissionConfigPath() == null || event.getPermissionConfigPath().isBlank()
                ? LanguageManager.getLanguageString("chat.permission.policy.unknown")
                : event.getPermissionConfigPath();
        final String rule = event.getPermissionMatchedRule() == null || event.getPermissionMatchedRule().isBlank()
                ? LanguageManager.getLanguageString("chat.permission.rule.none")
                : event.getPermissionMatchedRule();
        return LanguageManager.getLanguageString(
                "chat.permission.message",
                reason,
                policy,
                rule
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
            return;
        }

        streamControlButton.setGraphic(new FontIcon(ICON_REGENERATE));
        streamControlButton.setDisable(!hasAnyUserMessages);
    }
}
