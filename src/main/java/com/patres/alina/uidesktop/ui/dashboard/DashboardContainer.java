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
import javafx.scene.control.Separator;
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
    private final VBox widgetsBox = new VBox(0);
    
    private final Separator separatorAfterMusic = new Separator();
    private final Separator separatorAfterCalendar = new Separator();
    private final Separator separatorAfterTasks = new Separator();
    private final Separator separatorAfterGithub = new Separator();
    
    private final MediaControlWidget mediaControlWidget;
    private final DashboardPane dashboardPane;
    private final GitHubWidget gitHubWidget;
    private final JiraWidget jiraWidget;
    private final GoogleCalendarWidget googleCalendarWidget;
    
    private boolean collapsed = false;

    public DashboardContainer(MediaControlWidget mediaControlWidget, DashboardPane dashboardPane, GitHubWidget gitHubWidget, JiraWidget jiraWidget, GoogleCalendarWidget googleCalendarWidget) {
        this.mediaControlWidget = mediaControlWidget;
        this.dashboardPane = dashboardPane;
        this.gitHubWidget = gitHubWidget;
        this.jiraWidget = jiraWidget;
        this.googleCalendarWidget = googleCalendarWidget;
        
        getStyleClass().add("workspace-dashboard");
        
        titleLabel.getStyleClass().add("workspace-container-title");
        
        collapseButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT, "workspace-collapse-button");
        collapseButton.setOnAction(event -> toggleCollapsed());

        final HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox header = new HBox(8, titleLabel, spacer, collapseButton);
        header.getStyleClass().add("workspace-dashboard-header");

        // Style separators
        for (final Separator sep : new Separator[]{separatorAfterMusic, separatorAfterCalendar, separatorAfterTasks, separatorAfterGithub}) {
            sep.getStyleClass().add("workspace-widget-separator");
        }

        widgetsBox.getChildren().addAll(
                mediaControlWidget, separatorAfterMusic,
                googleCalendarWidget, separatorAfterCalendar,
                dashboardPane, separatorAfterTasks,
                gitHubWidget, separatorAfterGithub,
                jiraWidget
        );
        
        // Remove borders from child widgets — container provides the border
        googleCalendarWidget.getStyleClass().remove("workspace-dashboard");
        mediaControlWidget.getStyleClass().remove("workspace-dashboard");
        dashboardPane.getStyleClass().remove("workspace-dashboard");
        gitHubWidget.getStyleClass().remove("workspace-dashboard");
        jiraWidget.getStyleClass().remove("workspace-dashboard");
        
        // Add inner-widget style for tighter padding
        googleCalendarWidget.getStyleClass().add("workspace-widget-inner");
        mediaControlWidget.getStyleClass().add("workspace-widget-inner");
        dashboardPane.getStyleClass().add("workspace-widget-inner");
        gitHubWidget.getStyleClass().add("workspace-widget-inner");
        jiraWidget.getStyleClass().add("workspace-widget-inner");

        setSpacing(4);
        setPadding(new Insets(10, 14, 10, 14));
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
            updateWidgetVisibility(googleCalendarWidget, settings.showDashboardCalendar());
            
            // Separators follow the widget ABOVE them (hidden if that widget is hidden)
            updateWidgetVisibility(separatorAfterMusic, settings.showDashboardMusic());
            updateWidgetVisibility(separatorAfterCalendar, settings.showDashboardCalendar());
            updateWidgetVisibility(separatorAfterTasks, settings.showDashboardTasks());
            updateWidgetVisibility(separatorAfterGithub, settings.showDashboardGithub());
        });
    }
    
    private void updateWidgetVisibility(javafx.scene.Node widget, boolean visible) {
        widget.setManaged(visible);
        widget.setVisible(visible);
    }
}
