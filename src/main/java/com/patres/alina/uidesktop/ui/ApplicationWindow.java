package com.patres.alina.uidesktop.ui;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.chat.thread.ui.ChatThreadHistoryPane;
import com.patres.alina.uidesktop.command.settings.CommandPane;
import com.patres.alina.uidesktop.common.event.CommandShortcutExecutedEvent;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.settings.ui.AssistantSettingsPane;
import com.patres.alina.uidesktop.settings.ui.UiSettingsPane;
import com.patres.alina.uidesktop.settings.ui.WorkspaceSettingsPane;
import com.patres.alina.uidesktop.ui.chat.Browser;
import com.patres.alina.uidesktop.ui.chat.ChatWindow;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.dashboard.DashboardPane;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

public class ApplicationWindow extends BorderPane {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationWindow.class);

    private ChatWindow chatWindow;

    private ChatThread chatThread;


    @FXML
    private VBox centerPane;

    @FXML
    public StackPane rootCenterContainer;

    private final AppModalPane appModalPane = new AppModalPane();
    private final ApplicationModalPaneContent uiSettingsModalPane = new UiSettingsPane(appModalPane::hide);
    private final ApplicationModalPaneContent assistantServerSettings = new AssistantSettingsPane(appModalPane::hide);
    private final ApplicationModalPaneContent workspaceSettingsPane = new WorkspaceSettingsPane(appModalPane::hide);
    private final ApplicationModalPaneContent chatThreadHistoryPane = new ChatThreadHistoryPane(appModalPane::hide, this);
    private final ApplicationModalPaneContent commandPane = new CommandPane(appModalPane::hide, this);
    private final DashboardPane dashboardPane = new DashboardPane();

    public ApplicationWindow() {
        super();
        try {
            var loader = new FXMLLoader(
                    Resources.getResource("fxml/application-window.fxml").toURL()
            );
            loader.setController(ApplicationWindow.this);
            loader.setRoot(this);
            loader.setResources(LanguageManager.getBundle());
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load FXML file", e);
        }
    }

    @FXML
    public void initialize() {
        centerPane.setSpacing(14);
        centerPane.getChildren().add(dashboardPane);
        createAndOpenInitialChatThread();
        rootCenterContainer.getChildren()
                .add(appModalPane);

        // Subscribe to command shortcut executed event
        DefaultEventBus.getInstance().subscribe(
                CommandShortcutExecutedEvent.class,
                this::handleCommandShortcutExecuted
        );
    }

    private void handleCommandShortcutExecuted(CommandShortcutExecutedEvent event) {
        // Load the thread on JavaFX thread WITHOUT activating the application
        Platform.runLater(() -> {
            ChatThread thread = BackendApi.getChatThread(event.getThreadId()).orElse(null);
            if (thread != null) {
                loadChatThread(thread);
                // Close any modal panes
                appModalPane.hide(false);
            }
        });
    }

    public void openThreadHistories() {
        chatThreadHistoryPane.reload();
        appModalPane.show(chatThreadHistoryPane);
    }

    public void createAndOpenNewChatThread() {
        prepareOpenCodeForFreshChat();
        createAndOpenInitialChatThread();
    }

    private void createAndOpenInitialChatThread() {
        chatThread = BackendApi.createChatThread();
        openChatThread(chatThread);
    }

    public void createNewChatThread() {
        prepareOpenCodeForFreshChat();
        chatThread = BackendApi.createChatThread();
        loadChatThread(chatThread);
    }

    private void prepareOpenCodeForFreshChat() {
        getChatThread().ifPresent(currentThread -> BackendApi.cancelChatMessagesStream(currentThread.id()));
        try {
            BackendApi.prepareOpenCodeForFreshChat();
        } catch (RuntimeException e) {
            logger.warn("Cannot reset OpenCode runtime before creating a new chat", e);
        }
    }

    public void loadChatThread(ChatThread chatThread) {
        centerPane.getChildren().removeIf(node -> node instanceof ChatWindow);
        if (chatWindow != null) {
            chatWindow.unsubscribeEvents();
        }
        chatWindow = new ChatWindow(chatThread, this);
        centerPane.getChildren().add(chatWindow);
        VBox.setMargin(chatWindow, new Insets(2, 0, 0, 0));
        VBox.setVgrow(chatWindow, javafx.scene.layout.Priority.ALWAYS);
        dashboardPane.refreshAsync();
    }

    public void openChatThread(ChatThread chatThread) {
        loadChatThread(chatThread);
        appModalPane.hide(true);
    }

    public void openUiSettings() {
        uiSettingsModalPane.reload();
        appModalPane.show(uiSettingsModalPane);
    }

    public void openAssistantSettings() {
        assistantServerSettings.reload();
        appModalPane.show(assistantServerSettings);
    }

    public void openWorkspaceSettings() {
        workspaceSettingsPane.reload();
        appModalPane.show(workspaceSettingsPane);
    }

    public void openCommands() {
        commandPane.reload();
        appModalPane.show(commandPane);
    }

    public void openCurrentOpenCodeSession() {
        getChatThread()
                .map(ChatThread::id)
                .map(BackendApi::getOpenCodeSessionWebUrl)
                .ifPresent(Browser::openWebpage);
    }

    public Stage getStage() {
        return (Stage) getScene().getWindow();
    }


    public Node getTopPane() {
        return getTop();
    }

    public Optional<ChatThread> getChatThread() {
        return Optional.ofNullable(chatWindow)
                .map(ChatWindow::getChatThread);
    }

    public AppModalPane getAppModalPane() {
        return appModalPane;
    }

    public ChatWindow getChatWindow() {
        return chatWindow;
    }
}
