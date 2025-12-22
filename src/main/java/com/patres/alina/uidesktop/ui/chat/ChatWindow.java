package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.message.OnMessageCompleteCallback;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.messagecontext.MessageContextException;
import com.patres.alina.uidesktop.messagecontext.MessageWithContextGenerator;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.common.event.shortcut.FocusShortcutTriggeredEvent;
import com.patres.alina.uidesktop.common.event.shortcut.SpeechShortcutTriggeredEvent;
import com.patres.alina.uidesktop.microphone.AudioRecorder;
import com.patres.alina.uidesktop.command.SearchCommandPopup;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static com.patres.alina.common.message.ChatMessageRole.ASSISTANT;


public class ChatWindow extends BorderPane {

    private static final Logger logger = LoggerFactory.getLogger(ChatWindow.class);
    private static final String ICON_STOP = "fth-square";
    private static final String ICON_REGENERATE = "fth-refresh-cw";

    private final ChatThread chatThread;
    private final List<ChatMessageResponseModel> messages;
    private final ApplicationWindow applicationWindow;

    private final Consumer<SpeechShortcutTriggeredEvent> speechShortcutTriggeredEventConsumer = event -> triggerSpeechAction();
    private final Consumer<FocusShortcutTriggeredEvent> focusShortcutTriggeredEventConsumer = event -> triggerFocusAction();
    private final Consumer<ChatMessageStreamEvent> chatMessageStreamEventConsumer = event -> handleChatMessageStreamEvent(event);

    private boolean streamingStarted;
    private boolean ignoreIncomingTokens;
    private StreamControlMode streamControlMode = StreamControlMode.REGENERATE;
    private boolean regenerating;
    private boolean replaceExistingAssistantMessageOnStart;

    private String basePromptText = "";
    private boolean showingStatusPrompt;

    private enum StreamControlMode {
        STOP,
        REGENERATE
    }



    private SearchCommandPopup popup;
    private CardListItem currentCommand;

    @FXML
    private StackPane chatAnswersPane;

    @FXML
    private VBox footerPane;

    @FXML
    private Button sendButton;

    @FXML
    private Button recordButton;

    @FXML
    private Button streamControlButton;

    @FXML
    private TextArea chatTextArea;

    @FXML
    private VBox inputButtonsBox;

    @FXML
    private Label commandLabel;

    private Browser browser;
    private RecordMode recordMode = RecordMode.PREPARE_TO_START_RECORDING;
    private final AudioRecorder audioRecorder = new AudioRecorder();

    private final List<Node> actionNodes;
    private final List<Node> nomRecordingActionNodes;
    private boolean hasAnyUserMessages;

    public ChatWindow(ChatThread chatThread, ApplicationWindow applicationWindow) {
        super();
        this.chatThread = chatThread;
        this.applicationWindow = applicationWindow;
        messages = BackendApi.getMessagesByThreadId(chatThread.id());

        try {
            var loader = new FXMLLoader(
                    Resources.getResource("fxml/chat-window.fxml").toURL()
            );
            loader.setController(ChatWindow.this);
            loader.setRoot(this);
            loader.load();
            setMaxWidth(Double.MAX_VALUE);
            actionNodes = List.of(sendButton, recordButton, chatTextArea);
            nomRecordingActionNodes = actionNodes.stream()
                    .filter(it -> it != recordButton)
                    .toList();

        } catch (IOException e) {
            throw new RuntimeException("Unable to load FXML file", e);
        }

        initEventsSubscriptions();
    }

    private void initEventsSubscriptions() {
        DefaultEventBus.getInstance().subscribe(
                SpeechShortcutTriggeredEvent.class,
                speechShortcutTriggeredEventConsumer
        );

        DefaultEventBus.getInstance().subscribe(
                FocusShortcutTriggeredEvent.class,
                focusShortcutTriggeredEventConsumer
        );

        DefaultEventBus.getInstance().subscribe(
                ChatMessageStreamEvent.class,
                chatMessageStreamEventConsumer
        );
    }

    public void unsubscribeEvents() {
        DefaultEventBus.getInstance().unsubscribe(
                SpeechShortcutTriggeredEvent.class,
                speechShortcutTriggeredEventConsumer
        );

        DefaultEventBus.getInstance().unsubscribe(
                FocusShortcutTriggeredEvent.class,
                focusShortcutTriggeredEventConsumer
        );

        DefaultEventBus.getInstance().unsubscribe(
                ChatMessageStreamEvent.class,
                chatMessageStreamEventConsumer
        );
    }

