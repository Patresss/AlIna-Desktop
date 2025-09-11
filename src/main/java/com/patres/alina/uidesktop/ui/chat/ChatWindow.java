package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.event.ChatMessageReceivedEvent;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ChatMessageStyleType;
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
import java.util.function.Consumer;

import static com.patres.alina.common.message.ChatMessageRole.ASSISTANT;


public class ChatWindow extends BorderPane {

    private static final Logger logger = LoggerFactory.getLogger(ChatWindow.class);

    private final ChatThread chatThread;
    private final List<ChatMessageResponseModel> messages;
    private final ApplicationWindow applicationWindow;

    private final Consumer<SpeechShortcutTriggeredEvent> speechShortcutTriggeredEventConsumer = event -> triggerSpeechAction();
    private final Consumer<FocusShortcutTriggeredEvent> focusShortcutTriggeredEventConsumer = event -> triggerFocusAction();
    private final Consumer<ChatMessageStreamEvent> chatMessageStreamEventConsumer = event -> handleChatMessageStreamEvent(event);

    private StringBuilder currentStreamingMessage = new StringBuilder();
    private boolean isCurrentlyStreaming = false;



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
    private TextArea chatTextArea;

    @FXML
    private Label commandLabel;

    @FXML
    private Label chatInfoLabel;

    private Browser browser;
    private RecordMode recordMode = RecordMode.PREPARE_TO_START_RECORDING;
    private final AudioRecorder audioRecorder = new AudioRecorder();

    private final List<Node> actionNodes;
    private final List<Node> nomRecordingActionNodes;

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
        initializeInputHandler();
        setCurrentCommand(null);

        configureRecordingButton();
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
                sendMessage();
            }
        }
    }

    @FXML
    public void sendMessage() {
        final String message = chatTextArea.getText().trim();
        chatTextArea.clear();
        displayMessage(message, ChatMessageRole.USER, ChatMessageStyleType.NONE);
        prepareContextAndSendMessageToService(message);
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
        try {
            final MessageWithContextGenerator messageWithContextGenerator = new MessageWithContextGenerator(message);
            final String messageToSend = messageWithContextGenerator.replacePathsWithContents();
            sendMessageToService(messageToSend);
        } catch (MessageContextException e) {
            logger.error("Cannot generate context for message '{}', sending original message", message, e);
            handleError(e.getMessage());
            sendMessageToService(message);
        }
    }

    private void sendMessageToService(final String message) {
        Thread.startVirtualThread(() -> {
            try {
                Platform.runLater(() -> {
                    browser.showLoader();
                    actionNodes.forEach(node -> node.setDisable(true));
                    chatTextArea.setText(LanguageManager.getLanguageString("chat.message.sending"));
                });

                final ChatMessageSendModel chatMessageSendModel = new ChatMessageSendModel(message, chatThread.id(), getCurrentCommandId());
                
                BackendApi.sendChatMessagesStream(chatMessageSendModel);
                
                // Note: UI updates will be handled by the stream event handler
                // The loader will be hidden and controls re-enabled in the stream completion handler
                
            } catch (Exception e) {
                // Handle any immediate errors (e.g., connection issues before streaming starts)
                logger.error("Error starting streaming message", e);
                final ChatMessageResponseModel errorResponse = handelExceptionAsMessage(message, e, e.getMessage());
                displayMessage(errorResponse);
                
                Platform.runLater(() -> {
                    browser.hideLoader(); // Hide loader on error
                    actionNodes.forEach(node -> node.setDisable(false));
                    chatTextArea.clear();
                    chatTextArea.requestFocus();
                });
            }
        });
    }


    private void handleChatMessageReceivedEvent(ChatMessageReceivedEvent event) {
        actionNodes.forEach(node -> node.setDisable(false));
        chatTextArea.clear();

        Platform.runLater(() -> {
            chatTextArea.requestFocus();
        });


    }

    private void handleChatMessageStreamEvent(ChatMessageStreamEvent event) {
        if (!event.getThreadId().equals(chatThread.id())) {
            return; // Event is not for this thread
        }

        switch (event.getEventType()) {
            case TOKEN -> {
                if (!isCurrentlyStreaming) {
                    // First token arrived - hide loader and start streaming display
                    isCurrentlyStreaming = true;
                    currentStreamingMessage = new StringBuilder();
                    Platform.runLater(() -> {
                        browser.hideLoader(); // Hide loader on first token
                        browser.startStreamingAssistantMessage();
                    });
                }
                // Continue with existing token handling...
                currentStreamingMessage.append(event.getToken());
                Platform.runLater(() -> browser.appendToStreamingMessage(event.getToken()));
            }
            case COMPLETE -> {
                isCurrentlyStreaming = false;
                Platform.runLater(() -> {
                    browser.finishStreamingMessage();
                    actionNodes.forEach(node -> node.setDisable(false));
                    chatTextArea.clear();
                    chatTextArea.requestFocus();
                });
            }
            case ERROR -> {
                isCurrentlyStreaming = false;
                logger.error("Streaming error: {}", event.getErrorMessage());
                Platform.runLater(() -> {
                    browser.hideLoader(); // Ensure loader is hidden on error
                    browser.finishStreamingMessage();
                    displayMessage("Error: " + event.getErrorMessage(), ASSISTANT, ChatMessageStyleType.DANGER);
                    actionNodes.forEach(node -> node.setDisable(false));
                    chatTextArea.clear();
                    chatTextArea.requestFocus();
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

    public Label getCommandLabel() {
        return commandLabel;
    }

    private void setCurrentCommand(final CardListItem currentCommand) {
        this.currentCommand = currentCommand;
        if (currentCommand == null) {
            chatTextArea.setPromptText(LanguageManager.getLanguageString("chat.message.type.noCommand"));
            commandLabel.setText(LanguageManager.getLanguageString("chat.command.noCommand"));
            commandLabel.setGraphic(null);
        } else {
            chatTextArea.setPromptText(LanguageManager.getLanguageString("chat.message.type.command", currentCommand.description()));
            commandLabel.setText(LanguageManager.getLanguageString("chat.command.current", currentCommand.name()));
            commandLabel.setGraphic(new FontIcon(currentCommand.icon()));
        }

    }

    public TextArea getChatTextArea() {
        return chatTextArea;
    }


}
