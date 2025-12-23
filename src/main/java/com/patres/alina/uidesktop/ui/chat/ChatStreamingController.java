package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.util.FxThreadRunner;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.patres.alina.common.message.ChatMessageRole.ASSISTANT;

public class ChatStreamingController {

    private static final Logger logger = LoggerFactory.getLogger(ChatStreamingController.class);
    private static final String ICON_STOP = "fth-square";
    private static final String ICON_REGENERATE = "fth-refresh-cw";

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
        FxThreadRunner.run(() -> setStreamControlMode(StreamControlMode.REGENERATE));
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
        FxThreadRunner.runAndWait(() -> {
            beginStreamingUiState(isRegeneration);
            browser.showLoader();
        });
    }

    public void handleStartError() {
        FxThreadRunner.run(() -> {
            browser.hideLoader();
            endStreamingUiState();
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.stream.error"));
            chatTextArea.clear();
            chatTextArea.requestFocus();
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
            case COMPLETE -> handleCompleteEvent();
            case CANCELLED -> handleCancelledEvent();
            case ERROR -> handleErrorEvent(event);
        }
    }

    private void handleTokenEvent(ChatMessageStreamEvent event) {
        if (ignoreIncomingTokens) {
            return;
        }
        if (!streamingStarted) {
            streamingStarted = true;
            FxThreadRunner.run(() -> {
                browser.hideLoader();
                browser.startStreamingAssistantMessage(replaceExistingAssistantMessageOnStart);
                statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.stream.streaming"));
            });
        }
        FxThreadRunner.run(() -> browser.appendToStreamingMessage(event.getToken()));
    }

    private void handleCompleteEvent() {
        FxThreadRunner.run(() -> {
            browser.hideLoader();
            if (regenerating) {
                browser.discardRegenerationBackup();
            }
            browser.finishStreamingMessage();
            endStreamingUiState();
            chatTextArea.clear();
            chatTextArea.requestFocus();
        });
    }

    private void handleCancelledEvent() {
        FxThreadRunner.run(() -> {
            browser.hideLoader();
            if (regenerating) {
                browser.restoreRegenerationTarget();
            } else {
                browser.finishStreamingMessage();
            }
            endStreamingUiState();
            statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.stream.cancelled"));
            chatTextArea.clear();
            chatTextArea.requestFocus();
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
            browser.hideLoader();
            if (regenerating) {
                browser.restoreRegenerationTarget();
            } else {
                browser.finishStreamingMessage();
            }
            browser.addContent(errorContent, ASSISTANT, ChatMessageStyleType.DANGER);
            endStreamingUiState();
            chatTextArea.clear();
            chatTextArea.requestFocus();
            statusPrompt.showStatusPrompt(errorLabel);
        });
    }

    private void stopStreaming() {
        ignoreIncomingTokens = true;
        FxThreadRunner.run(() -> {
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

    private void beginStreamingUiState(final boolean isRegeneration) {
        ignoreIncomingTokens = false;
        streamingStarted = false;
        regenerating = isRegeneration;
        replaceExistingAssistantMessageOnStart = isRegeneration && browser.prepareRegenerationTarget();
        setStreamControlMode(StreamControlMode.STOP);
        actionNodes.forEach(node -> node.setDisable(true));
        statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.stream.connecting"));
    }

    private void endStreamingUiState() {
        setStreamControlMode(StreamControlMode.REGENERATE);
        actionNodes.forEach(node -> node.setDisable(false));
        statusPrompt.clearStatusPrompt();
        ignoreIncomingTokens = false;
        streamingStarted = false;
        regenerating = false;
        replaceExistingAssistantMessageOnStart = false;
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