    @FXML
    public void initialize() {
        browser = new Browser();
        chatAnswersPane.getChildren().add(browser);

        messages.forEach(this::displayMessage);
        hasAnyUserMessages = messages.stream().anyMatch(m -> m.seder() == ChatMessageRole.USER);
        initializeInputHandler();
        setCurrentCommand(null);

        configureStreamingControls();
        bindInputHeightToButtonsBox();
        configureRecordingButton();
    }

    private void bindInputHeightToButtonsBox() {
        if (inputButtonsBox == null) {
            return;
        }
        chatTextArea.minHeightProperty().bind(inputButtonsBox.heightProperty());
        chatTextArea.prefHeightProperty().bind(inputButtonsBox.heightProperty());
    }

    private void configureStreamingControls() {
        setStreamControlMode(StreamControlMode.REGENERATE);
    }

    private void configureRecordingButton() {
        recordButton.setOnAction(event -> triggerSpeechAction());
    }

    private void triggerSpeechAction() {
        if (recordMode == RecordMode.PREPARE_TO_START_RECORDING) {
            recordMode = RecordMode.PREPARE_TO_STOP_RECORDING;
            startRecording();
        } else {
            recordMode = RecordMode.PREPARE_TO_START_RECORDING;
            stopAndSendRecording();
        }
        Platform.runLater(() -> recordButton.setGraphic(recordMode.getFontIcon()));
    }

    private void triggerFocusAction() {
        final Window window = chatTextArea.getScene().getWindow();
        if (window instanceof Stage stage) {
            Platform.runLater(() -> {
                if (chatTextArea.isFocused()) {
                    stage.toBack();
                } else {
                    chatTextArea.requestFocus();
                    chatTextArea.selectEnd();
                    stage.setIconified(true); // requestFocus doesn't activate window https://stackoverflow.com/questions/28086570/make-javafx-window-active
                    stage.setIconified(false);
                }
            });
        }
    }

    public void startRecording() {
        nomRecordingActionNodes.forEach(node -> node.setDisable(true));
        chatTextArea.setText(LanguageManager.getLanguageString("chat.message.sending"));
        audioRecorder.startRecording();
    }

    public void stopAndSendRecording() {
        Thread.startVirtualThread(() -> {
            audioRecorder.stopRecording()
                    .ifPresentOrElse(
                            this::stopAndSendRecording,
                            () -> handleError("Cannot stop recording. File is not found"));
        });
    }

    private void handleError(String message) {
        logger.error(message);
        displayMessage(message, ASSISTANT, ChatMessageStyleType.DANGER);
        actionNodes.forEach(node -> node.setDisable(false));
    }

    private void stopAndSendRecording(File audio) {
        try {
            actionNodes.forEach(node -> node.setDisable(true));
            String message = BackendApi.sendChatMessagesAsAudio(audio).content();
            displayMessage(message, ChatMessageRole.USER, ChatMessageStyleType.NONE);
            sendMessageToService(message);
        } catch (Exception e) {
            final ChatMessageResponseModel chatMessageResponseModel = handelExceptionAsMessage("<speech>", e, e.getMessage());
            displayMessage(chatMessageResponseModel);
        } finally {
            if (audio != null) {
                audio.delete();
            }
        }
    }

    private void initializeInputHandler() {
        popup = new SearchCommandPopup(chatTextArea);
        chatTextArea.textProperty().addListener(
                (observableValue, oldValue, newValue) -> popup.handleTextChangeListener(newValue, applicationWindow.getStage())
        );
        popup.getSelectedCommandProperty().addListener((observable, oldValue, newValue) -> {
            setCurrentCommand(newValue);
        });
    }


