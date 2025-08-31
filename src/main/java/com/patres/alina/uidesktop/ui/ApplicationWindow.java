package com.patres.alina.uidesktop.ui;

import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.chat.thread.ui.ChatThreadHistoryPane;
import com.patres.alina.uidesktop.command.settings.CommandPane;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.settings.ui.AssistantSettingsPane;
import com.patres.alina.uidesktop.settings.ui.ServerSettingsPane;
import com.patres.alina.uidesktop.settings.ui.UiSettingsPane;
import com.patres.alina.uidesktop.ui.chat.ChatWindow;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class ApplicationWindow extends BorderPane {

    private ChatWindow chatWindow;

    private ChatThread chatThread;


    @FXML
    private VBox centerPane;

    @FXML
    public StackPane rootCenterContainer;

    private final AppModalPane appModalPane = new AppModalPane();
    private final ApplicationModalPaneContent uiSettingsModalPane = new UiSettingsPane(appModalPane::hide);
    private final ApplicationModalPaneContent assistantServerSettings = new AssistantSettingsPane(appModalPane::hide);
    private final ApplicationModalPaneContent chatThreadHistoryPane = new ChatThreadHistoryPane(appModalPane::hide, this);
    private final ApplicationModalPaneContent commandPane = new CommandPane(appModalPane::hide, this);

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
        createAndOpenNewChatThread();
        rootCenterContainer.getChildren()
                .add(appModalPane);

    }

    public void openThreadHistories() {
        chatThreadHistoryPane.reload();
        appModalPane.show(chatThreadHistoryPane);
    }

    public void createAndOpenNewChatThread() {
        chatThread = BackendApi.createChatThread();
        openChatThread(chatThread);
    }

    public void createNewChatThread() {
        chatThread = BackendApi.createChatThread();
        loadChatThread(chatThread);
    }

    public void loadChatThread(ChatThread chatThread) {
        centerPane.getChildren().removeIf(node -> node instanceof ChatWindow);
        if (chatWindow != null) {
            chatWindow.unsubscribeEvents();
        }
        chatWindow = new ChatWindow(chatThread, this);
        centerPane.getChildren().add(chatWindow);
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

    public void openCommands() {
        commandPane.reload();
        appModalPane.show(commandPane);
    }

    public Stage getStage() {
        return (Stage) getScene().getWindow();
    }


    public Node getTopPane() {
        return getTop();
    }

    public Optional<ChatThread> getChatThread() {
        return Optional.of(chatWindow)
                .map(ChatWindow::getChatThread);
    }

    public AppModalPane getAppModalPane() {
        return appModalPane;
    }
}
