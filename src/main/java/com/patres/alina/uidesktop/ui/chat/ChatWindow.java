package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.event.ChatNotificationEvent;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.message.CommandUsageInfo;
import com.patres.alina.common.message.OnMessageCompleteCallback;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.command.Command;
import com.patres.alina.server.command.CommandConstants;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.messagecontext.MessageContextException;
import com.patres.alina.uidesktop.messagecontext.MessageWithContextGenerator;
import com.patres.alina.uidesktop.quickaction.QuickActionType;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.common.event.shortcut.FocusShortcutTriggeredEvent;
import com.patres.alina.uidesktop.command.SearchCommandPopup;
import com.patres.alina.uidesktop.settings.UiSettings;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.uidesktop.ui.language.ApplicationLanguage;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.theme.SamplerTheme;
import com.patres.alina.uidesktop.ui.theme.ThemeManager;
import com.patres.alina.uidesktop.ui.util.FxThreadRunner;
import com.patres.alina.uidesktop.ui.util.NotificationSoundPlayer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
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
import java.util.Objects;
import java.util.function.Consumer;

import static com.patres.alina.common.message.ChatMessageRole.ASSISTANT;
import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;


public class ChatWindow extends BorderPane {

    private static final Logger logger = LoggerFactory.getLogger(ChatWindow.class);
    private final ChatThread chatThread;
    private final List<ChatMessageResponseModel> messages;
    private final ApplicationWindow applicationWindow;

    private final Consumer<FocusShortcutTriggeredEvent> focusShortcutTriggeredEventConsumer = event -> triggerFocusAction();
    private Consumer<ChatMessageStreamEvent> chatMessageStreamEventConsumer;
    private final Consumer<ChatNotificationEvent> chatNotificationEventConsumer = this::handleChatNotification;

    private SearchCommandPopup popup;
    private CardListItem currentCommand;
    private volatile Command currentCommandDetails;
    private ChatInputMode inputMode = ChatInputMode.CHAT;
    private volatile String selectedModel;
    private boolean settingFromHistory = false;

    @FXML
    private StackPane chatAnswersPane;

    @FXML
    private VBox footerPane;

    @FXML
    private StackPane composerPane;

    @FXML
    private HBox messageInputRow;

    @FXML
    private Button sendButton;

    @FXML
    private Button streamControlButton;

    @FXML
    private TextArea chatTextArea;

    @FXML
    private VBox inputButtonsBox;

    @FXML
    private VBox permissionComposerPane;

    @FXML
    private Label permissionComposerTitleLabel;

    @FXML
    private Label permissionComposerMessageLabel;

    @FXML
    private Label permissionComposerStatusLabel;

    @FXML
    private Button permissionApproveButton;

    @FXML
    private Button permissionApproveAlwaysButton;

    @FXML
    private Button permissionDenyButton;

    @FXML
    private HBox statusBar;

    @FXML
    private Label commandLabel;

    @FXML
    private Label modelLabel;

    private ContextMenu modelMenu;

    private Browser browser;
    private ChatStatusPrompt statusPrompt;
    private ChatStreamingController streamingController;

    private List<Node> actionNodes;

    public ChatWindow(ChatThread chatThread, ApplicationWindow applicationWindow) {
        this(chatThread, applicationWindow, BackendApi.getMessagesByThreadId(chatThread.id()));
    }

    public ChatWindow(ChatThread chatThread, ApplicationWindow applicationWindow, List<ChatMessageResponseModel> prefetchedMessages) {
        super();
        this.chatThread = chatThread;
        this.applicationWindow = applicationWindow;
        this.messages = prefetchedMessages;

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

        DefaultEventBus.getInstance().subscribe(
                ChatNotificationEvent.class,
                chatNotificationEventConsumer
        );
    }

    public void focusTextArea() {
        chatTextArea.requestFocus();
    }

    public void unsubscribeEvents() {
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

        DefaultEventBus.getInstance().unsubscribe(
                ChatNotificationEvent.class,
                chatNotificationEventConsumer
        );

        if (browser != null) {
            browser.dispose();
        }
    }

