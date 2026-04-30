package com.patres.alina.uidesktop.ui.dashboard;

import com.patres.alina.common.event.WorkspaceSettingsUpdatedEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Container for all dashboard widgets with global collapse control.
 * Acts as a transparent wrapper — each widget is its own card.
 */
public class DashboardContainer extends VBox {

    private final Label titleLabel = new Label("WORKSPACE");
    private final VBox widgetsBox = new VBox(6);
    private final FontIcon collapseIcon = new FontIcon(Feather.CHEVRON_UP);
    private final HBox collapseBar = new HBox();

    private final MediaControlWidget mediaControlWidget;
    private final DashboardPane dashboardPane;
    private final GitHubWidget gitHubWidget;
    private final JiraWidget jiraWidget;
    private final GoogleCalendarWidget googleCalendarWidget;
    private final ObsidianWidget obsidianWidget;

    private boolean collapsed = false;

    public DashboardContainer(MediaControlWidget mediaControlWidget, DashboardPane dashboardPane, GitHubWidget gitHubWidget, JiraWidget jiraWidget, GoogleCalendarWidget googleCalendarWidget, ObsidianWidget obsidianWidget) {
        this.mediaControlWidget = mediaControlWidget;
        this.dashboardPane = dashboardPane;
        this.gitHubWidget = gitHubWidget;
        this.jiraWidget = jiraWidget;
        this.googleCalendarWidget = googleCalendarWidget;
        this.obsidianWidget = obsidianWidget;

        getStyleClass().add("workspace-dashboard-container");

        titleLabel.getStyleClass().add("workspace-container-title");

        final HBox header = new HBox(6, titleLabel);
        header.getStyleClass().add("workspace-dashboard-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(2, 4, 4, 4));

        widgetsBox.getChildren().addAll(
                mediaControlWidget,
                googleCalendarWidget,
                dashboardPane,
                gitHubWidget,
                jiraWidget,
                obsidianWidget
        );

        // Bottom collapse bar
        collapseIcon.getStyleClass().add("workspace-collapse-bar-icon");
        collapseBar.getStyleClass().add("workspace-collapse-bar");
        collapseBar.setAlignment(Pos.CENTER);
        collapseBar.getChildren().add(collapseIcon);
        collapseBar.setOnMouseClicked(event -> toggleCollapsed());

        setSpacing(4);
        setMinWidth(0);
        getChildren().addAll(header, widgetsBox, collapseBar);

        updateCollapseBar();

        DefaultEventBus.getInstance().subscribe(WorkspaceSettingsUpdatedEvent.class, event -> refreshVisibility());
        refreshVisibility();
    }

    private void toggleCollapsed() {
        collapsed = !collapsed;
        updateCollapseBar();
        widgetsBox.setManaged(!collapsed);
        widgetsBox.setVisible(!collapsed);
        titleLabel.setManaged(!collapsed);
        titleLabel.setVisible(!collapsed);
    }

    private void updateCollapseBar() {
        collapseIcon.setIconCode(collapsed ? Feather.CHEVRON_DOWN : Feather.CHEVRON_UP);
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
            updateWidgetVisibility(obsidianWidget, settings.showDashboardObsidian());
        });
    }

    private void updateWidgetVisibility(javafx.scene.Node widget, boolean visible) {
        widget.setManaged(visible);
        widget.setVisible(visible);
    }
}
