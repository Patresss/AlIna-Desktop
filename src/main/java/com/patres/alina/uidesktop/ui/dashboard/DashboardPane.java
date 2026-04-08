package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.dashboard.DashboardState;
import com.patres.alina.common.dashboard.DashboardTask;
import com.patres.alina.common.dashboard.DashboardTaskUpdateRequest;
import com.patres.alina.common.event.DashboardUpdatedEvent;
import com.patres.alina.common.event.WorkspaceSettingsUpdatedEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.util.EmojiLabelHelper;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

public class DashboardPane extends VBox {

    private final Label titleLabel = new Label();
    private final Label countLabel = new Label();
    private final Button collapseButton = new Button();
    private final VBox tasksBox = new VBox(2);
    private final VBox detailsBox = new VBox(2);

    private Timeline refreshTimeline;

    public DashboardPane() {
        getStyleClass().add("workspace-dashboard");
        
        final FontIcon listIcon = new FontIcon(Feather.LIST);
        listIcon.getStyleClass().add("workspace-dashboard-title");
        titleLabel.setText("Tasks");
        titleLabel.setGraphic(listIcon);
        titleLabel.setGraphicTextGap(6);
        titleLabel.getStyleClass().add("workspace-dashboard-title");
        
        countLabel.getStyleClass().add("workspace-dashboard-count");
        collapseButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT, "workspace-collapse-button");
        tasksBox.getStyleClass().add("workspace-task-list");
        detailsBox.getStyleClass().add("workspace-dashboard-content");

        final HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox header = new HBox(8, titleLabel, countLabel, spacer, collapseButton);
        header.getStyleClass().add("workspace-dashboard-header");

        detailsBox.getChildren().add(tasksBox);

        setSpacing(4);
        setPadding(new Insets(6, 0, 4, 0));
        getChildren().addAll(header, detailsBox);

        initializeRefreshTimer();

        DefaultEventBus.getInstance().subscribe(WorkspaceSettingsUpdatedEvent.class, event -> {
            refreshAsync();
            updateRefreshTimer();
        });
        DefaultEventBus.getInstance().subscribe(DashboardUpdatedEvent.class, event -> refreshAsync());

        refreshAsync();
    }

    private void initializeRefreshTimer() {
        final int refreshSeconds = BackendApi.getWorkspaceSettings().dashboardTasksRefreshSeconds();
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(refreshSeconds), event -> refreshAsync()));
        refreshTimeline.setCycleCount(Animation.INDEFINITE);
        refreshTimeline.play();
    }

    private void updateRefreshTimer() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        initializeRefreshTimer();
    }

    public void refreshAsync() {
        Thread.startVirtualThread(() -> {
            final DashboardState state = BackendApi.getDashboardState();
            Platform.runLater(() -> render(state));
        });
    }

    private void render(final DashboardState state) {
        setManaged(state.visible());
        setVisible(state.visible());
        if (!state.visible()) {
            return;
        }

        countLabel.setText(String.valueOf(state.tasks().size()));
        countLabel.setManaged(!state.tasks().isEmpty());
        countLabel.setVisible(!state.tasks().isEmpty());
        collapseButton.setText(null);
        collapseButton.setGraphic(new FontIcon(state.collapsed() ? Feather.CHEVRON_DOWN : Feather.CHEVRON_UP));
        collapseButton.setOnAction(event -> toggleCollapsed(state.collapsed()));
        detailsBox.setManaged(!state.collapsed());
        detailsBox.setVisible(!state.collapsed());

        tasksBox.getChildren().clear();
        if (state.tasks().isEmpty()) {
            final Label emptyState = new Label("Brak aktywnych tasków.");
            emptyState.getStyleClass().add("workspace-dashboard-empty");
            tasksBox.getChildren().add(emptyState);
            return;
        }

        for (final DashboardTask task : state.tasks()) {
            final CheckBox checkBox = new CheckBox();
            checkBox.getStyleClass().add("workspace-task-checkbox");
            checkBox.setOnAction(event -> updateTask(task, checkBox.isSelected()));

            final Label taskLabel = new Label();
            EmojiLabelHelper.applyEmojiText(taskLabel, task.title());
            taskLabel.getStyleClass().add("workspace-task-label");
            taskLabel.setMaxWidth(Double.MAX_VALUE);
            taskLabel.setMinHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
            taskLabel.setWrapText(false);
            HBox.setHgrow(taskLabel, Priority.ALWAYS);

            taskLabel.setOnMouseClicked(event -> {
                EmojiLabelHelper.toggleWrap(taskLabel);
            });

            final HBox row = new HBox(8, checkBox, taskLabel);
            row.getStyleClass().add("workspace-task-item");
            row.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(row, Priority.ALWAYS);
            tasksBox.getChildren().add(row);
        }
    }

    private void toggleCollapsed(final boolean collapsed) {
        Thread.startVirtualThread(() -> {
            BackendApi.updateWorkspaceSettings(
                    BackendApi.getWorkspaceSettings().withDashboardCollapsed(!collapsed)
            );
            refreshAsync();
        });
    }

    private void updateTask(final DashboardTask task, final boolean completed) {
        Thread.startVirtualThread(() -> {
            BackendApi.updateDashboardTask(new DashboardTaskUpdateRequest(
                    task.sourceFile(),
                    task.lineNumber(),
                    completed
            ));
            refreshAsync();
        });
    }
}
