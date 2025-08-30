package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.event.ChatMessageReceivedEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.thread.ChatThreadResponse;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.messagecontext.MessageContextException;
import com.patres.alina.uidesktop.messagecontext.MessageWithContextGenerator;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.common.event.shortcut.FocusShortcutTriggeredEvent;
import com.patres.alina.uidesktop.common.event.shortcut.SpeechShortcutTriggeredEvent;
import com.patres.alina.uidesktop.microphone.AudioRecorder;
import com.patres.alina.uidesktop.plugin.SearchPluginPopup;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import feign.FeignException;
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

    private final ChatThreadResponse chatThread;
    private final List<ChatMessageResponseModel> messages;
    private final ApplicationWindow applicationWindow;

    private final Consumer<SpeechShortcutTriggeredEvent> speechShortcutTriggeredEventConsumer = event -> triggerSpeechAction();
    private final Consumer<FocusShortcutTriggeredEvent> focusShortcutTriggeredEventConsumer = event -> triggerFocusAction();
    private final Consumer<ChatMessageReceivedEvent> chatMessageReceivedEventConsumer = event -> handleChatMessageReceivedEvent(event);



    private SearchPluginPopup popup;
    private CardListItem currentPlugin;

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
    private Label pluginLabel;

    @FXML
    private Label chatInfoLabel;

    private Browser browser;
    private RecordMode recordMode = RecordMode.PREPARE_TO_START_RECORDING;
    private final AudioRecorder audioRecorder = new AudioRecorder();

    private final List<Node> actionNodes;
    private final List<Node> nomRecordingActionNodes;

    public ChatWindow(ChatThreadResponse chatThread, ApplicationWindow applicationWindow) {
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
    }

    @FXML
    public void initialize() {
        browser = new Browser();
        chatAnswersPane.getChildren().add(browser);

        messages.forEach(this::displayMessage);
        initializeInputHandler();
        setCurrentPlugin(null);

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
        Platform.runLater(() -> browser.showLoaderForUserMessage());
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
        popup = new SearchPluginPopup(chatTextArea);
        chatTextArea.textProperty().addListener(
                (observableValue, oldValue, newValue) -> popup.handleTextChangeListener(newValue, applicationWindow.getStage())
        );
        popup.getSelectedPluginProperty().addListener((observable, oldValue, newValue) -> {
            setCurrentPlugin(newValue);
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
                Platform.runLater(() -> browser.showLoader());
                actionNodes.forEach(node -> node.setDisable(true));
                chatTextArea.setText(LanguageManager.getLanguageString("chat.message.sending"));

                final ChatMessageSendModel chatMessageSendModel = new ChatMessageSendModel(message, chatThread.id(), getCurrentPluginId());
                final ChatMessageResponseModel chatResponse = getChatResponse(chatMessageSendModel);
                displayMessage(chatResponse);

                actionNodes.forEach(node -> node.setDisable(false));
                chatTextArea.clear();

                Platform.runLater(() -> {
                    chatTextArea.requestFocus();
                });
            } finally {
                Platform.runLater(() -> browser.hideLoader());
            }
        });
    }


    private void handleChatMessageReceivedEvent(ChatMessageReceivedEvent event) {
        actionNodes.forEach(node -> node.setDisable(false));
        chatTextArea.clear();

        Platform.runLater(() -> {
            chatTextArea.requestFocus();
        });

        Platform.runLater(() -> browser.hideLoader());

    }

    private String getCurrentPluginId() {
        return currentPlugin == null ? null : currentPlugin.id();
    }

    private static ChatMessageResponseModel getChatResponse(final ChatMessageSendModel chatMessageSendModel) {
        try {
            return BackendApi.sendChatMessages(chatMessageSendModel);
        } catch (Exception e) {
            return handelExceptionAsMessage(chatMessageSendModel.content(), e, e.getMessage());
        }
    }

    private static ChatMessageResponseModel handelExceptionAsMessage(final String content, final Exception e, final String message) {
        logger.error("Cannot send a message `{}` to server", content, e);
        final String errorMessage = LanguageManager.getLanguageString("chat.message.error");
        return new ChatMessageResponseModel(errorMessage + ": " + message, ASSISTANT, LocalDateTime.now(), ChatMessageStyleType.DANGER, null);
    }

    public ChatThreadResponse getChatThread() {
        return chatThread;
    }

    public Label getPluginLabel() {
        return pluginLabel;
    }

    private void setCurrentPlugin(final CardListItem currentPlugin) {
        this.currentPlugin = currentPlugin;
        if (currentPlugin == null) {
            chatTextArea.setPromptText(LanguageManager.getLanguageString("chat.message.type.noPlugin"));
            pluginLabel.setText(LanguageManager.getLanguageString("chat.plugin.noPlugin"));
            pluginLabel.setGraphic(null);
        } else {
            chatTextArea.setPromptText(LanguageManager.getLanguageString("chat.message.type.plugin", currentPlugin.description()));
            pluginLabel.setText(LanguageManager.getLanguageString("chat.plugin.current", currentPlugin.name()));
            pluginLabel.setGraphic(new FontIcon(currentPlugin.icon()));
        }

    }

    public TextArea getChatTextArea() {
        return chatTextArea;
    }


}