    @FXML
    public void chatTextAreaOnKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            if (event.isShiftDown()) {
                chatTextArea.insertText(chatTextArea.getCaretPosition(), System.lineSeparator());
            } else if (!chatTextArea.getText().isBlank()) {
                sendMessageFromUi();
            }
        }
    }

    @FXML
    public void sendMessageFromUi() {
        final String message = chatTextArea.getText().trim();
        chatTextArea.clear();
        displayMessage(message, ChatMessageRole.USER, ChatMessageStyleType.NONE);
        hasAnyUserMessages = true;
        prepareContextAndSendMessageToService(message);
    }

    public void sendMessage(final String message, final String commandId, final OnMessageCompleteCallback onComplete) {
        displayMessage(message, ChatMessageRole.USER, ChatMessageStyleType.NONE);
        hasAnyUserMessages = true;
        prepareContextAndSendMessageToService(message, commandId, onComplete);
    }

    private void displayMessage(final ChatMessageResponseModel message) {
        displayMessage(message.content(), message.seder(), message.styleType());
    }

    private void displayMessage(final String text,
                                final ChatMessageRole chatMessageRole,
                                final ChatMessageStyleType chatMessageStyleType) {
        Platform.runLater(() -> browser.addContent(text, chatMessageRole, chatMessageStyleType));
    }

    private void prepareContextAndSendMessageToService(final String message) {
        prepareContextAndSendMessageToService(message, getCurrentCommandId());
    }

    private void prepareContextAndSendMessageToService(final String message, final String commandId) {
        prepareContextAndSendMessageToService(message, commandId, null);
    }

    private void prepareContextAndSendMessageToService(final String message, final String commandId, final OnMessageCompleteCallback onComplete) {
        try {
            final MessageWithContextGenerator messageWithContextGenerator = new MessageWithContextGenerator(message);
            final String messageToSend = messageWithContextGenerator.replacePathsWithContents();
            sendMessageToService(messageToSend, commandId, onComplete);
        } catch (MessageContextException e) {
            logger.error("Cannot generate context for message '{}', sending original message", message, e);
            handleError(e.getMessage());
            sendMessageToService(message, commandId, onComplete);
        }
    }

    private void sendMessageToService(final String message) {
        sendMessageToService(message, getCurrentCommandId());
    }

    private void sendMessageToService(final String message, final String commandId) {
        sendMessageToService(message, commandId, null);
    }

    private void sendMessageToService(final String message, final String commandId, final OnMessageCompleteCallback onComplete) {
        Thread.startVirtualThread(() -> {
            try {
                runOnFxThreadAndWait(() -> {
                    beginStreamingUiState(false);
                    browser.showLoader();
                });

                final ChatMessageSendModel chatMessageSendModel = new ChatMessageSendModel(message, chatThread.id(), commandId, onComplete);

                BackendApi.sendChatMessagesStream(chatMessageSendModel);
                
                // Note: UI updates will be handled by the stream event handler
                // The loader will be hidden and controls re-enabled in the stream completion handler
                
            } catch (Exception e) {
                // Handle any immediate errors (e.g., connection issues before streaming starts)
                logger.error("Error starting streaming message", e);
                final ChatMessageResponseModel errorResponse = handelExceptionAsMessage(message, e, e.getMessage());
                displayMessage(errorResponse);
                
                runOnFxThread(() -> {
                    browser.hideLoader(); // Hide loader on error
                    endStreamingUiState();
                    showStatusPrompt(LanguageManager.getLanguageString("chat.stream.error"));
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
        showStatusPrompt(LanguageManager.getLanguageString("chat.stream.connecting"));
    }

    private void endStreamingUiState() {
        setStreamControlMode(StreamControlMode.REGENERATE);

        actionNodes.forEach(node -> node.setDisable(false));
        clearStatusPrompt();
        ignoreIncomingTokens = false;
        streamingStarted = false;
        regenerating = false;
        replaceExistingAssistantMessageOnStart = false;
    }

    @FXML
    public void streamControlFromUi() {
        if (streamControlMode == StreamControlMode.STOP) {
            stopStreaming();
            return;
        }
        regenerateLastResponse();
    }

    private void stopStreaming() {
        ignoreIncomingTokens = true;
        streamControlButton.setDisable(true);
        showStatusPrompt(LanguageManager.getLanguageString("chat.stream.cancelling"));
        Thread.startVirtualThread(() -> BackendApi.cancelChatMessagesStream(chatThread.id()));
    }

    private void regenerateLastResponse() {
        Thread.startVirtualThread(() -> {
            try {
                runOnFxThreadAndWait(() -> {
                    beginStreamingUiState(true);
                    browser.showLoader();
                });
                BackendApi.regenerateLastAssistantResponse(chatThread.id());
            } catch (Exception e) {
                logger.error("Error starting regenerate streaming", e);
                runOnFxThread(() -> {
                    browser.hideLoader();
                    endStreamingUiState();
                    showStatusPrompt(LanguageManager.getLanguageString("chat.stream.error"));
                    displayMessage("Error: " + e.getMessage(), ASSISTANT, ChatMessageStyleType.DANGER);
                    chatTextArea.clear();
                    chatTextArea.requestFocus();
                });
            }
        });
    }

    private void runOnFxThread(final Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        Platform.runLater(action);
    }

    private void runOnFxThreadAndWait(final Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }
        final CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        try {
            // Avoid deadlocks: this is only used from background/virtual threads.
            if (!latch.await(5, TimeUnit.SECONDS)) {
                logger.warn("Timed out waiting for JavaFX thread to apply UI state");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void handleChatMessageStreamEvent(ChatMessageStreamEvent event) {
        if (!event.getThreadId().equals(chatThread.id())) {
            return;
        }

        switch (event.getEventType()) {
            case TOKEN -> {
                if (ignoreIncomingTokens) {
                    return;
                }
                if (!streamingStarted) {
                    // First token arrived - hide loader and start streaming display
                    streamingStarted = true;
                    Platform.runLater(() -> {
                        browser.hideLoader(); // Hide loader on first token
                        browser.startStreamingAssistantMessage(replaceExistingAssistantMessageOnStart);
                        showStatusPrompt(LanguageManager.getLanguageString("chat.stream.streaming"));
                    });
                }
                Platform.runLater(() -> browser.appendToStreamingMessage(event.getToken()));
            }
            case COMPLETE -> {
                Platform.runLater(() -> {
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
            case CANCELLED -> {
                Platform.runLater(() -> {
                    browser.hideLoader();
                    if (regenerating) {
                        browser.restoreRegenerationTarget();
                    } else {
                        browser.finishStreamingMessage();
                    }
                    endStreamingUiState();
                    showStatusPrompt(LanguageManager.getLanguageString("chat.stream.cancelled"));
                    chatTextArea.clear();
                    chatTextArea.requestFocus();
                });
            }
            case ERROR -> {
                logger.error("Streaming error: {}", event.getErrorMessage());
                Platform.runLater(() -> {
                    browser.hideLoader(); // Ensure loader is hidden on error
                    if (regenerating) {
                        browser.restoreRegenerationTarget();
                    } else {
                        browser.finishStreamingMessage();
                    }
                    displayMessage("Error: " + event.getErrorMessage(), ASSISTANT, ChatMessageStyleType.DANGER);
                    endStreamingUiState();
                    chatTextArea.clear();
                    chatTextArea.requestFocus();
                    showStatusPrompt(LanguageManager.getLanguageString("chat.stream.error"));
                });
            }
        }
    }

    private String getCurrentCommandId() {
        return currentCommand == null ? null : currentCommand.id();
    }

    private static ChatMessageResponseModel handelExceptionAsMessage(final String content, final Exception e, final String message) {
        logger.error("Cannot send a message `{}` to server", content, e);
        final String errorMessage = LanguageManager.getLanguageString("chat.message.error");
        return new ChatMessageResponseModel(errorMessage + ": " + message, ASSISTANT, LocalDateTime.now(), ChatMessageStyleType.DANGER, null);
    }

    public ChatThread getChatThread() {
        return chatThread;
    }

    private void setCurrentCommand(final CardListItem currentCommand) {
        this.currentCommand = currentCommand;
        if (currentCommand == null) {
            setBasePromptText(LanguageManager.getLanguageString("chat.message.type.noCommand"));
            commandLabel.setText(LanguageManager.getLanguageString("chat.command.noCommand"));
            commandLabel.setGraphic(null);
        } else {
            setBasePromptText(LanguageManager.getLanguageString("chat.message.type.command", currentCommand.description()));
            commandLabel.setText(LanguageManager.getLanguageString("chat.command.current", currentCommand.name()));
            commandLabel.setGraphic(new FontIcon(currentCommand.icon()));
        }

    }

    private void setBasePromptText(final String text) {
        basePromptText = text == null ? "" : text;
        if (!showingStatusPrompt) {
            chatTextArea.setPromptText(basePromptText);
        }
    }

    private void showStatusPrompt(final String text) {
        showingStatusPrompt = true;
        chatTextArea.clear();
        chatTextArea.setPromptText(text == null ? "" : text);
    }

    private void clearStatusPrompt() {
        showingStatusPrompt = false;
        chatTextArea.setPromptText(basePromptText);
    }

    private void setStreamControlMode(final StreamControlMode mode) {
        this.streamControlMode = mode;

        if (streamControlButton == null) {
            return;
        }

        if (mode == StreamControlMode.STOP) {
            streamControlButton.setDisable(false);
            streamControlButton.setGraphic(new FontIcon(ICON_STOP));
            return;
        }

        // REGENERATE
        streamControlButton.setGraphic(new FontIcon(ICON_REGENERATE));
        streamControlButton.setDisable(!hasAnyUserMessages);
    }

}
