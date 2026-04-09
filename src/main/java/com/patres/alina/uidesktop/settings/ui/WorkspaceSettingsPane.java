package com.patres.alina.uidesktop.settings.ui;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import com.patres.alina.common.opencode.OpenCodeRuntimeStatus;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import org.kordamp.ikonli.feather.Feather;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTile;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableEditableSpinner;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizablePasswordField;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableRegion;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableTextArea;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableTextField;

public class WorkspaceSettingsPane extends SettingsModalPaneContent {

    private ToggleSwitch showDashboardToggle;
    private ToggleSwitch alwaysOnTopToggle;
    private ToggleSwitch showDashboardMusicToggle;
    private ToggleSwitch showDashboardTasksToggle;
    private ToggleSwitch showDashboardGithubToggle;
    private ToggleSwitch showDashboardJiraToggle;
    private ToggleSwitch showDashboardCalendarToggle;
    private ToggleSwitch calendarHideAllDayToggle;
    private ToggleSwitch calendarShowOnlyCurrentAndFutureToggle;

    private TextField tasksFileField;
    private TextField openCodeHostnameField;
    private TextField openCodePortField;
    private TextField openCodeWorkingDirectoryField;
    private TextArea openCodeStatusArea;
    private PasswordField githubTokenField;
    private TextField jiraEmailField;
    private PasswordField jiraApiTokenField;

    private Spinner<Integer> dashboardTaskLimitSpinner;
    private Spinner<Integer> dashboardTasksRefreshSpinner;
    private Spinner<Integer> dashboardGithubRefreshSpinner;
    private Spinner<Integer> dashboardMediaRefreshSpinner;
    private Spinner<Integer> dashboardGithubPrLimitSpinner;
    private Spinner<Integer> dashboardJiraRefreshSpinner;
    private Spinner<Integer> dashboardJiraIssueLimitSpinner;
    private Spinner<Integer> dashboardCalendarRefreshSpinner;

    private WorkspaceSettings settings;

    public WorkspaceSettingsPane(final Runnable backFunction) {
        super(backFunction);
    }

    @Override
    protected void reset() {
        settings = BackendApi.getWorkspaceSettings();

        showDashboardToggle.setSelected(settings.showDashboard());
        alwaysOnTopToggle.setSelected(settings.keepWindowAlwaysOnTop());
        showDashboardMusicToggle.setSelected(settings.showDashboardMusic());
        showDashboardTasksToggle.setSelected(settings.showDashboardTasks());
        showDashboardGithubToggle.setSelected(settings.showDashboardGithub());
        showDashboardJiraToggle.setSelected(settings.showDashboardJira());
        showDashboardCalendarToggle.setSelected(settings.showDashboardCalendar());
        calendarHideAllDayToggle.setSelected(settings.calendarHideAllDayEvents());
        calendarShowOnlyCurrentAndFutureToggle.setSelected(settings.calendarShowOnlyCurrentAndFuture());
        tasksFileField.setText(orEmpty(settings.tasksFile()));
        openCodeHostnameField.setText(orEmpty(settings.openCodeHostname()));
        openCodePortField.setText(String.valueOf(settings.openCodePort()));
        openCodeWorkingDirectoryField.setText(orEmpty(settings.openCodeWorkingDirectory()));
        dashboardTaskLimitSpinner.getValueFactory().setValue(settings.dashboardTaskLimit());
        dashboardTasksRefreshSpinner.getValueFactory().setValue(settings.dashboardTasksRefreshSeconds());
        dashboardGithubRefreshSpinner.getValueFactory().setValue(settings.dashboardGithubRefreshSeconds());
        dashboardMediaRefreshSpinner.getValueFactory().setValue(settings.dashboardMediaRefreshSeconds());
        dashboardGithubPrLimitSpinner.getValueFactory().setValue(settings.dashboardGithubPrLimit());
        dashboardJiraRefreshSpinner.getValueFactory().setValue(settings.dashboardJiraRefreshSeconds());
        dashboardJiraIssueLimitSpinner.getValueFactory().setValue(settings.dashboardJiraIssueLimit());
        dashboardCalendarRefreshSpinner.getValueFactory().setValue(settings.dashboardCalendarRefreshSeconds());
        githubTokenField.setText(orEmpty(settings.githubToken()));
        jiraEmailField.setText(orEmpty(settings.jiraEmail()));
        jiraApiTokenField.setText(orEmpty(settings.jiraApiToken()));
        refreshOpenCodeStatus();
    }

