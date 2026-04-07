package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.event.WorkspaceSettingsUpdatedEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Container for all dashboard widgets with global collapse control.
 */
public class DashboardContainer extends VBox {

    private final Label titleLabel = new Label("Dashboard");
    private final Button collapseButton = new Button();
    private final VBox widgetsBox = new VBox(14);
    
    private final MediaControlWidget mediaControlWidget;
    private final DashboardPane dashboardPane;
    private final GitHubWidget gitHubWidget;
    private final JiraWidget jiraWidget;
    
    private boolean collapsed = false;

    public DashboardContainer(MediaControlWidget mediaControlWidget, DashboardPane dashboardPane, GitHubWidget gitHubWidget, JiraWidget jiraWidget) {
        this.mediaControlWidget = mediaControlWidget;
        this.dashboardPane = dashboardPane;
        this.gitHubWidget = gitHubWidget;
        this.jiraWidget = jiraWidget;
        
        getStyleClass().add("workspace-dashboard");
        
        titleLabel.getStyleClass().add("workspace-dashboard-title");
        
        collapseButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT, "workspace-collapse-button");
        collapseButton.setOnAction(event -> toggleCollapsed());

        final HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox header = new HBox(8, titleLabel, spacer, collapseButton);
        header.getStyleClass().add("workspace-dashboard-header");

        widgetsBox.getChildren().addAll(mediaControlWidget, dashboardPane, gitHubWidget, jiraWidget);
        
        // Remove borders from child widgets
        mediaControlWidget.getStyleClass().remove("workspace-dashboard");
        dashboardPane.getStyleClass().remove("workspace-dashboard");
        gitHubWidget.getStyleClass().remove("workspace-dashboard");
        jiraWidget.getStyleClass().remove("workspace-dashboard");

        setSpacing(10);
        setPadding(new Insets(10, 12, 10, 12));
        getChildren().addAll(header, widgetsBox);

        updateCollapseButton();
        
        DefaultEventBus.getInstance().subscribe(WorkspaceSettingsUpdatedEvent.class, event -> refreshVisibility());
        refreshVisibility();
    }

    private void toggleCollapsed() {
        collapsed = !collapsed;
        updateCollapseButton();
        widgetsBox.setManaged(!collapsed);
        widgetsBox.setVisible(!collapsed);
    }

    private void updateCollapseButton() {
        collapseButton.setText(null);
        collapseButton.setGraphic(new FontIcon(collapsed ? Feather.CHEVRON_DOWN : Feather.CHEVRON_UP));
    }
    
    private void refreshVisibility() {
        Platform.runLater(() -> {
            final WorkspaceSettings settings = BackendApi.getWorkspaceSettings();
            final boolean show = settings.showDashboard();
            setManaged(show);
            setVisible(show);
            
            // Update individual widget visibility
            updateWidgetVisibility(mediaControlWidget, settings.showDashboardMusic());
            updateWidgetVisibility(dashboardPane, settings.showDashboardTasks());
            updateWidgetVisibility(gitHubWidget, settings.showDashboardGithub());
            updateWidgetVisibility(jiraWidget, settings.showDashboardJira());
        });
    }
    
    private void updateWidgetVisibility(javafx.scene.Node widget, boolean visible) {
        widget.setManaged(visible);
        widget.setVisible(visible);
    }
}
