package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.event.CalendarAiPromptEvent;
import com.patres.alina.common.event.ChatNotificationEvent;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.message.CommandUsageInfo;
import com.patres.alina.common.message.ImageAttachment;
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
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

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
    private final Consumer<CalendarAiPromptEvent> calendarAiPromptEventConsumer = this::handleCalendarAiPrompt;

    private SearchCommandPopup popup;
    private CardListItem currentCommand;
    private volatile Command currentCommandDetails;
    private ChatInputMode inputMode = ChatInputMode.CHAT;
    private volatile String selectedModel;
    private boolean settingFromHistory = false;
    private final java.util.Map<String, ChatThread> recentThreadCache = new java.util.HashMap<>();
    private final List<ImageAttachment> pendingImages = new ArrayList<>();

    @FXML
    private StackPane chatAnswersPane;

    @FXML
    private FlowPane imageAttachmentPane;

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
    private Button clearChatButton;

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

        DefaultEventBus.getInstance().subscribe(
                CalendarAiPromptEvent.class,
                calendarAiPromptEventConsumer
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

        DefaultEventBus.getInstance().unsubscribe(
                CalendarAiPromptEvent.class,
                calendarAiPromptEventConsumer
        );

        if (browser != null) {
            browser.dispose();
        }
    }

    @FXML
    public void initialize() {
        statusPrompt = new ChatStatusPrompt(chatTextArea);
        browser = new Browser();
        browser.setSuggestionClickHandler(this::handleSuggestionClick);
        browser.setWelcomeActionHandler(new Browser.WelcomeActionHandler() {
            @Override
            public void onSelectCommand(final String commandId) {
                try {
                    final Command command = BackendApi.getCommand(commandId);
                    if (command != null) {
                        final var item = new CardListItem(
                                command.id(),
                                command.name(),
                                command.description(),
                                command.icon(),
                                command.state()
                        );
                        setCurrentCommand(item);
                        chatTextArea.requestFocus();
                    }
                } catch (final Exception e) {
                    logger.warn("Cannot load command from welcome screen: {}", commandId, e);
                }
            }

            @Override
            public void onOpenThread(final String threadId) {
                // Use cached thread to avoid registry lookup issues, load in current tab
                final ChatThread cached = recentThreadCache.get(threadId);
                if (cached != null) {
                    Thread.startVirtualThread(() -> {
                        try {
                            final var msgs = BackendApi.getMessagesByThreadId(threadId);
                            FxThreadRunner.run(() -> applicationWindow.loadChatThreadInActiveTab(cached, msgs));
                        } catch (final Exception e) {
                            logger.warn("Cannot load messages for thread from welcome screen: {}", threadId, e);
                            FxThreadRunner.run(() -> applicationWindow.loadChatThreadInActiveTab(cached, List.of()));
                        }
                    });
                    return;
                }
                // Fallback: fetch via API on virtual thread
                Thread.startVirtualThread(() -> {
                    try {
                        final var optThread = BackendApi.getChatThread(threadId);
                        optThread.ifPresent(thread -> {
                            final var msgs = BackendApi.getMessagesByThreadId(threadId);
                            FxThreadRunner.run(() -> applicationWindow.loadChatThreadInActiveTab(thread, msgs));
                        });
                    } catch (final Exception e) {
                        logger.warn("Cannot open thread from welcome screen: {}", threadId, e);
                    }
                });
            }
        });
        chatAnswersPane.getChildren().add(browser);

        actionNodes = List.of(sendButton);

        browser.whenReady(() -> {
            messages.forEach(this::displayMessage);
            localizeWelcomeScreen();
            populateWelcomeData();
        });
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
        installButtonTooltips();

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
                FxThreadRunner.run(() -> {
                    setCurrentCommand(currentCommand);
                    installButtonTooltips();
                })
        );
    }

    private void installButtonTooltips() {
        Tooltip.install(sendButton, new Tooltip(LanguageManager.getLanguageString("chat.button.send")));
        Tooltip.install(clearChatButton, new Tooltip(LanguageManager.getLanguageString("chat.button.clearChat")));
        streamingController.refreshStreamControlTooltip();
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
        // Handle Ctrl+V / Cmd+V for image paste
        if (event.getCode() == KeyCode.V && (event.isShortcutDown())) {
            final Clipboard clipboard = Clipboard.getSystemClipboard();
            if (clipboard.hasImage()) {
                handleClipboardImagePaste(clipboard.getImage());
                event.consume();
                return;
            }
            // If no image, let the default text paste happen
        }

        if (event.getCode() == KeyCode.ESCAPE) {
            if (inputMode != ChatInputMode.CHAT) {
                exitInputMode();
                event.consume();
                return;
            } else if (currentCommand != null) {
                setCurrentCommand(null);
                event.consume();
                return;
            }
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
        if (message.isBlank() && currentCommand == null && pendingImages.isEmpty()) {
            return;
        }
        PromptHistoryManager.getInstance().addPrompt(message);
        final String commandId = getCurrentCommandId();
        final List<ImageAttachment> images = takeAndClearPendingImages();
        chatTextArea.clear();
        setCurrentCommand(null);
        PreparedMessage prepared = prepareMessageToSend(message, commandId);
        final String displayText = resolveDisplayText(message, prepared.commandUsageInfo());
        displayMessageWithImages(displayText, ChatMessageRole.USER, ChatMessageStyleType.NONE, images);
        streamingController.markUserMessageSent();
        sendMessageToService(prepared.messageToSend(), commandId, images);
    }

    public void sendMessage(final String message, final String commandId, final OnMessageCompleteCallback onComplete) {
        PreparedMessage prepared = prepareMessageToSend(message, commandId);
        final String displayText = resolveDisplayText(message, prepared.commandUsageInfo());
        displayMessage(displayText, ChatMessageRole.USER, ChatMessageStyleType.NONE);
        streamingController.markUserMessageSent();
        sendMessageToService(prepared.messageToSend(), commandId, onComplete, List.of());
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
        final String displayText = message.commandUsageInfo() != null
                && message.commandUsageInfo().prompt() != null
                && !message.commandUsageInfo().prompt().isBlank()
                ? message.commandUsageInfo().prompt()
                : message.content();
        displayMessage(displayText, message.sender(), message.styleType());
    }

    private String resolveDisplayText(final String message, final CommandUsageInfo commandUsageInfo) {
        if (commandUsageInfo != null && commandUsageInfo.prompt() != null && !commandUsageInfo.prompt().isBlank()) {
            return commandUsageInfo.prompt();
        }
        return message;
    }

    private void displayMessage(final String text,
                                final ChatMessageRole chatMessageRole,
                                final ChatMessageStyleType chatMessageStyleType) {
        FxThreadRunner.run(() -> browser.addContent(text, chatMessageRole, chatMessageStyleType));
    }

    private void displayMessageWithImages(final String text,
                                           final ChatMessageRole chatMessageRole,
                                           final ChatMessageStyleType chatMessageStyleType,
                                           final List<ImageAttachment> images) {
        FxThreadRunner.run(() -> browser.addContentWithImages(text, chatMessageRole, chatMessageStyleType, images));
    }

    private void handleChatNotification(final ChatNotificationEvent event) {
        if (!applicationWindow.isActiveTab(chatThread.id())) {
            return;
        }
        NotificationSoundPlayer.playIfEnabled();
        displayMessage(event.getMessage(), ASSISTANT, event.getStyleType());
    }

    private void handleCalendarAiPrompt(final CalendarAiPromptEvent event) {
        if (!applicationWindow.isActiveTab(chatThread.id())) {
            return;
        }
        FxThreadRunner.run(() -> sendMessage(event.getMessage(), null, null));
    }

    private void sendMessageToService(final String message, final String commandId) {
        sendMessageToService(message, commandId, null, List.of());
    }

    private void sendMessageToService(final String message, final String commandId, final List<ImageAttachment> images) {
        sendMessageToService(message, commandId, null, images);
    }

    private void sendMessageToService(final String message, final String commandId, final OnMessageCompleteCallback onComplete, final List<ImageAttachment> images) {
        Thread.startVirtualThread(() -> {
            try {
                final boolean backgroundMode = onComplete != null;
                streamingController.beginStreaming(false, backgroundMode);
                final ChatMessageSendModel chatMessageSendModel = new ChatMessageSendModel(
                        message, chatThread.id(), commandId, ChatMessageStyleType.NONE, onComplete, selectedModel,
                        images != null ? images : List.of()
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

    @FXML
    public void clearChatFromUi() {
        applicationWindow.clearCurrentChatThread();
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

    // ═══════════════════════════════════════════
    // Image paste support
    // ═══════════════════════════════════════════

    private void handleClipboardImagePaste(final Image fxImage) {
        if (fxImage == null) {
            return;
        }
        Thread.startVirtualThread(() -> {
            try {
                final String base64 = convertFxImageToBase64(fxImage);
                if (base64 == null || base64.isBlank()) {
                    return;
                }
                final ImageAttachment attachment = new ImageAttachment(base64);
                FxThreadRunner.run(() -> addImageAttachment(attachment, fxImage));
            } catch (Exception e) {
                logger.error("Cannot process pasted image", e);
            }
        });
    }

    private String convertFxImageToBase64(final Image fxImage) {
        try {
            final java.awt.image.BufferedImage bufferedImage = SwingFXUtils.fromFXImage(fxImage, null);
            if (bufferedImage == null) {
                return null;
            }
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            logger.error("Cannot convert image to Base64", e);
            return null;
        }
    }

    private void addImageAttachment(final ImageAttachment attachment, final Image fxImage) {
        pendingImages.add(attachment);
        updateImageAttachmentPane();

        final StackPane thumbnailContainer = createImageThumbnail(attachment, fxImage);
        imageAttachmentPane.getChildren().add(thumbnailContainer);
    }

    private StackPane createImageThumbnail(final ImageAttachment attachment, final Image fxImage) {
        final ImageView imageView = new ImageView(fxImage);
        imageView.setFitWidth(64);
        imageView.setFitHeight(64);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.getStyleClass().add("image-attachment-thumbnail");

        final Button removeButton = new Button();
        removeButton.setGraphic(new FontIcon("mdal-close"));
        removeButton.getStyleClass().addAll("image-attachment-remove-button", "flat");
        removeButton.setMaxSize(18, 18);
        removeButton.setMinSize(18, 18);

        final StackPane container = new StackPane(imageView, removeButton);
        container.getStyleClass().add("image-attachment-container");
        container.setMaxSize(72, 72);
        container.setMinSize(72, 72);
        StackPane.setAlignment(removeButton, Pos.TOP_RIGHT);

        removeButton.setOnAction(_ -> {
            pendingImages.remove(attachment);
            imageAttachmentPane.getChildren().remove(container);
            updateImageAttachmentPane();
        });

        return container;
    }

    private void updateImageAttachmentPane() {
        final boolean hasImages = !pendingImages.isEmpty();
        imageAttachmentPane.setVisible(hasImages);
        imageAttachmentPane.setManaged(hasImages);
    }

    private List<ImageAttachment> takeAndClearPendingImages() {
        if (pendingImages.isEmpty()) {
            return List.of();
        }
        final List<ImageAttachment> images = List.copyOf(pendingImages);
        pendingImages.clear();
        imageAttachmentPane.getChildren().clear();
        updateImageAttachmentPane();
        return images;
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

    private void handleSuggestionClick(final String text) {
        chatTextArea.setText(text);
        chatTextArea.positionCaret(text.length());
        chatTextArea.requestFocus();
    }

    private void localizeWelcomeScreen() {
        browser.updateWelcomeSubtitle(LanguageManager.getLanguageString("welcome.subtitle"));
    }

    private void populateWelcomeData() {
        Thread.startVirtualThread(() -> {
            try {
                final String greeting = buildGreeting();
                final String commandsLabel = LanguageManager.getLanguageString("welcome.commands");
                final String recentLabel = LanguageManager.getLanguageString("welcome.recent");
                final String tipPrefix = LanguageManager.getLanguageString("welcome.tip.prefix");

                final List<Command> commands = BackendApi.getEnabledCommands().stream()
                        .filter(c -> c.visibility().showInWelcomeScreen())
                        .toList();
                final String commandsJson = buildCommandsJson(commands);

                final List<ChatThread> allThreads = BackendApi.getChatThreads();
                final String recentJson = buildRecentThreadsJson(allThreads, chatThread.id(), 3);

                // Cache the recent threads so onOpenThread can use them without extra API calls
                allThreads.stream()
                        .filter(t -> !t.id().equals(chatThread.id()))
                        .forEach(t -> recentThreadCache.put(t.id(), t));

                final String tipText = WelcomeTips.getRandom();

                FxThreadRunner.run(() ->
                        browser.populateWelcomeData(greeting, commandsJson, commandsLabel, recentJson, tipPrefix, tipText, recentLabel)
                );

                // Fetch note count for particle logo
                refreshNoteCount();
            } catch (final Exception e) {
                logger.warn("Failed to populate welcome screen data", e);
            }
        });
    }

    private void refreshNoteCount() {
        try {
            final long count = BackendApi.getNoteCount();
            final String label = LanguageManager.getLanguageString("welcome.notes");
            FxThreadRunner.run(() -> browser.updateNoteCount(count, label));
        } catch (final Exception e) {
            logger.warn("Failed to refresh note count", e);
        }
    }

    private static String buildGreeting() {
        final int hour = LocalDateTime.now().getHour();
        if (hour >= 5 && hour < 12) {
            return LanguageManager.getLanguageString("welcome.greeting.morning");
        } else if (hour >= 12 && hour < 18) {
            return LanguageManager.getLanguageString("welcome.greeting.afternoon");
        } else {
            return LanguageManager.getLanguageString("welcome.greeting.evening");
        }
    }

    private static String buildRecentThreadsJson(final List<ChatThread> threads, final String currentThreadId, final int limit) {
        final var sb = new StringBuilder("[");
        int count = 0;
        final List<ChatThread> sorted = threads.stream()
                .filter(t -> !t.id().equals(currentThreadId))
                .sorted(java.util.Comparator.comparing(
                        t -> t.createdAt() != null ? t.createdAt() : java.time.LocalDateTime.MIN,
                        java.util.Comparator.reverseOrder()))
                .toList();
        for (final ChatThread t : sorted) {
            final String name = t.name() != null && !t.name().isBlank() ? t.name() : "…";
            if (count > 0) sb.append(",");
            sb.append("{\"id\":\"").append(escapeJson(t.id()))
                    .append("\",\"name\":\"").append(escapeJson(name))
                    .append("\"}");
            count++;
            if (count >= limit) break;
        }
        return sb.append("]").toString();
    }

    private static String buildCommandsJson(final List<Command> commands) {
        final var sb = new StringBuilder("[");
        for (int i = 0; i < commands.size(); i++) {
            final Command c = commands.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"id\":\"").append(escapeJson(c.id()))
                    .append("\",\"name\":\"").append(escapeJson(c.name()))
                    .append("\",\"description\":\"").append(escapeJson(c.description() != null ? c.description() : ""))
                    .append("\"}");
        }
        return sb.append("]").toString();
    }

    private static String escapeJson(final String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private record PreparedMessage(
            String messageToSend,
            CommandUsageInfo commandUsageInfo
    ) {
    }

}