    @FXML
    public void initialize() {
        statusPrompt = new ChatStatusPrompt(chatTextArea);
        browser = new Browser();
        chatAnswersPane.getChildren().add(browser);

        actionNodes = List.of(sendButton);

        browser.whenReady(() -> messages.forEach(this::displayMessage));
        boolean hasAnyUserMessages = messages.stream().anyMatch(m -> m.sender() == ChatMessageRole.USER);

        streamingController = new ChatStreamingController(
                browser,
                streamControlButton,
                actionNodes,
                chatTextArea,
                messageInputRow,
                permissionComposerPane,
                permissionComposerTitleLabel,
                permissionComposerMessageLabel,
                permissionComposerStatusLabel,
                permissionApproveButton,
                permissionApproveAlwaysButton,
                permissionDenyButton,
                statusPrompt,
                chatThread.id(),
                hasAnyUserMessages
        );
        streamingController.initialize();

        initializeInputHandler();
        initModelSelector();
        setCurrentCommand(null);
        bindInputHeightToButtonsBox();
        initLanguageListener();

        chatMessageStreamEventConsumer = streamingController::handleStreamEvent;
        initEventsSubscriptions();
    }

    private void initModelSelector() {
        if (modelLabel == null) return;

        modelMenu = new ContextMenu();

        // Initialize with the global default model; each tab tracks its own from here on
        Thread.startVirtualThread(() -> {
            final List<String> models = BackendApi.getChatModels();
            final AssistantSettings settings = BackendApi.getAssistantSettings();
            final String defaultModel = settings.resolveModelIdentifier();
            selectedModel = defaultModel;
            FxThreadRunner.run(() -> {
                modelLabel.setText(defaultModel);
                populateModelMenu(models);
            });
        });

        modelLabel.setOnMouseClicked(_ -> {
            if (modelMenu.isShowing()) {
                modelMenu.hide();
            } else {
                modelMenu.show(modelLabel, javafx.geometry.Side.TOP, 0, 0);
            }
        });
    }

    private void populateModelMenu(final List<String> models) {
        modelMenu.getItems().clear();
        for (String model : models) {
            MenuItem item = new MenuItem(model);
            item.setOnAction(_ -> {
                selectedModel = model;
                modelLabel.setText(model);
            });
            modelMenu.getItems().add(item);
        }
    }

    private void bindInputHeightToButtonsBox() {
        if (inputButtonsBox == null) {
            return;
        }
        chatTextArea.minHeightProperty().bind(inputButtonsBox.heightProperty());
        chatTextArea.prefHeightProperty().bind(inputButtonsBox.heightProperty());

        if (statusBar != null) {
            inputButtonsBox.widthProperty().addListener((_, _, newVal) ->
                    statusBar.setPadding(new Insets(0, newVal.doubleValue(), 0, 0))
            );
        }
    }

