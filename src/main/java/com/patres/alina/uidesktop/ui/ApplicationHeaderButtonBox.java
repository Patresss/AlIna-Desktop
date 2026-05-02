package com.patres.alina.uidesktop.ui;

import com.patres.alina.common.event.WorkspaceSettingsUpdatedEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;

public class ApplicationHeaderButtonBox extends HBox {

    @FXML
    private ToggleButton pinToggleButton;
    @FXML
    private ToggleButton splitModeToggleButton;
    @FXML
    private MenuItem uiSettingsMenuItem;
    @FXML
    private MenuItem dashboardSettingsMenuItem;
    @FXML
    private MenuItem openCodeSettingsMenuItem;
    @FXML
    private MenuItem commandsMenuItem;
    @FXML
    private MenuItem quickActionSettingsMenuItem;
    @FXML
    private MenuItem schedulerMenuItem;

    private ApplicationWindow applicationWindow;

    public ApplicationHeaderButtonBox(ApplicationWindow applicationWindow) {
        super();
        this.applicationWindow = applicationWindow;
        try {
            var loader = new FXMLLoader(
                    Resources.getResource("fxml/header-bar-button-box.fxml").toURL()
            );
            loader.setController(ApplicationHeaderButtonBox.this);
            loader.setRoot(this);
            loader.setResources(LanguageManager.getBundle());
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load FXML file", e);
        }
    }

    @FXML
    public void initialize() {
        pinToggleButton.setSelected(BackendApi.getWorkspaceSettings().keepWindowAlwaysOnTop());
        pinToggleButton.selectedProperty()
                .addListener((obs, oldValue, newValue) -> {
                    getStage().setAlwaysOnTop(newValue);
                    final WorkspaceSettings settings = BackendApi.getWorkspaceSettings();
                    BackendApi.updateWorkspaceSettings(settings.withKeepWindowAlwaysOnTop(newValue));
                });

        // Split mode toggle
        final WorkspaceSettings initialSettings = BackendApi.getWorkspaceSettings();
        splitModeToggleButton.setSelected(initialSettings.splitMode());
        refreshSplitModeVisibility(initialSettings);

        splitModeToggleButton.selectedProperty()
                .addListener((obs, oldValue, newValue) -> {
                    final WorkspaceSettings settings = BackendApi.getWorkspaceSettings();
                    BackendApi.updateWorkspaceSettings(settings.withSplitMode(newValue));
                    applicationWindow.applySplitMode(newValue);
                });

        // Listen for settings changes to show/hide split mode button when dashboard visibility changes
        DefaultEventBus.getInstance().subscribe(
                WorkspaceSettingsUpdatedEvent.class,
                event -> Platform.runLater(() -> {
                    final WorkspaceSettings settings = BackendApi.getWorkspaceSettings();
                    refreshSplitModeVisibility(settings);
                })
        );

        uiSettingsMenuItem.textProperty().bind(LanguageManager.createStringBinding("settings.ui.title"));
        dashboardSettingsMenuItem.textProperty().bind(LanguageManager.createStringBinding("settings.dashboard.title"));
        openCodeSettingsMenuItem.textProperty().bind(LanguageManager.createStringBinding("settings.opencode.title"));
        commandsMenuItem.textProperty().bind(LanguageManager.createStringBinding("command.title"));
        quickActionSettingsMenuItem.textProperty().bind(LanguageManager.createStringBinding("quickaction.settings.title"));
        schedulerMenuItem.textProperty().bind(LanguageManager.createStringBinding("scheduler.title"));
    }

    private void refreshSplitModeVisibility(WorkspaceSettings settings) {
        final boolean dashboardVisible = settings.showDashboard();
        splitModeToggleButton.setVisible(dashboardVisible);
        splitModeToggleButton.setManaged(dashboardVisible);
    }


    @FXML
    public void openThreadHistories() {
        applicationWindow.openThreadHistories();
    }

    @FXML
    public void createAndOpenNewChatThread() {
        applicationWindow.createAndOpenNewChatThread();
    }

    @FXML
    public void openUiSettings() {
        applicationWindow.openUiSettings();
    }

    @FXML
    public void openAbout() {
        applicationWindow.openAbout();
    }

    @FXML
    public void openDashboardSettings() {
        applicationWindow.openDashboardSettings();
    }

    @FXML
    public void openOpenCodeSettings() {
        applicationWindow.openOpenCodeSettings();
    }

    @FXML
    public void openCommands() {
        applicationWindow.openCommands();
    }

    @FXML
    public void openQuickActionSettings() {
        applicationWindow.openQuickActionSettings();
    }

    @FXML
    public void openSchedulerSettings() {
        applicationWindow.openSchedulerSettings();
    }

    @FXML
    public void openOpenCodeSession() {
        applicationWindow.openCurrentOpenCodeSession();
    }

    public Stage getStage() {
        return (Stage) getScene().getWindow();
    }

    /** Programmatically set the split mode toggle (triggers the listener → persists + applies layout). */
    public void setSplitModeSelected(boolean selected) {
        splitModeToggleButton.setSelected(selected);
    }

}
