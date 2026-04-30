package com.patres.alina.uidesktop.ui.dashboard;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.dashboard.DashboardState;
import com.patres.alina.common.dashboard.DashboardTask;
import com.patres.alina.common.dashboard.DashboardTaskUpdateRequest;
import com.patres.alina.common.event.DashboardUpdatedEvent;
import com.patres.alina.common.event.WorkspaceSettingsUpdatedEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.util.EmojiLabelHelper;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardPane extends VBox {

    private final Label titleLabel = new Label();
    private final Label countLabel = new Label();
    private final Button collapseButton = new Button();
    private final VBox tasksBox = new VBox(2);
    private final VBox detailsBox = new VBox(2);

    private Timeline refreshTimeline;

    public DashboardPane() {
        getStyleClass().add("workspace-dashboard");
        
        final FontIcon listIcon = new FontIcon(Feather.CHECK_SQUARE);
        listIcon.getStyleClass().add("workspace-dashboard-title");
        titleLabel.setText("Tasks");
        titleLabel.setGraphic(listIcon);
        titleLabel.setGraphicTextGap(5);
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

        setSpacing(2);
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
        updateCollapsedStyle(state.collapsed());

        tasksBox.getChildren().clear();
        if (state.tasks().isEmpty()) {
            final Label emptyState = new Label(LanguageManager.getLanguageString("dashboard.tasks.empty"));
            emptyState.getStyleClass().add("workspace-dashboard-empty");
            tasksBox.getChildren().add(emptyState);
            return;
        }

        final List<String> configuredGroups = state.configuredGroups();
        if (configuredGroups.isEmpty()) {
            // Flat list – original behaviour
            for (final DashboardTask task : state.tasks()) {
                tasksBox.getChildren().add(buildTaskRow(task));
            }
        } else {
            // Grouped rendering
            // Build ordered map: configured groups first, then ungrouped tasks
            final Map<String, List<DashboardTask>> byGroup = new LinkedHashMap<>();
            for (final String g : configuredGroups) {
                byGroup.put(g, new ArrayList<>());
            }
            final List<DashboardTask> ungrouped = new ArrayList<>();
            for (final DashboardTask task : state.tasks()) {
                if (task.group() != null && byGroup.containsKey(task.group())) {
                    byGroup.get(task.group()).add(task);
                } else {
                    ungrouped.add(task);
                }
            }

            // Render ungrouped tasks first (no section label)
            for (final DashboardTask task : ungrouped) {
                tasksBox.getChildren().add(buildTaskRow(task));
            }

            // Render each group that has tasks
            for (final Map.Entry<String, List<DashboardTask>> entry : byGroup.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                final Label groupLabel = new Label(formatGroupLabel(entry.getKey()));
                groupLabel.getStyleClass().add("workspace-task-group-label");
                tasksBox.getChildren().add(groupLabel);
                for (final DashboardTask task : entry.getValue()) {
                    tasksBox.getChildren().add(buildTaskRow(task));
                }
            }
        }
    }

    private String formatGroupLabel(final String group) {
        if (group == null || group.isBlank()) {
            return group;
        }
        final String spaced = group.replace("-", " ");
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    private HBox buildTaskRow(final DashboardTask task) {
        final CheckBox checkBox = new CheckBox();
        checkBox.getStyleClass().add("workspace-task-checkbox");
        checkBox.setOnAction(event -> updateTask(task, checkBox.isSelected()));

        final Label taskLabel = new Label();
        EmojiLabelHelper.applyEmojiText(taskLabel, task.title());
        taskLabel.getStyleClass().add("workspace-task-label");
        taskLabel.setWrapText(false);

        taskLabel.setOnMouseClicked(event -> EmojiLabelHelper.toggleWrap(taskLabel));

        final Region rowSpacer = new Region();
        HBox.setHgrow(rowSpacer, Priority.ALWAYS);

        final String aiPrompt = BackendApi.getWorkspaceSettings().tasksAiPrompt();
        final String arguments = buildTaskArguments(task);
        final Region aiSlot = DashboardAiButton.createSlot(aiPrompt, arguments);

        final HBox row = new HBox(8, checkBox, taskLabel, rowSpacer, aiSlot);
        row.getStyleClass().add("workspace-task-item");
        row.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(row, Priority.ALWAYS);
        return row;
    }

    private String buildTaskArguments(final DashboardTask task) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Task: ").append(task.title());
        if (task.group() != null && !task.group().isBlank()) {
            sb.append("\nGroup: ").append(task.group());
        }
        if (task.sourceFile() != null && !task.sourceFile().isBlank()) {
            sb.append("\nSource: ").append(task.sourceFile());
        }
        return sb.toString();
    }

    private void toggleCollapsed(final boolean collapsed) {
        Thread.startVirtualThread(() -> {
            BackendApi.updateWorkspaceSettings(
                    BackendApi.getWorkspaceSettings().withDashboardCollapsed(!collapsed)
            );
            refreshAsync();
        });
    }

    private void updateCollapsedStyle(final boolean collapsed) {
        if (collapsed) {
            if (!getStyleClass().contains("workspace-dashboard-collapsed")) {
                getStyleClass().add("workspace-dashboard-collapsed");
            }
        } else {
            getStyleClass().remove("workspace-dashboard-collapsed");
        }
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
