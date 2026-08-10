package com.patres.alina.uidesktop.ui;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.event.ChatMessageStreamEvent;
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
import com.patres.alina.uidesktop.ui.calendar.GoogleCalendarFeed;
import com.patres.alina.uidesktop.ui.dashboard.DashboardPane;
import com.patres.alina.uidesktop.ui.dashboard.DashboardContainer;
import com.patres.alina.uidesktop.ui.dashboard.DashboardHeightPolicy;
import com.patres.alina.uidesktop.ui.dashboard.GitHubWidget;
import com.patres.alina.uidesktop.ui.dashboard.GoogleCalendarWidget;
import com.patres.alina.uidesktop.ui.dashboard.JiraWidget;
import com.patres.alina.uidesktop.ui.dashboard.MediaControlWidget;
import com.patres.alina.uidesktop.ui.dashboard.ObsidianWidget;
import com.patres.alina.uidesktop.ui.dashboard.UpcomingCalendarEventWidget;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationWindow extends BorderPane {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationWindow.class);

    // Tab system: maps threadId -> ChatWindow
    private final Map<String, ChatWindow> chatWindows = new LinkedHashMap<>();
    private final Map<String, ChatThread> chatThreads = new LinkedHashMap<>();
    private String activeTabId;
    private ChatTabBar chatTabBar;

    private ApplicationHeaderButtonBox headerButtonBox;
    private String activeAgentSessionUri;
    private long agentSessionUriRefreshGeneration;
    private final Set<String> agentSessionRefreshSignalledThreads = ConcurrentHashMap.newKeySet();

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
    private final ObsidianWidget obsidianWidget = new ObsidianWidget();
    private final GoogleCalendarFeed calendarFeed;
    private final GoogleCalendarWidget googleCalendarWidget;
    private final UpcomingCalendarEventWidget upcomingCalendarEventWidget;
    private final DashboardContainer dashboardContainer;

    // Split mode layout
    private final VBox chatContentPane = new VBox();
    private final HBox splitContainer = new HBox();
    private final ScrollPane dashboardScrollPane = new ScrollPane();
    private final Region dashboardChatSeparator = new Region();
    private boolean splitModeActive = false;
    private boolean dashboardHeightRefreshPending = false;

    @SuppressWarnings("unused") // retained as field to keep event subscription alive
    private SchedulerTaskExecutor schedulerTaskExecutor;

    public ApplicationWindow(final GoogleCalendarFeed calendarFeed) {
        super();
        this.calendarFeed = calendarFeed;
        googleCalendarWidget = new GoogleCalendarWidget(calendarFeed);
        upcomingCalendarEventWidget = new UpcomingCalendarEventWidget(calendarFeed);
        dashboardContainer = new DashboardContainer(
                mediaControlWidget,
                dashboardPane,
                gitHubWidget,
                jiraWidget,
                googleCalendarWidget,
                upcomingCalendarEventWidget,
                obsidianWidget
        );
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
        centerPane.getStyleClass().add("application-content");
        rootCenterContainer.getStyleClass().add("application-root-center");

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
        dashboardScrollPane.setHmin(0);
        dashboardScrollPane.setHmax(0);
        dashboardScrollPane.setHvalue(0);
        dashboardScrollPane.setMaxWidth(Double.MAX_VALUE);
        dashboardScrollPane.setMinWidth(0);
        dashboardScrollPane.getStyleClass().add("dashboard-scroll");
        chatContentPane.setMaxWidth(Double.MAX_VALUE);
        splitContainer.setMinWidth(0);
        VBox.setVgrow(splitContainer, Priority.ALWAYS);
        splitContainer.setSpacing(8);

        // Separator between dashboard and chat
        dashboardChatSeparator.getStyleClass().add("dashboard-chat-separator");
        dashboardChatSeparator.setMaxWidth(Double.MAX_VALUE);

        // Build normal layout: chatTabBar, scrollable dashboard, separator, chatContentPane
        centerPane.getChildren().add(chatTabBar);
        dashboardContainer.getStyleClass().add("dashboard-area");
        dashboardScrollPane.setContent(dashboardContainer);
        centerPane.getChildren().add(dashboardScrollPane);
        centerPane.getChildren().add(dashboardChatSeparator);
        centerPane.getChildren().add(chatContentPane);

        centerPane.heightProperty().addListener((observable, oldValue, newValue) -> scheduleDashboardHeightRefresh());
        centerPane.widthProperty().addListener((observable, oldValue, newValue) -> scheduleDashboardHeightRefresh());
        dashboardContainer.layoutBoundsProperty().addListener((observable, oldValue, newValue) -> scheduleDashboardHeightRefresh());
        dashboardScrollPane.viewportBoundsProperty().addListener((observable, oldBounds, newBounds) ->
                synchronizeDashboardViewportWidth(newBounds.getWidth())
        );

        refreshIntegrationWidgets();
        createAndOpenInitialChatThread();
        rootCenterContainer.getChildren()
                .add(appModalPane);

        // Listen for dashboard collapse/expand to adjust split layout
        dashboardContainer.setOnCollapsedStateChanged(this::dashboardCollapsedStateChanged);
        scheduleDashboardHeightRefresh();

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
                event -> Platform.runLater(() -> {
                    refreshIntegrationWidgets();
                    agentSessionRefreshSignalledThreads.clear();
                    refreshAgentSessionUri();
                })
        );

        DefaultEventBus.getInstance().subscribe(
                ChatMessageStreamEvent.class,
                this::handleAgentSessionStreamEvent
        );

        // Update the tab when the active agent reports a persisted thread title.
        DefaultEventBus.getInstance().subscribe(
                ChatThreadTitleUpdatedEvent.class,
                event -> Platform.runLater(() ->
                        chatTabBar.updateTabName(event.getThreadId(), event.getNewTitle())
                )
        );

        // Initialize scheduler task executor
        schedulerTaskExecutor = new SchedulerTaskExecutor(this);
    }

    private void refreshIntegrationWidgets() {
        var settings = BackendApi.getWorkspaceSettings();
        gitHubWidget.refresh(settings.githubToken());
        jiraWidget.refresh();
        calendarFeed.refreshNow();
        obsidianWidget.refresh();
        // Keep chat separator in sync with dashboard visibility (only in normal mode)
        if (!splitModeActive) {
            final boolean showDashboard = settings.showDashboard();
            dashboardScrollPane.setManaged(showDashboard);
            dashboardScrollPane.setVisible(showDashboard);
            dashboardChatSeparator.setManaged(showDashboard);
            dashboardChatSeparator.setVisible(showDashboard);
            scheduleDashboardHeightRefresh();
        } else {
            updateSplitLayoutForDashboardState();
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
        refreshAgentSessionUri();
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

        refreshAgentSessionUri();
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
            // Normal -> Split: move the dashboard viewport to the right of the chat.
            centerPane.getChildren().remove(dashboardScrollPane);
            centerPane.getChildren().remove(dashboardChatSeparator);
            centerPane.getChildren().remove(chatContentPane);

            if (!dashboardScrollPane.getStyleClass().contains("split-dashboard-scroll")) {
                dashboardScrollPane.getStyleClass().add("split-dashboard-scroll");
            }
            dashboardScrollPane.setMinHeight(0);
            dashboardScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
            dashboardScrollPane.setMaxHeight(Double.MAX_VALUE);

            splitContainer.getChildren().setAll(chatContentPane, dashboardScrollPane);
            HBox.setHgrow(chatContentPane, Priority.ALWAYS);
            HBox.setHgrow(dashboardScrollPane, Priority.ALWAYS);
            centerPane.getChildren().add(splitContainer);

            // Apply correct widths based on dashboard collapsed state
            updateSplitLayoutForDashboardState();
        } else {
            // Binding removal alone retains the last split width in JavaFX.
            // Restore the normal layout's flexible constraints before moving
            // the nodes back into the vertically stacked workspace.
            SplitModeWidthConstraints.reset(dashboardScrollPane, chatContentPane);

            // Restore dashboard scroll pane visibility in case it was hidden
            dashboardScrollPane.setManaged(true);
            dashboardScrollPane.setVisible(true);

            // Split -> Normal: restore the vertical command-center/workbench composition.
            centerPane.getChildren().remove(splitContainer);
            splitContainer.getChildren().clear();
            dashboardScrollPane.getStyleClass().remove("split-dashboard-scroll");

            centerPane.getChildren().add(dashboardScrollPane);
            centerPane.getChildren().add(dashboardChatSeparator);
            centerPane.getChildren().add(chatContentPane);
            refreshIntegrationWidgets();
            scheduleDashboardHeightRefresh();
        }

        refreshDashboardLayoutAfterWindowResize();
    }

    /**
     * Adjusts the split layout widths based on whether the dashboard is collapsed.
     * When collapsed, chat takes full width; when expanded, 50/50 split is used.
     */
    private void updateSplitLayoutForDashboardState() {
        if (!splitModeActive) {
            return;
        }

        // Start every split-state calculation from neutral constraints. This
        // prevents the previous collapsed/expanded state from leaking into the
        // next one when a property is unbound.
        SplitModeWidthConstraints.reset(dashboardScrollPane, chatContentPane);

        if (!BackendApi.getWorkspaceSettings().showDashboard()) {
            dashboardScrollPane.setManaged(false);
            dashboardScrollPane.setVisible(false);
            chatContentPane.prefWidthProperty().bind(splitContainer.widthProperty());
            chatContentPane.minWidthProperty().bind(splitContainer.widthProperty());
            return;
        }

        if (dashboardContainer.isCollapsed()) {
            // Retain a slim, clickable rail so the dashboard can be restored without a shortcut.
            dashboardScrollPane.setManaged(true);
            dashboardScrollPane.setVisible(true);
            dashboardScrollPane.setPrefWidth(44);
            dashboardScrollPane.setMinWidth(44);
            chatContentPane.prefWidthProperty().bind(splitContainer.widthProperty().subtract(52));
            chatContentPane.minWidthProperty().bind(splitContainer.widthProperty().subtract(52));
        } else {
            // Dashboard expanded: 50/50 split
            dashboardScrollPane.setManaged(true);
            dashboardScrollPane.setVisible(true);
            var halfWidth = splitContainer.widthProperty().subtract(8).divide(2);
            dashboardScrollPane.prefWidthProperty().bind(halfWidth);
            dashboardScrollPane.minWidthProperty().bind(halfWidth);
            chatContentPane.prefWidthProperty().bind(halfWidth);
            chatContentPane.minWidthProperty().bind(halfWidth);
        }
    }

    private void dashboardCollapsedStateChanged() {
        updateSplitLayoutForDashboardState();
        scheduleDashboardHeightRefresh();
    }

    /**
     * Re-synchronizes the dashboard after the Stage has been resized by the
     * floating expand control. The deferred pulse runs after the new Stage
     * bounds are visible to the scene graph, preventing the ScrollPane skin
     * from retaining an offset or content width from the previous window size.
     */
    public void refreshDashboardLayoutAfterWindowResize() {
        Platform.runLater(() -> {
            rootCenterContainer.applyCss();
            rootCenterContainer.layout();
            dashboardScrollPane.applyCss();
            dashboardScrollPane.layout();
            synchronizeDashboardViewportWidth(dashboardScrollPane.getViewportBounds().getWidth());
            dashboardContainer.applyCss();
            dashboardContainer.layout();
            dashboardScrollPane.requestLayout();
            centerPane.requestLayout();
            scheduleDashboardHeightRefresh();
        });
    }

    private void synchronizeDashboardViewportWidth(double viewportWidth) {
        if (viewportWidth <= 0) {
            return;
        }
        dashboardScrollPane.setHmin(0);
        dashboardScrollPane.setHmax(0);
        dashboardScrollPane.setHvalue(0);
        dashboardContainer.setMinWidth(0);
        dashboardContainer.setPrefWidth(viewportWidth);
        dashboardContainer.setMaxWidth(viewportWidth);
        dashboardContainer.requestLayout();
        scheduleDashboardHeightRefresh();
    }

    private void scheduleDashboardHeightRefresh() {
        if (dashboardHeightRefreshPending) {
            return;
        }
        dashboardHeightRefreshPending = true;
        Platform.runLater(() -> {
            dashboardHeightRefreshPending = false;
            refreshDashboardHeight();
        });
    }

    private void refreshDashboardHeight() {
        if (splitModeActive || !dashboardScrollPane.isManaged()) {
            return;
        }
        dashboardContainer.applyCss();
        dashboardContainer.layout();
        final double viewportWidth = dashboardScrollPane.getViewportBounds().getWidth();
        final double contentWidth = Math.max(0, viewportWidth > 0 ? viewportWidth : centerPane.getWidth());
        final double preferredContentHeight = dashboardContainer.prefHeight(contentWidth);
        final double targetHeight = DashboardHeightPolicy.resolve(
                centerPane.getHeight(),
                preferredContentHeight,
                dashboardContainer.isCollapsed()
        );
        dashboardScrollPane.setMinHeight(targetHeight);
        dashboardScrollPane.setPrefHeight(targetHeight);
        dashboardScrollPane.setMaxHeight(targetHeight);
    }

    // ═══════════════════════════════════════════
    // Public API (preserved for compatibility)
    // ═══════════════════════════════════════════

    /** Called by AssistantAppLauncher so we can programmatically drive the split-mode toggle. */
    public void setHeaderButtonBox(ApplicationHeaderButtonBox box) {
        this.headerButtonBox = box;
        refreshAgentSessionUri();
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

    public void openCurrentAgentSession() {
        if (activeAgentSessionUri != null && !activeAgentSessionUri.isBlank()) {
            Browser.openExternalUri(activeAgentSessionUri);
        }
    }

    private void handleAgentSessionStreamEvent(final ChatMessageStreamEvent event) {
        if (event.getThreadId() == null || event.getThreadId().isBlank()) {
            return;
        }
        final boolean terminalEvent = switch (event.getEventType()) {
            case COMPLETE, CANCELLED, ERROR -> true;
            default -> false;
        };
        if (!terminalEvent && !agentSessionRefreshSignalledThreads.add(event.getThreadId())) {
            return;
        }
        Platform.runLater(() -> {
            if (isActiveTab(event.getThreadId()) && (terminalEvent || activeAgentSessionUri == null)) {
                refreshAgentSessionUri();
            }
        });
    }

    private void refreshAgentSessionUri() {
        final long refreshGeneration = ++agentSessionUriRefreshGeneration;
        final String expectedThreadId = activeTabId;
        activeAgentSessionUri = null;
        if (headerButtonBox != null) {
            headerButtonBox.setAgentSessionAvailable(false);
        }
        if (expectedThreadId == null || expectedThreadId.isBlank()) {
            return;
        }
        Thread.startVirtualThread(() -> {
            final String resolvedUri;
            try {
                resolvedUri = BackendApi.getAgentSessionExternalUri(expectedThreadId);
            } catch (Exception e) {
                logger.warn("Cannot resolve external agent session URI for thread {}", expectedThreadId, e);
                Platform.runLater(() -> applyAgentSessionUri(refreshGeneration, expectedThreadId, null));
                return;
            }
            Platform.runLater(() -> applyAgentSessionUri(refreshGeneration, expectedThreadId, resolvedUri));
        });
    }

    private void applyAgentSessionUri(final long refreshGeneration,
                                      final String expectedThreadId,
                                      final String resolvedUri) {
        if (refreshGeneration != agentSessionUriRefreshGeneration
                || !expectedThreadId.equals(activeTabId)) {
            return;
        }
        activeAgentSessionUri = resolvedUri;
        if (headerButtonBox != null) {
            headerButtonBox.setAgentSessionAvailable(resolvedUri != null && !resolvedUri.isBlank());
        }
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

    public String getThreadTitle(final String threadId) {
        final ChatThread thread = chatThreads.get(threadId);
        return thread == null || thread.name() == null ? "" : thread.name();
    }

    public void activateThread(final String threadId) {
        if (threadId == null || threadId.isBlank() || !chatWindows.containsKey(threadId)) {
            return;
        }
        chatTabBar.selectTab(threadId);
        switchToTab(threadId);
        final ChatWindow window = chatWindows.get(threadId);
        if (window != null) {
            window.focusTextArea();
        }
    }

    private ChatWindow getActiveChatWindow() {
        if (activeTabId == null) {
            return null;
        }
        return chatWindows.get(activeTabId);
    }
}