    private void initLanguageListener() {
        LanguageManager.localeProperty().addListener((_, _, _) ->
                FxThreadRunner.run(() -> setCurrentCommand(currentCommand))
        );
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

    private void initializeInputHandler() {
        popup = new SearchCommandPopup(chatTextArea);
        chatTextArea.textProperty().addListener(
                (observableValue, oldValue, newValue) -> popup.handleTextChangeListener(newValue, applicationWindow.getStage())
        );
        chatTextArea.textProperty().addListener((observableValue, oldValue, newValue) -> {
            if (!settingFromHistory) {
                PromptHistoryManager.getInstance().resetNavigation();
            }
        });
        popup.getSelectedCommandProperty().addListener((observable, oldValue, newValue) -> {
            setCurrentCommand(newValue);
        });
        popup.getSelectedQuickActionProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                executeQuickAction(newValue);
            }
        });
    }


    @FXML
    public void chatTextAreaOnKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ESCAPE && inputMode != ChatInputMode.CHAT) {
            exitInputMode();
            event.consume();
            return;
        }
        if (event.getCode() == KeyCode.UP && !popup.isShowing()) {
            PromptHistoryManager historyManager = PromptHistoryManager.getInstance();
            if (chatTextArea.getText().isEmpty() || historyManager.isNavigating()) {
                String prompt = historyManager.navigateUp(chatTextArea.getText());
                if (prompt != null) {
                    settingFromHistory = true;
                    chatTextArea.setText(prompt);
                    chatTextArea.positionCaret(prompt.length());
                    settingFromHistory = false;
                }
                event.consume();
                return;
            }
        }
        if (event.getCode() == KeyCode.DOWN && !popup.isShowing()) {
            PromptHistoryManager historyManager = PromptHistoryManager.getInstance();
            if (historyManager.isNavigating()) {
                String prompt = historyManager.navigateDown();
                if (prompt != null) {
                    settingFromHistory = true;
                    chatTextArea.setText(prompt);
                    chatTextArea.positionCaret(prompt.length());
                    settingFromHistory = false;
                }
                event.consume();
                return;
            }
        }
        if (event.getCode() == KeyCode.ENTER) {
            if (event.isShiftDown()) {
                chatTextArea.insertText(chatTextArea.getCaretPosition(), System.lineSeparator());
            } else if (inputMode == ChatInputMode.ADD_TASK) {
                submitAddTask();
                event.consume();
            } else if (!chatTextArea.getText().isBlank() || currentCommand != null) {
                sendMessageFromUi();
            }
        }
    }

    @FXML
    public void sendMessageFromUi() {
        final String message = chatTextArea.getText().trim();
        if (message.isBlank() && currentCommand == null) {
            return;
        }
        PromptHistoryManager.getInstance().addPrompt(message);
        final String commandId = getCurrentCommandId();
        chatTextArea.clear();
        setCurrentCommand(null);
        PreparedMessage prepared = prepareMessageToSend(message, commandId);
        final String displayText = resolveDisplayText(message, prepared.commandUsageInfo());
        displayMessage(displayText, ChatMessageRole.USER, ChatMessageStyleType.NONE, prepared.commandUsageInfo());
        streamingController.markUserMessageSent();
        sendMessageToService(prepared.messageToSend(), commandId);
    }

    public void sendMessage(final String message, final String commandId, final OnMessageCompleteCallback onComplete) {
        PreparedMessage prepared = prepareMessageToSend(message, commandId);
        final String displayText = resolveDisplayText(message, prepared.commandUsageInfo());
        displayMessage(displayText, ChatMessageRole.USER, ChatMessageStyleType.NONE, prepared.commandUsageInfo());
        streamingController.markUserMessageSent();
        sendMessageToService(prepared.messageToSend(), commandId, onComplete);
    }

    /**
     * Sends a message using a specific model override.
     * If modelOverride is non-null, it temporarily overrides this tab's selectedModel for the request.
     */
    public void sendMessageWithModel(final String message, final String commandId, final OnMessageCompleteCallback onComplete, final String modelOverride) {
        if (modelOverride != null && !modelOverride.isBlank()) {
            final String previousModel = selectedModel;
            selectedModel = modelOverride;
            sendMessage(message, commandId, (aiResponse) -> {
                selectedModel = previousModel;
                if (onComplete != null) onComplete.onComplete(aiResponse);
            });
        } else {
            sendMessage(message, commandId, onComplete);
        }
    }

    private void displayMessage(final ChatMessageResponseModel message) {
        displayMessage(message.content(), message.sender(), message.styleType(), message.commandUsageInfo());
    }

    private void displayMessage(final String text,
                                final ChatMessageRole chatMessageRole,
                                final ChatMessageStyleType chatMessageStyleType) {
        displayMessage(text, chatMessageRole, chatMessageStyleType, null);
    }

    private String resolveDisplayText(final String message, final CommandUsageInfo commandUsageInfo) {
        if (!message.isBlank()) {
            return message;
        }
        if (commandUsageInfo != null && commandUsageInfo.commandName() != null && !commandUsageInfo.commandName().isBlank()) {
            return commandUsageInfo.commandName();
        }
        return message;
    }

    private void displayMessage(final String text,
                                final ChatMessageRole chatMessageRole,
                                final ChatMessageStyleType chatMessageStyleType,
                                final CommandUsageInfo commandUsageInfo) {
        FxThreadRunner.run(() -> browser.addContent(text, chatMessageRole, chatMessageStyleType, commandUsageInfo));
    }

    private void handleChatNotification(final ChatNotificationEvent event) {
        if (!applicationWindow.isActiveTab(chatThread.id())) {
            return;
        }
        NotificationSoundPlayer.playIfEnabled();
        displayMessage(event.getMessage(), ASSISTANT, event.getStyleType());
    }

    private void sendMessageToService(final String message, final String commandId) {
        sendMessageToService(message, commandId, null);
    }

    private void sendMessageToService(final String message, final String commandId, final OnMessageCompleteCallback onComplete) {
        Thread.startVirtualThread(() -> {
            try {
                final boolean backgroundMode = onComplete != null;
                streamingController.beginStreaming(false, backgroundMode);
                final ChatMessageSendModel chatMessageSendModel = new ChatMessageSendModel(
                        message, chatThread.id(), commandId, ChatMessageStyleType.NONE, onComplete, selectedModel
                );

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
        return new ChatMessageResponseModel(errorMessage + ": " + message, ASSISTANT, LocalDateTime.now(), ChatMessageStyleType.DANGER, null, null);
    }

    public ChatThread getChatThread() {
        return chatThread;
    }

    private PreparedMessage prepareMessageToSend(final String message, final String commandId) {
        String messageToSend = message;
        try {
            final MessageWithContextGenerator messageWithContextGenerator = new MessageWithContextGenerator(message);
            messageToSend = messageWithContextGenerator.replacePathsWithContents();
        } catch (MessageContextException e) {
            logger.error("Cannot generate context for message '{}', sending original message", message, e);
            handleError(e.getMessage());
        }
        CommandUsageInfo commandUsageInfo = buildCommandUsageInfo(commandId, messageToSend);
        return new PreparedMessage(messageToSend, commandUsageInfo);
    }

    private CommandUsageInfo buildCommandUsageInfo(final String commandId, final String messageToSend) {
        if (commandId == null || commandId.isBlank()) {
            return null;
        }
        final Command command = resolveCommandDetails(commandId);
        if (command == null) {
            return null;
        }
        final String prompt = buildPromptWithCommand(command.systemPrompt(), messageToSend);
        return new CommandUsageInfo(command.id(), command.name(), command.icon(), prompt);
    }

    private Command resolveCommandDetails(final String commandId) {
        if (currentCommandDetails != null && Objects.equals(commandId, currentCommandDetails.id())) {
            return currentCommandDetails;
        }
        try {
            return BackendApi.getCommand(commandId);
        } catch (Exception e) {
            logger.warn("Cannot load command details for id {}", commandId, e);
            return null;
        }
    }

    private String buildPromptWithCommand(final String systemPrompt, final String message) {
        final String commandContent = systemPrompt == null ? "" : systemPrompt.trim();
        if (commandContent.isEmpty()) {
            return message;
        }
        if (commandContent.contains(CommandConstants.ARGUMENTS_PLACEHOLDER)) {
            return commandContent.replace(CommandConstants.ARGUMENTS_PLACEHOLDER, message);
        }
        return commandContent + System.lineSeparator() + message;
    }

    private void executeQuickAction(QuickActionType actionType) {
        FxThreadRunner.run(() -> {
            switch (actionType) {
                case CLEAR_CHAT -> applicationWindow.clearCurrentChatThread();
                case NEW_CHAT -> applicationWindow.createNewChatThread();
                case MODELS -> {
                    if (modelMenu != null) {
                        Thread.startVirtualThread(() -> {
                            final List<String> models = BackendApi.getChatModels();
                            FxThreadRunner.run(() -> {
                                populateModelMenu(models);
                                modelMenu.show(modelLabel, javafx.geometry.Side.TOP, 0, 0);
                            });
                        });
                    }
                }
                case THEME -> showThemeMenu();
                case LANGUAGE -> showLanguageMenu();
                case HISTORY -> applicationWindow.openThreadHistories();
                case COMMANDS -> applicationWindow.openCommands();
                case UI_SETTINGS -> applicationWindow.openUiSettings();
                case ASSISTANT_SETTINGS -> applicationWindow.openOpenCodeSettings();
                case DASHBOARD_SETTINGS -> applicationWindow.openDashboardSettings();
                case OPENCODE_SETTINGS -> applicationWindow.openOpenCodeSettings();
                case ADD_TASK -> enterAddTaskMode();
            }
        });
    }

    private void enterAddTaskMode() {
        inputMode = ChatInputMode.ADD_TASK;
        statusPrompt.setBasePromptText(LanguageManager.getLanguageString("quickaction.addTask.prompt"));
        commandLabel.setText(LanguageManager.getLanguageString("quickaction.addTask.title"));
        commandLabel.setGraphic(new FontIcon("mdal-add_task"));
        chatTextArea.requestFocus();
    }

    private void exitInputMode() {
        inputMode = ChatInputMode.CHAT;
        chatTextArea.clear();
        setCurrentCommand(currentCommand);
    }

    private void submitAddTask() {
        final String taskContent = chatTextArea.getText().trim();
        if (taskContent.isBlank()) {
            return;
        }
        chatTextArea.clear();
        inputMode = ChatInputMode.CHAT;
        setCurrentCommand(currentCommand);
        Thread.startVirtualThread(() -> {
            try {
                BackendApi.addDashboardTask(taskContent);
            } catch (Exception e) {
                logger.error("Cannot add dashboard task", e);
            }
        });
    }

    private void showThemeMenu() {
        ThemeManager tm = ThemeManager.getInstance();
        ContextMenu themeMenu = new ContextMenu();
        List<SamplerTheme> themes = tm.getRepository().getAll();
        SamplerTheme currentTheme = tm.getTheme();

        for (SamplerTheme theme : themes) {
            MenuItem item = new MenuItem(theme.getName());
            if (currentTheme != null && theme.getName().equals(currentTheme.getName())) {
                item.setGraphic(new FontIcon("mdal-check"));
            }
            item.setOnAction(_ -> {
                tm.setTheme(theme);
                saveThemeToUiSettings(theme.getName());
            });
            themeMenu.getItems().add(item);
        }

        themeMenu.show(chatTextArea, javafx.geometry.Side.TOP, 0, 0);
    }

    private void showLanguageMenu() {
        ContextMenu languageMenu = new ContextMenu();
        UiSettings currentSettings = UI_SETTINGS.getSettings();
        String currentLang = currentSettings.language();

        for (ApplicationLanguage lang : ApplicationLanguage.values()) {
            MenuItem item = new MenuItem(lang.getLanguageName());
            if (lang.getLocale().getLanguage().equals(currentLang)) {
                item.setGraphic(new FontIcon("mdal-check"));
            }
            item.setOnAction(_ -> {
                LanguageManager.setLanguage(lang);
                saveLanguageToUiSettings(lang.getLocale().getLanguage());
            });
            languageMenu.getItems().add(item);
        }

        languageMenu.show(chatTextArea, javafx.geometry.Side.TOP, 0, 0);
    }

    private void saveThemeToUiSettings(String themeName) {
        Thread.startVirtualThread(() -> {
            UiSettings current = UI_SETTINGS.getSettings();
            UiSettings updated = new UiSettings(
                    themeName,
                    current.language(),
                    current.shortcutKeysSettings(),
                    current.soundNotificationEnabled(),
                    current.notificationSoundType(),
                    current.showExpandButton(),
                    current.expandWidth(),
                    current.autoSplitOnExpand()
            );
            UI_SETTINGS.saveDocument(updated);
        });
    }

    private void saveLanguageToUiSettings(String language) {
        Thread.startVirtualThread(() -> {
            UiSettings current = UI_SETTINGS.getSettings();
            UiSettings updated = new UiSettings(
                    current.theme(),
                    language,
                    current.shortcutKeysSettings(),
                    current.soundNotificationEnabled(),
                    current.notificationSoundType(),
                    current.showExpandButton(),
                    current.expandWidth(),
                    current.autoSplitOnExpand()
                    );
            UI_SETTINGS.saveDocument(updated);
        });
    }

    private void setCurrentCommand(final CardListItem currentCommand) {
        this.currentCommand = currentCommand;
        currentCommandDetails = null;
        if (currentCommand == null) {
            statusPrompt.setBasePromptText(LanguageManager.getLanguageString("chat.message.type.noCommand"));
            commandLabel.setText(LanguageManager.getLanguageString("chat.command.noCommand"));
            commandLabel.setGraphic(null);
        } else {
            statusPrompt.setBasePromptText(LanguageManager.getLanguageString("chat.message.type.command", currentCommand.description()));
            commandLabel.setText(LanguageManager.getLanguageString("chat.command.current", currentCommand.name()));
            commandLabel.setGraphic(new FontIcon(currentCommand.icon()));
            Thread.startVirtualThread(() -> {
                try {
                    Command command = BackendApi.getCommand(currentCommand.id());
                    FxThreadRunner.run(() -> {
                        if (this.currentCommand != null && Objects.equals(currentCommand.id(), this.currentCommand.id())) {
                            currentCommandDetails = command;
                        }
                    });
                } catch (Exception e) {
                    logger.warn("Cannot load command details for id {}", currentCommand.id(), e);
                }
            });
        }

    }

    private record PreparedMessage(
            String messageToSend,
            CommandUsageInfo commandUsageInfo
    ) {
    }

}
