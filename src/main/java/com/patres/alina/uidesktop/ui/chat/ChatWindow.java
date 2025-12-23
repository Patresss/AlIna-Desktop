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
import com.patres.alina.uidesktop.command.SearchCommandPopup;
import com.patres.alina.uidesktop.microphone.AudioRecorder;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.util.FxThreadRunner;
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

    private Consumer<SpeechShortcutTriggeredEvent> speechShortcutTriggeredEventConsumer;
    private final Consumer<FocusShortcutTriggeredEvent> focusShortcutTriggeredEventConsumer = event -> triggerFocusAction();
    private Consumer<ChatMessageStreamEvent> chatMessageStreamEventConsumer;

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
    private ChatStatusPrompt statusPrompt;
    private ChatRecordingController recordingController;
    private ChatStreamingController streamingController;

    private List<Node> actionNodes;
    private List<Node> nonRecordingActionNodes;

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
        } catch (IOException e) {
            throw new RuntimeException("Unable to load FXML file", e);
        }
    }

    private void initEventsSubscriptions() {
        if (speechShortcutTriggeredEventConsumer != null) {
            DefaultEventBus.getInstance().subscribe(
                    SpeechShortcutTriggeredEvent.class,
                    speechShortcutTriggeredEventConsumer
            );
        }

        DefaultEventBus.getInstance().subscribe(
                FocusShortcutTriggeredEvent.class,
                focusShortcutTriggeredEventConsumer
        );

        if (chatMessageStreamEventConsumer != null) {
            DefaultEventBus.getInstance().subscribe(
                    ChatMessageStreamEvent.class,
                    chatMessageStreamEventConsumer
            );
        }
    }

    public void unsubscribeEvents() {
        if (speechShortcutTriggeredEventConsumer != null) {
            DefaultEventBus.getInstance().unsubscribe(
                    SpeechShortcutTriggeredEvent.class,
                    speechShortcutTriggeredEventConsumer
            );
        }

        DefaultEventBus.getInstance().unsubscribe(
                FocusShortcutTriggeredEvent.class,
                focusShortcutTriggeredEventConsumer
        );

        if (chatMessageStreamEventConsumer != null) {
            DefaultEventBus.getInstance().unsubscribe(
                    ChatMessageStreamEvent.class,
                    chatMessageStreamEventConsumer
            );
        }
    }

    @FXML
    public void initialize() {
        statusPrompt = new ChatStatusPrompt(chatTextArea);
        browser = new Browser();
        chatAnswersPane.getChildren().add(browser);

        actionNodes = List.of(sendButton, recordButton, chatTextArea);
        nonRecordingActionNodes = actionNodes.stream()
                .filter(it -> it != recordButton)
                .toList();

        messages.forEach(this::displayMessage);
        boolean hasAnyUserMessages = messages.stream().anyMatch(m -> m.seder() == ChatMessageRole.USER);

        streamingController = new ChatStreamingController(
                browser,
                streamControlButton,
                actionNodes,
                chatTextArea,
                statusPrompt,
                chatThread.id(),
                hasAnyUserMessages
        );
        streamingController.initialize();

        recordingController = new ChatRecordingController(
                recordButton,
                chatTextArea,
                actionNodes,
                nonRecordingActionNodes,
                new AudioRecorder(),
                statusPrompt,
                this::handleTranscriptionReady,
                this::displaySpeechError
        );
        recordingController.bind();

        initializeInputHandler();
        setCurrentCommand(null);
        bindInputHeightToButtonsBox();

        speechShortcutTriggeredEventConsumer = event -> triggerSpeechAction();
        chatMessageStreamEventConsumer = streamingController::handleStreamEvent;
        initEventsSubscriptions();
    }

    private void bindInputHeightToButtonsBox() {
        if (inputButtonsBox == null) {
            return;
        }
        chatTextArea.minHeightProperty().bind(inputButtonsBox.heightProperty());
        chatTextArea.prefHeightProperty().bind(inputButtonsBox.heightProperty());
    }

    private void triggerSpeechAction() {
        if (recordingController != null) {
            recordingController.toggleRecording();
        }
    }

    private void triggerFocusAction() {
        final Window window = chatTextArea.getScene().getWindow();
        if (window instanceof Stage stage) {
            FxThreadRunner.run(() -> {
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

    private void handleError(String message) {
        logger.error(message);
        displayMessage(message, ASSISTANT, ChatMessageStyleType.DANGER);
        FxThreadRunner.run(() -> actionNodes.forEach(node -> node.setDisable(false)));
    }

    private void handleTranscriptionReady(String message) {
        displayMessage(message, ChatMessageRole.USER, ChatMessageStyleType.NONE);
        streamingController.markUserMessageSent();
        sendMessageToService(message);
    }

    private void displaySpeechError(String message) {
        displayMessage(message, ASSISTANT, ChatMessageStyleType.DANGER);
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
        if (message.isBlank()) {
            return;
        }
        chatTextArea.clear();
        displayMessage(message, ChatMessageRole.USER, ChatMessageStyleType.NONE);
        streamingController.markUserMessageSent();
        prepareContextAndSendMessageToService(message);
    }

    public void sendMessage(final String message, final String commandId, final OnMessageCompleteCallback onComplete) {
        displayMessage(message, ChatMessageRole.USER, ChatMessageStyleType.NONE);
        streamingController.markUserMessageSent();
        prepareContextAndSendMessageToService(message, commandId, onComplete);
    }

    private void displayMessage(final ChatMessageResponseModel message) {
        displayMessage(message.content(), message.seder(), message.styleType());
    }

    private void displayMessage(final String text,
                                final ChatMessageRole chatMessageRole,
                                final ChatMessageStyleType chatMessageStyleType) {
        FxThreadRunner.run(() -> browser.addContent(text, chatMessageRole, chatMessageStyleType));
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
                streamingController.beginStreaming(false);
                final ChatMessageSendModel chatMessageSendModel = new ChatMessageSendModel(message, chatThread.id(), commandId, onComplete);

                BackendApi.sendChatMessagesStream(chatMessageSendModel);
            } catch (Exception e) {
                logger.error("Error starting streaming message", e);
                final ChatMessageResponseModel errorResponse = handelExceptionAsMessage(message, e, e.getMessage());
                displayMessage(errorResponse);

                streamingController.handleStartError();
            }
        });
    }

    @FXML
    public void streamControlFromUi() {
        if (streamingController != null) {
            streamingController.streamControlFromUi();
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
            statusPrompt.setBasePromptText(LanguageManager.getLanguageString("chat.message.type.noCommand"));
            commandLabel.setText(LanguageManager.getLanguageString("chat.command.noCommand"));
            commandLabel.setGraphic(null);
        } else {
            statusPrompt.setBasePromptText(LanguageManager.getLanguageString("chat.message.type.command", currentCommand.description()));
            commandLabel.setText(LanguageManager.getLanguageString("chat.command.current", currentCommand.name()));
            commandLabel.setGraphic(new FontIcon(currentCommand.icon()));
        }

    }

}
