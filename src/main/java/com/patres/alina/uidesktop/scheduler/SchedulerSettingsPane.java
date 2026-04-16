package com.patres.alina.uidesktop.scheduler;

import atlantafx.base.controls.Tile;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import com.patres.alina.server.scheduler.ExecutionMode;
import com.patres.alina.server.scheduler.ScheduledTask;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTile;

/**
 * Settings pane for managing scheduled tasks.
 * Displays a list of existing tasks with controls, and a form to add/edit tasks.
 */
public class SchedulerSettingsPane extends ApplicationModalPaneContent {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private VBox taskListBox;

    // Form fields for adding/editing
    private TextField nameField;
    private TextField cronField;
    private TextArea promptArea;
    private ChoiceBox<ExecutionMode> executionModeChoice;
    private ChoiceBox<String> modelChoice;
    private Button submitButton;
    private Button cancelEditButton;

    /** When non-null, the form is in edit mode for this task id. */
    private String editingTaskId = null;

    public SchedulerSettingsPane(Runnable backFunction) {
        super(backFunction);
    }

    @Override
    public void reload() {
        clearForm();
        refreshTaskList();
    }

    @Override
    protected List<Node> generateContent() {
        var header = createTextSeparator("scheduler.title", Styles.TITLE_3);

        taskListBox = new VBox(8);
        taskListBox.setPadding(new Insets(4, 0, 8, 0));

        // ─── Add/Edit task form ───
        var addHeader = createTextSeparator("scheduler.add.title", Styles.TITLE_4);

        nameField = new TextField();
        nameField.setPromptText("Morning summary");
        var nameTile = createTile("scheduler.task.name.title", "scheduler.task.name.description");
        nameTile.setAction(nameField);

        cronField = new TextField();
        cronField.setPromptText("0 0 8 * * MON-FRI");
        var cronTile = createTile("scheduler.task.cron.title", "scheduler.task.cron.description");
        cronTile.setAction(cronField);

        promptArea = new TextArea();
        promptArea.setPromptText("Summarize my day...");
        promptArea.setPrefRowCount(3);
        promptArea.setWrapText(true);
        var promptTile = createTile("scheduler.task.prompt.title", "scheduler.task.prompt.description");
        promptTile.setAction(promptArea);

        executionModeChoice = new ChoiceBox<>();
        executionModeChoice.getItems().addAll(ExecutionMode.values());
        executionModeChoice.setValue(ExecutionMode.NEW_TAB);
        executionModeChoice.setConverter(new StringConverter<>() {
            @Override
            public String toString(ExecutionMode mode) {
                if (mode == null) return "";
                return switch (mode) {
                    case CURRENT_TAB -> "Current tab";
                    case NEW_TAB -> "New tab (activate)";
                    case BACKGROUND -> "Background (silent)";
                };
            }

            @Override
            public ExecutionMode fromString(String s) {
                return ExecutionMode.valueOf(s);
            }
        });
        var modeTile = createTile("scheduler.task.mode.title", "scheduler.task.mode.description");
        modeTile.setAction(executionModeChoice);

        modelChoice = new ChoiceBox<>();
        loadAvailableModels();
        var modelTile = createTile("scheduler.task.model.title", "scheduler.task.model.description");
        modelTile.setAction(modelChoice);

        submitButton = new Button("Add task", new FontIcon(Feather.PLUS));
        submitButton.getStyleClass().addAll(Styles.ACCENT);
        submitButton.setOnAction(e -> submitForm());

        cancelEditButton = new Button("Cancel", new FontIcon(Feather.X));
        cancelEditButton.getStyleClass().addAll(Styles.FLAT);
        cancelEditButton.setVisible(false);
        cancelEditButton.setManaged(false);
        cancelEditButton.setOnAction(e -> clearForm());

        HBox buttonBox = new HBox(8, submitButton, cancelEditButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setPadding(new Insets(4, 0, 0, 0));

        refreshTaskList();

        return List.of(
                header,
                taskListBox,
                new Separator(),
                addHeader,
                nameTile,
                cronTile,
                promptTile,
                modeTile,
                modelTile,
                buttonBox
        );
    }

    // ═══════════════════════════════════════════
    // Task list
    // ═══════════════════════════════════════════

    private void loadAvailableModels() {
        Thread.startVirtualThread(() -> {
            try {
                List<String> models = BackendApi.getChatModels();
                javafx.application.Platform.runLater(() -> {
                    modelChoice.getItems().clear();
                    modelChoice.getItems().add(null); // "default" option
                    modelChoice.getItems().addAll(models);
                    modelChoice.setValue(null);
                    modelChoice.setConverter(new StringConverter<>() {
                        @Override
                        public String toString(String model) {
                            return model == null || model.isBlank() ? "(default)" : model;
                        }

                        @Override
                        public String fromString(String s) {
                            return "(default)".equals(s) ? null : s;
                        }
                    });
                });
            } catch (Exception e) {
                // Models will be empty — user can still save without a model
            }
        });
    }

    private void refreshTaskList() {
        if (taskListBox == null) return;
        taskListBox.getChildren().clear();

        List<ScheduledTask> tasks = BackendApi.getScheduledTasks();
        if (tasks.isEmpty()) {
            Label emptyLabel = new Label();
            emptyLabel.textProperty().bind(
                    com.patres.alina.uidesktop.ui.language.LanguageManager.createStringBinding("scheduler.empty"));
            emptyLabel.getStyleClass().add(Styles.TEXT_MUTED);
            taskListBox.getChildren().add(emptyLabel);
            return;
        }

        for (ScheduledTask task : tasks) {
            taskListBox.getChildren().add(createTaskRow(task));
        }
    }

    private Node createTaskRow(ScheduledTask task) {
        String modeName = switch (task.executionMode() != null ? task.executionMode() : ExecutionMode.NEW_TAB) {
            case CURRENT_TAB -> "Current tab";
            case NEW_TAB -> "New tab";
            case BACKGROUND -> "Background";
        };

        String description = task.cron() + " | " + modeName;
        if (task.model() != null && !task.model().isBlank()) {
            description += " | " + task.model();
        }
        if (task.lastRun() != null) {
            description += " | Last: " + task.lastRun().format(DISPLAY_FORMAT);
        }
        if (task.nextRun() != null) {
            description += " | Next: " + task.nextRun().format(DISPLAY_FORMAT);
        }

        Tile tile = createTile(null, null);
        tile.setTitle(task.name());
        tile.titleProperty().unbind();
        tile.setDescription(description);
        tile.descriptionProperty().unbind();

        // Controls: toggle + edit + run now + delete
        ToggleSwitch enabledToggle = new ToggleSwitch();
        enabledToggle.setSelected(task.enabled());
        enabledToggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
            BackendApi.updateScheduledTask(task.withEnabled(newVal));
            refreshTaskList();
        });