    @Override
    protected void save() {
        final WorkspaceSettings updated = new WorkspaceSettings(
                showDashboardToggle.isSelected(),
                settings.dashboardCollapsed(),
                alwaysOnTopToggle.isSelected(),
                tasksFileField.getText(),
                dashboardTaskLimitSpinner.getValue(),
                openCodeHostnameField.getText(),
                parseInteger(openCodePortField.getText(), WorkspaceSettings.DEFAULT_OPENCODE_PORT),
                openCodeWorkingDirectoryField.getText(),
                githubTokenField.getText(),
                dashboardTasksRefreshSpinner.getValue(),
                dashboardGithubRefreshSpinner.getValue(),
                dashboardMediaRefreshSpinner.getValue(),
                dashboardGithubPrLimitSpinner.getValue(),
                dashboardJiraRefreshSpinner.getValue(),
                dashboardJiraIssueLimitSpinner.getValue(),
                jiraEmailField.getText(),
                jiraApiTokenField.getText(),
                showDashboardMusicToggle.isSelected(),
                showDashboardTasksToggle.isSelected(),
                showDashboardGithubToggle.isSelected(),
                showDashboardJiraToggle.isSelected(),
                showDashboardCalendarToggle.isSelected(),
                dashboardCalendarRefreshSpinner.getValue(),
                calendarHideAllDayToggle.isSelected(),
                calendarShowOnlyCurrentAndFutureToggle.isSelected(),
                settings.calendarNotificationsEnabled(),
                settings.calendarNotificationMinutesBefore()
        );
        BackendApi.updateWorkspaceSettings(updated);
        settings = updated;
        refreshOpenCodeStatus();
    }

