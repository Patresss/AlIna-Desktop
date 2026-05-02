package com.patres.alina.uidesktop.ui;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.event.ChatThreadTitleUpdatedEvent;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.chat.thread.ui.ChatThreadHistoryPane;
import com.patres.alina.uidesktop.command.settings.CommandPane;
import com.patres.alina.uidesktop.common.event.CommandShortcutExecutedEvent;
import com.patres.alina.uidesktop.quickaction.settings.QuickActionSettingsPane;
import com.patres.alina.uidesktop.scheduler.SchedulerSettingsPane;
import com.patres.alina.uidesktop.scheduler.SchedulerTaskExecutor;
import com.patres.alina.uidesktop.settings.ui.AboutPane;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.settings.ui.DashboardSettingsPane;
import com.patres.alina.uidesktop.settings.ui.OpenCodeSettingsPane;
import com.patres.alina.uidesktop.settings.ui.UiSettingsPane;
import com.patres.alina.uidesktop.ui.chat.Browser;
import com.patres.alina.uidesktop.ui.chat.ChatTabBar;
import com.patres.alina.uidesktop.ui.chat.ChatWindow;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.dashboard.DashboardPane;
import com.patres.alina.uidesktop.ui.dashboard.DashboardContainer;
import com.patres.alina.uidesktop.ui.dashboard.GitHubWidget;
import com.patres.alina.uidesktop.ui.dashboard.GoogleCalendarWidget;
import com.patres.alina.uidesktop.ui.dashboard.JiraWidget;
import com.patres.alina.uidesktop.ui.dashboard.MediaControlWidget;
import com.patres.alina.uidesktop.ui.dashboard.ObsidianWidget;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ApplicationWindow extends BorderPane {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationWindow.class);

    // Tab system: maps threadId -> ChatWindow
    private final Map<String, ChatWindow> chatWindows = new LinkedHashMap<>();
    private final Map<String, ChatThread> chatThreads = new LinkedHashMap<>();
    private String activeTabId;
    private ChatTabBar chatTabBar;

    private ApplicationHeaderButtonBox headerButtonBox;

    @FXML
    private VBox centerPane;

    @FXML
    public StackPane rootCenterContainer;

    private final AppModalPane appModalPane = new AppModalPane();
    private final ApplicationModalPaneContent uiSettingsModalPane = new UiSettingsPane(appModalPane::hide);
    private final ApplicationModalPaneContent dashboardSettingsPane = new DashboardSettingsPane(appModalPane::hide);
    private final ApplicationModalPaneContent openCodeSettingsPane = new OpenCodeSettingsPane(appModalPane::hide);
    private final ApplicationModalPaneContent chatThreadHistoryPane = new ChatThreadHistoryPane(appModalPane::hide, this);
    private final ApplicationModalPaneContent commandPane = new CommandPane(appModalPane::hide, this);
    private final ApplicationModalPaneContent quickActionSettingsPane = new QuickActionSettingsPane(appModalPane::hide);
    private final ApplicationModalPaneContent schedulerSettingsPane = new SchedulerSettingsPane(appModalPane::hide);
    private final ApplicationModalPaneContent aboutPane = new AboutPane(appModalPane::hide);
    private final DashboardPane dashboardPane = new DashboardPane();
    private final MediaControlWidget mediaControlWidget = new MediaControlWidget();
    private final GitHubWidget gitHubWidget = new GitHubWidget();
    private final JiraWidget jiraWidget = new JiraWidget();
    private final GoogleCalendarWidget googleCalendarWidget = new GoogleCalendarWidget();
    private final ObsidianWidget obsidianWidget = new ObsidianWidget();
    private final DashboardContainer dashboardContainer = new DashboardContainer(mediaControlWidget, dashboardPane, gitHubWidget, jiraWidget, googleCalendarWidget, obsidianWidget);

    // Split mode layout
    private final VBox chatContentPane = new VBox();
    private final HBox splitContainer = new HBox();
    private final ScrollPane dashboardScrollPane = new ScrollPane();
    private final Region dashboardChatSeparator = new Region();
    private boolean splitModeActive = false;

    @SuppressWarnings("unused") // retained as field to keep event subscription alive
    private SchedulerTaskExecutor schedulerTaskExecutor;

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
        centerPane.setSpacing(0);
        centerPane.setMinWidth(0);

        // Initialize tab bar
        chatTabBar = new ChatTabBar();
        chatTabBar.setOnTabSelected(this::handleTabSelected);
        chatTabBar.setOnTabClosed(this::handleTabClosed);
        chatTabBar.setOnNewTabRequested(this::createAndOpenNewChatThread);

        // Chat content pane holds all chat windows
        chatContentPane.setMinWidth(0);
        VBox.setVgrow(chatContentPane, Priority.ALWAYS);

        // Configure split container (used only in split mode)
        dashboardScrollPane.setFitToWidth(true);
        dashboardScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        dashboardScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        dashboardScrollPane.setMaxWidth(Double.MAX_VALUE);
        dashboardScrollPane.setMinWidth(0);
        dashboardScrollPane.getStyleClass().add("split-dashboard-scroll");
        chatContentPane.setMaxWidth(Double.MAX_VALUE);
        splitContainer.setMinWidth(0);
        VBox.setVgrow(splitContainer, Priority.ALWAYS);
        splitContainer.setSpacing(8);

        // Separator between dashboard and chat
        dashboardChatSeparator.getStyleClass().add("dashboard-chat-separator");
        dashboardChatSeparator.setMaxWidth(Double.MAX_VALUE);

        // Build normal layout: chatTabBar, dashboardContainer, separator, chatContentPane
        centerPane.getChildren().add(chatTabBar);
        dashboardContainer.getStyleClass().add("dashboard-area");
        centerPane.getChildren().add(dashboardContainer);
        centerPane.getChildren().add(dashboardChatSeparator);
        centerPane.getChildren().add(chatContentPane);

        refreshIntegrationWidgets();
        createAndOpenInitialChatThread();
        rootCenterContainer.getChildren()
                .add(appModalPane);

        // Apply persisted split mode
        final boolean persistedSplitMode = BackendApi.getWorkspaceSettings().splitMode();
        if (persistedSplitMode) {
            applySplitMode(true);
        }

        // Subscribe to command shortcut executed event
        DefaultEventBus.getInstance().subscribe(
                CommandShortcutExecutedEvent.class,
                this::handleCommandShortcutExecuted
        );

        // Refresh integration widgets when workspace settings change
        DefaultEventBus.getInstance().subscribe(
                com.patres.alina.common.event.WorkspaceSettingsUpdatedEvent.class,
                event -> Platform.runLater(this::refreshIntegrationWidgets)
        );

        // Update tab name and thread history when OpenCode generates a session title
        DefaultEventBus.getInstance().subscribe(
                ChatThreadTitleUpdatedEvent.class,
                event -> {
                    chatTabBar.updateTabName(event.getThreadId(), event.getNewTitle());
                    BackendApi.renameChatThread(new com.patres.alina.common.thread.ChatThreadRenameRequest(event.getThreadId(), event.getNewTitle()));
                }
        );

        // Initialize scheduler task executor
        schedulerTaskExecutor = new SchedulerTaskExecutor(this);
    }

    private void refreshIntegrationWidgets() {
        var settings = BackendApi.getWorkspaceSettings();
        gitHubWidget.refresh(settings.githubToken());
        jiraWidget.refresh();
        googleCalendarWidget.refresh();
        obsidianWidget.refresh();
        // Keep chat separator in sync with dashboard visibility (only in normal mode)
        if (!splitModeActive) {
            final boolean showDashboard = settings.showDashboard();
            dashboardChatSeparator.setManaged(showDashboard);
            dashboardChatSeparator.setVisible(showDashboard);
        }
    }

    private void handleCommandShortcutExecuted(CommandShortcutExecutedEvent event) {
        Thread.startVirtualThread(() -> {
            ChatThread thread = BackendApi.getChatThread(event.getThreadId()).orElse(null);
            if (thread != null) {
                List<ChatMessageResponseModel> messages = BackendApi.getMessagesByThreadId(thread.id());
                Platform.runLater(() -> {
                    loadChatThreadInActiveTab(thread, messages);
                    appModalPane.hide(false);
                });
            }
        });
    }

    // ═══════════════════════════════════════════
    // Tab management
    // ═══════════════════════════════════════════

    private void handleTabSelected(String threadId) {
        switchToTab(threadId);
    }

    private void handleTabClosed(String threadId) {
        closeTab(threadId);
    }

    private void switchToTab(String threadId) {
        if (threadId.equals(activeTabId)) {
            return;
        }

        // Hide current chat window
        ChatWindow currentWindow = getActiveChatWindow();
        if (currentWindow != null) {
            currentWindow.setVisible(false);
            currentWindow.setManaged(false);
        }

        activeTabId = threadId;

        // Show the target chat window
        ChatWindow targetWindow = chatWindows.get(threadId);
        if (targetWindow != null) {
            targetWindow.setVisible(true);
            targetWindow.setManaged(true);
        }
    }

    private void closeTab(String threadId) {
        if (chatWindows.size() <= 1) {
            return;
        }

        // If closing the active tab, switch to a neighbor first
        if (threadId.equals(activeTabId)) {
            List<String> tabIds = chatTabBar.getTabIds();
            int index = tabIds.indexOf(threadId);
            String nextTabId;
            if (index > 0) {
                nextTabId = tabIds.get(index - 1);
            } else {
                nextTabId = tabIds.get(index + 1);
            }
            chatTabBar.selectTab(nextTabId);
            switchToTab(nextTabId);
        }

        // Remove the chat window
        ChatWindow window = chatWindows.remove(threadId);
        chatThreads.remove(threadId);
        if (window != null) {
            window.unsubscribeEvents();
            chatContentPane.getChildren().remove(window);
        }

        chatTabBar.removeTab(threadId);
    }

    private void addTabAndLoadChat(ChatThread chatThread, List<ChatMessageResponseModel> messages) {
        // If tab already exists, just switch to it
        if (chatWindows.containsKey(chatThread.id())) {
            chatTabBar.selectTab(chatThread.id());
            switchToTab(chatThread.id());
            return;
        }

        // Hide current active chat window
        ChatWindow currentWindow = getActiveChatWindow();
        if (currentWindow != null) {
            currentWindow.setVisible(false);
            currentWindow.setManaged(false);
        }

        // Create new ChatWindow
        ChatWindow newWindow = new ChatWindow(chatThread, this, messages);
        newWindow.setVisible(true);
        newWindow.setManaged(true);
        newWindow.setMinWidth(0);
        VBox.setMargin(newWindow, new Insets(2, 0, 0, 0));
        VBox.setVgrow(newWindow, javafx.scene.layout.Priority.ALWAYS);

        // Store in maps
        chatWindows.put(chatThread.id(), newWindow);
        chatThreads.put(chatThread.id(), chatThread);
        activeTabId = chatThread.id();

        // Add to chat content pane
        chatContentPane.getChildren().add(newWindow);

        // Add tab and activate it
        chatTabBar.addTab(chatThread, true);

        newWindow.focusTextArea();
        dashboardPane.refreshAsync();
    }

    // ═══════════════════════════════════════════
    // Split mode
    // ═══════════════════════════════════════════

    public void applySplitMode(boolean split) {
        if (split == splitModeActive) {
            return;
        }
        splitModeActive = split;

        if (split) {
            // Normal -> Split: move dashboardContainer into scrollPane on the left, chatContentPane on the right
            centerPane.getChildren().remove(dashboardContainer);
            centerPane.getChildren().remove(dashboardChatSeparator);
            centerPane.getChildren().remove(chatContentPane);

            dashboardScrollPane.setContent(dashboardContainer);
            VBox.setMargin(dashboardContainer, Insets.EMPTY);

            // 50/50 split: bind both halves to exactly half the container width
            var halfWidth = splitContainer.widthProperty().subtract(8).divide(2);
            dashboardScrollPane.prefWidthProperty().bind(halfWidth);
            dashboardScrollPane.minWidthProperty().bind(halfWidth);
            chatContentPane.prefWidthProperty().bind(halfWidth);
            chatContentPane.minWidthProperty().bind(halfWidth);

            splitContainer.getChildren().setAll(chatContentPane, dashboardScrollPane);
            centerPane.getChildren().add(splitContainer);
        } else {
            // Unbind size constraints before moving back
            dashboardScrollPane.prefWidthProperty().unbind();
            dashboardScrollPane.minWidthProperty().unbind();
            chatContentPane.prefWidthProperty().unbind();
            chatContentPane.minWidthProperty().unbind();
            chatContentPane.setPrefWidth(-1);
            chatContentPane.setMinWidth(0);

            // Split -> Normal: move dashboard and chatContentPane back into centerPane vertically
            centerPane.getChildren().remove(splitContainer);
            splitContainer.getChildren().clear();
            dashboardScrollPane.setContent(null);

            centerPane.getChildren().add(dashboardContainer);
            centerPane.getChildren().add(dashboardChatSeparator);
            centerPane.getChildren().add(chatContentPane);
        }
    }

    // ═══════════════════════════════════════════
    // Public API (preserved for compatibility)
    // ═══════════════════════════════════════════

    /** Called by AssistantAppLauncher so we can programmatically drive the split-mode toggle. */
    public void setHeaderButtonBox(ApplicationHeaderButtonBox box) {
        this.headerButtonBox = box;
    }

    /**
     * Programmatically toggle split mode as if the user clicked the header button.
     * This keeps WorkspaceSettings and the toggle button visual state in sync.
     */
    public void setSplitMode(boolean split) {
        if (headerButtonBox != null) {
            headerButtonBox.setSplitModeSelected(split);
        } else {
            // Fallback if called before header is attached
            applySplitMode(split);
        }
    }

    public void openThreadHistories() {
        chatThreadHistoryPane.reload();
        appModalPane.show(chatThreadHistoryPane);
    }

    public void createAndOpenNewChatThread() {
        Thread.startVirtualThread(() -> {
            final ChatThread newThread = BackendApi.createChatThread();
            Platform.runLater(() -> {
                addTabAndLoadChat(newThread, List.of());
                appModalPane.hide(true);
            });
        });
    }

    private void createAndOpenInitialChatThread() {
        Thread.startVirtualThread(() -> {
            ChatThread chatThread = BackendApi.createChatThread();
            Platform.runLater(() -> addTabAndLoadChat(chatThread, List.of()));
        });
    }

    public void createNewChatThread() {
        Thread.startVirtualThread(() -> {
            final ChatThread newThread = BackendApi.createChatThread();
            Platform.runLater(() -> addTabAndLoadChat(newThread, List.of()));
        });
    }

    public void clearCurrentChatThread() {
        Thread.startVirtualThread(() -> {
            final ChatThread newThread = BackendApi.createChatThread();
            Platform.runLater(() -> loadChatThreadInActiveTab(newThread, List.of()));
        });
    }

    /**
     * Load a chat thread in the currently active tab (replaces the content).
     * Used by command shortcuts and history selection when we want to replace
     * the active tab's content rather than creating a new tab.
     */
    public void loadChatThreadInActiveTab(ChatThread chatThread, List<ChatMessageResponseModel> messages) {
        // If there's already a tab for this thread, just switch to it
        if (chatWindows.containsKey(chatThread.id())) {
            chatTabBar.selectTab(chatThread.id());
            switchToTab(chatThread.id());
            return;
        }

        // Remove old active tab's window
        if (activeTabId != null) {
            ChatWindow oldWindow = chatWindows.remove(activeTabId);
            chatThreads.remove(activeTabId);
            if (oldWindow != null) {
                oldWindow.unsubscribeEvents();
                chatContentPane.getChildren().remove(oldWindow);
            }
            chatTabBar.removeTab(activeTabId);
        }

        // Add as new tab
        addTabAndLoadChat(chatThread, messages);
    }

    public void loadChatThread(ChatThread chatThread) {
        loadChatThread(chatThread, BackendApi.getMessagesByThreadId(chatThread.id()));
    }

    public void loadChatThread(ChatThread chatThread, List<ChatMessageResponseModel> messages) {
        // Open in a new tab (or switch to existing)
        addTabAndLoadChat(chatThread, messages);
    }

    public void openChatThread(ChatThread chatThread) {
        Thread.startVirtualThread(() -> {
            List<ChatMessageResponseModel> messages = BackendApi.getMessagesByThreadId(chatThread.id());
            Platform.runLater(() -> {
                addTabAndLoadChat(chatThread, messages);
                appModalPane.hide(true);
            });
        });
    }

    public void openUiSettings() {
        uiSettingsModalPane.reload();
        appModalPane.show(uiSettingsModalPane);
    }

    public void openDashboardSettings() {
        dashboardSettingsPane.reload();
        appModalPane.show(dashboardSettingsPane);
    }

    public void openAbout() {
        aboutPane.reload();
        appModalPane.show(aboutPane);
    }

    public void openOpenCodeSettings() {
        openCodeSettingsPane.reload();
        appModalPane.show(openCodeSettingsPane);
    }

    public void openCommands() {
        commandPane.reload();
        appModalPane.show(commandPane);
    }

    public void openQuickActionSettings() {
        quickActionSettingsPane.reload();
        appModalPane.show(quickActionSettingsPane);
    }

    public void openSchedulerSettings() {
        schedulerSettingsPane.reload();
        appModalPane.show(schedulerSettingsPane);
    }

    public void openCurrentOpenCodeSession() {
        getChatThread()
                .map(ChatThread::id)
                .map(BackendApi::getOpenCodeSessionWebUrl)
                .ifPresent(Browser::openWebpage);
    }

    public void collapseDashboard() {
        dashboardContainer.collapse();
    }

    public void expandDashboard() {
        dashboardContainer.expand();
    }

    public Stage getStage() {
        return (Stage) getScene().getWindow();
    }


    public Node getTopPane() {
        return getTop();
    }

    public Optional<ChatThread> getChatThread() {
        if (activeTabId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(chatThreads.get(activeTabId));
    }

    public AppModalPane getAppModalPane() {
        return appModalPane;
    }

    public ChatWindow getChatWindow() {
        return getActiveChatWindow();
    }

    public boolean isActiveTab(String threadId) {
        return threadId != null && threadId.equals(activeTabId);
    }

    private ChatWindow getActiveChatWindow() {
        if (activeTabId == null) {
            return null;
        }
        return chatWindows.get(activeTabId);
    }
}