        Button editButton = new Button(null, new FontIcon(Feather.EDIT_2));
        editButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        editButton.setOnAction(e -> startEditing(task));

        Button runNowButton = new Button(null, new FontIcon(Feather.PLAY));
        runNowButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        runNowButton.setOnAction(e -> BackendApi.runScheduledTaskNow(task.id()));

        Button deleteButton = new Button(null, new FontIcon(Feather.TRASH_2));
        deleteButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT, Styles.DANGER);
        deleteButton.setOnAction(e -> {
            BackendApi.deleteScheduledTask(task.id());
            if (task.id().equals(editingTaskId)) {
                clearForm();
            }
            refreshTaskList();
        });

        HBox controls = new HBox(8, enabledToggle, editButton, runNowButton, deleteButton);
        controls.setAlignment(Pos.CENTER_RIGHT);
        tile.setAction(controls);

        return tile;
    }

    // ═══════════════════════════════════════════
    // Form: add / edit
    // ═══════════════════════════════════════════

    private void startEditing(ScheduledTask task) {
        editingTaskId = task.id();
        nameField.setText(task.name());
        cronField.setText(task.cron());
        promptArea.setText(task.prompt());
        executionModeChoice.setValue(task.executionMode() != null ? task.executionMode() : ExecutionMode.NEW_TAB);
        modelChoice.setValue(task.model());

        submitButton.setText("Save");
        submitButton.setGraphic(new FontIcon(Feather.SAVE));
        cancelEditButton.setVisible(true);
        cancelEditButton.setManaged(true);

        // Scroll form into view
        nameField.requestFocus();
    }

    private void clearForm() {
        editingTaskId = null;
        if (nameField != null) nameField.clear();
        if (cronField != null) cronField.clear();
        if (promptArea != null) promptArea.clear();
        if (executionModeChoice != null) executionModeChoice.setValue(ExecutionMode.NEW_TAB);
        if (modelChoice != null) modelChoice.setValue(null);

        if (submitButton != null) {
            submitButton.setText("Add task");
            submitButton.setGraphic(new FontIcon(Feather.PLUS));
        }
        if (cancelEditButton != null) {
            cancelEditButton.setVisible(false);
            cancelEditButton.setManaged(false);
        }
    }

    private void submitForm() {
        String name = nameField.getText();
        String cron = cronField.getText();
        String prompt = promptArea.getText();
        ExecutionMode mode = executionModeChoice.getValue();
        String model = modelChoice.getValue();

        if (name == null || name.isBlank() || cron == null || cron.isBlank()
                || prompt == null || prompt.isBlank()) {
            return;
        }

        if (editingTaskId != null) {
            // Update existing task
            ScheduledTask updated = new ScheduledTask(
                    editingTaskId, name, cron, prompt, true, mode, model, null, null
            );
            BackendApi.updateScheduledTask(updated);
        } else {
            // Add new task
            ScheduledTask task = ScheduledTask.createNew(name, cron, prompt, mode, model);
            BackendApi.addScheduledTask(task);
        }

        clearForm();
        refreshTaskList();
    }
}