    @Override
    protected List<Node> generateContent() {
        settings = BackendApi.getWorkspaceSettings();

        final var header = createTextSeparator("settings.workspace.title", Styles.TITLE_3);
        final var dashboardHeader = createTextSeparator("settings.workspace.dashboard.section", Styles.TITLE_4);
        final var runtimeHeader = createTextSeparator("settings.workspace.runtime.title", Styles.TITLE_4);
        final var runtimeStatusHeader = createTextSeparator("settings.workspace.openCode.status.section", Styles.TITLE_4);
        final var integrationsHeader = createTextSeparator("settings.workspace.integrations.section", Styles.TITLE_4);

        showDashboardToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        alwaysOnTopToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        showDashboardMusicToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        showDashboardTasksToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        showDashboardGithubToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        showDashboardJiraToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        showDashboardCalendarToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        calendarHideAllDayToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        calendarShowOnlyCurrentAndFutureToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        tasksFileField = createResizableTextField(settingsBox);
        openCodeHostnameField = createResizableTextField(settingsBox);
        openCodePortField = createResizableTextField(settingsBox);
        openCodeWorkingDirectoryField = createResizableTextField(settingsBox);
        githubTokenField = createResizablePasswordField(settingsBox);
        jiraEmailField = createResizableTextField(settingsBox);
        jiraApiTokenField = createResizablePasswordField(settingsBox);

        openCodeStatusArea = createResizableTextArea(settingsBox);
        openCodeStatusArea.setPrefRowCount(8);
        openCodeStatusArea.setEditable(false);
        openCodeStatusArea.setWrapText(true);
        openCodeStatusArea.setFocusTraversable(false);

        final var taskLimitTile = createTile(
                "settings.workspace.taskLimit.title",
                "settings.workspace.taskLimit.description"
        );
        dashboardTaskLimitSpinner = createResizableEditableSpinner(1, 50, settings.dashboardTaskLimit(), settingsBox);
        taskLimitTile.setAction(dashboardTaskLimitSpinner);

        final var tasksRefreshTile = createTile(
                "settings.workspace.tasksRefresh.title",
                "settings.workspace.tasksRefresh.description"
        );
        dashboardTasksRefreshSpinner = createResizableEditableSpinner(1, 3600, settings.dashboardTasksRefreshSeconds(), settingsBox);
        tasksRefreshTile.setAction(dashboardTasksRefreshSpinner);

        final var githubRefreshTile = createTile(
                "settings.workspace.githubRefresh.title",
                "settings.workspace.githubRefresh.description"
        );
        dashboardGithubRefreshSpinner = createResizableEditableSpinner(1, 3600, settings.dashboardGithubRefreshSeconds(), settingsBox);
        githubRefreshTile.setAction(dashboardGithubRefreshSpinner);

        final var mediaRefreshTile = createTile(
                "settings.workspace.mediaRefresh.title",
                "settings.workspace.mediaRefresh.description"
        );
        dashboardMediaRefreshSpinner = createResizableEditableSpinner(1, 3600, settings.dashboardMediaRefreshSeconds(), settingsBox);
        mediaRefreshTile.setAction(dashboardMediaRefreshSpinner);

        final var githubPrLimitTile = createTile(
                "settings.workspace.githubPrLimit.title",
                "settings.workspace.githubPrLimit.description"
        );
        dashboardGithubPrLimitSpinner = createResizableEditableSpinner(1, 50, settings.dashboardGithubPrLimit(), settingsBox);
        githubPrLimitTile.setAction(dashboardGithubPrLimitSpinner);

        final var jiraRefreshTile = createTile(
                "settings.workspace.jiraRefresh.title",
                "settings.workspace.jiraRefresh.description"
        );
        dashboardJiraRefreshSpinner = createResizableEditableSpinner(1, 3600, settings.dashboardJiraRefreshSeconds(), settingsBox);
        jiraRefreshTile.setAction(dashboardJiraRefreshSpinner);

        final var jiraIssueLimitTile = createTile(
                "settings.workspace.jiraIssueLimit.title",
                "settings.workspace.jiraIssueLimit.description"
        );
        dashboardJiraIssueLimitSpinner = createResizableEditableSpinner(1, 50, settings.dashboardJiraIssueLimit(), settingsBox);
        jiraIssueLimitTile.setAction(dashboardJiraIssueLimitSpinner);

        final var calendarRefreshTile = createTile(
                "settings.workspace.calendarRefresh.title",
                "settings.workspace.calendarRefresh.description"
        );
        dashboardCalendarRefreshSpinner = createResizableEditableSpinner(1, 3600, settings.dashboardCalendarRefreshSeconds(), settingsBox);
        calendarRefreshTile.setAction(dashboardCalendarRefreshSpinner);

        final Button refreshOpenCodeStatusButton = createButton(Feather.REFRESH_CCW, e -> refreshOpenCodeStatus());
        final Node openCodeWorkingDirectoryPicker = createFilePickerField(openCodeWorkingDirectoryField, this::chooseOpenCodeWorkingDirectory);
        final VBox openCodeStatusBox = new VBox(8, openCodeStatusArea, refreshOpenCodeStatusButton);

        return List.of(
                header,
                dashboardHeader,
                tileFor(showDashboardToggle, "settings.workspace.dashboard.title", "settings.workspace.dashboard.description"),
                tileFor(alwaysOnTopToggle, "settings.workspace.ontop.title", "settings.workspace.ontop.description"),
                tileFor(showDashboardMusicToggle, "settings.workspace.showMusic.title", "settings.workspace.showMusic.description"),
                tileFor(showDashboardTasksToggle, "settings.workspace.showTasks.title", "settings.workspace.showTasks.description"),
                tileFor(showDashboardGithubToggle, "settings.workspace.showGithub.title", "settings.workspace.showGithub.description"),
                tileFor(showDashboardJiraToggle, "settings.workspace.showJira.title", "settings.workspace.showJira.description"),
                tileFor(showDashboardCalendarToggle, "settings.workspace.showCalendar.title", "settings.workspace.showCalendar.description"),
                tileFor(calendarHideAllDayToggle, "settings.workspace.calendarHideAllDay.title", "settings.workspace.calendarHideAllDay.description"),
                tileFor(calendarShowOnlyCurrentAndFutureToggle, "settings.workspace.calendarOnlyFuture.title", "settings.workspace.calendarOnlyFuture.description"),
                tileFor(tasksFileField, "settings.workspace.tasksFile.title", "settings.workspace.tasksFile.description"),
                taskLimitTile,
                tasksRefreshTile,
                githubRefreshTile,
                mediaRefreshTile,
                githubPrLimitTile,
                jiraRefreshTile,
                jiraIssueLimitTile,
                calendarRefreshTile,
                new Separator(),
                integrationsHeader,
                tileFor(githubTokenField, "settings.workspace.github.token.title", "settings.workspace.github.token.description"),
                tileFor(jiraEmailField, "settings.workspace.jira.email.title", "settings.workspace.jira.email.description"),
                tileFor(jiraApiTokenField, "settings.workspace.jira.token.title", "settings.workspace.jira.token.description"),
                new Separator(),
                runtimeHeader,
                tileFor(openCodeHostnameField, "settings.workspace.openCode.hostname.title", "settings.workspace.openCode.hostname.description"),
                tileFor(openCodePortField, "settings.workspace.openCode.port.title", "settings.workspace.openCode.port.description"),
                tileFor(openCodeWorkingDirectoryPicker, "settings.workspace.openCode.directory.title", "settings.workspace.openCode.directory.description"),
                new Separator(),
                runtimeStatusHeader,
                tileFor(openCodeStatusBox, "settings.workspace.openCode.status.title", "settings.workspace.openCode.status.description")
        );
    }

    private Node tileFor(final Node action, final String titleKey, final String descriptionKey) {
        final var tile = createTile(titleKey, descriptionKey);
        tile.setAction(action);
        return tile;
    }

    private Node createFilePickerField(final TextField field, final Runnable onPick) {
        final Button browseButton = createButton(Feather.FOLDER, e -> onPick.run());
        browseButton.setFocusTraversable(false);
        final HBox box = new HBox(8, field, browseButton);
        HBox.setHgrow(field, Priority.ALWAYS);
        return box;
    }

    private void chooseOpenCodeWorkingDirectory() {
        final DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("OpenCode working directory");
        applyInitialDirectory(chooser, openCodeWorkingDirectoryField.getText());
        final File selected = chooser.showDialog(settingsBox.getScene() == null ? null : settingsBox.getScene().getWindow());
        if (selected != null) {
            openCodeWorkingDirectoryField.setText(selected.getAbsolutePath());
        }
    }

    private void applyInitialDirectory(final DirectoryChooser chooser, final String currentValue) {
        final Path path = resolveExistingDirectory(currentValue);
        if (path != null) {
            chooser.setInitialDirectory(path.toFile());
        }
    }

    private Path resolveExistingDirectory(final String currentValue) {
        if (currentValue == null || currentValue.isBlank()) {
            return existingDirectory(Path.of(System.getProperty("user.home", ".")));
        }
        final Path path = Path.of(currentValue).toAbsolutePath().normalize();
        final Path directory = existingDirectory(path);
        return directory != null ? directory : existingDirectory(Path.of(System.getProperty("user.home", ".")));
    }

    private Path existingDirectory(final Path path) {
        if (path == null) {
            return null;
        }
        return Files.isDirectory(path) ? path : null;
    }

    private void refreshOpenCodeStatus() {
        try {
            final OpenCodeRuntimeStatus status = BackendApi.getOpenCodeRuntimeStatus();
            openCodeStatusArea.setText(formatOpenCodeStatus(status));
        } catch (Exception e) {
            openCodeStatusArea.setText("Status: unavailable" + System.lineSeparator()
                    + "Message: " + fallback(e.getMessage(), "Unknown error"));
        }
    }

    private String formatOpenCodeStatus(final OpenCodeRuntimeStatus status) {
        if (status == null) {
            return "Status unavailable.";
        }
        return String.join(System.lineSeparator(),
                "Status: " + (status.healthy() ? "running" : "unreachable"),
                "Version: " + fallback(status.version(), "-"),
                "Base URL: " + fallback(status.baseUrl(), "-"),
                "Host: " + fallback(status.hostname(), "-"),
                "Port: " + status.port(),
                "Working directory: " + fallback(status.workingDirectory(), "-"),
                "Working directory exists: " + yesNo(status.workingDirectoryExists()),
                "Process started by AlIna: " + yesNo(status.processRunning()),
                "Message: " + fallback(status.statusMessage(), "-")
        );
    }

    private String orEmpty(final String value) {
        return value == null ? "" : value;
    }

    private String fallback(final String value, final String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String yesNo(final boolean value) {
        return value ? "yes" : "no";
    }

    private int parseInteger(final String raw, final int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
