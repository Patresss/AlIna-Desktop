package com.patres.alina.uidesktop.settings.ui;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
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
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableTextField;

/**
 * Settings pane for all Dashboard-related settings, organized per integration.
 */
public class DashboardSettingsPane extends SettingsModalPaneContent {

    // General
    private ToggleSwitch showDashboardToggle;
    private ToggleSwitch alwaysOnTopToggle;

    // Music
    private ToggleSwitch showDashboardMusicToggle;
    private Spinner<Integer> dashboardMediaRefreshSpinner;

    // Calendar
    private ToggleSwitch showDashboardCalendarToggle;
    private ToggleSwitch calendarHideAllDayToggle;
    private ToggleSwitch calendarShowOnlyCurrentAndFutureToggle;
    private Spinner<Integer> dashboardCalendarRefreshSpinner;
    private ToggleSwitch calendarNotificationsToggle;
    private Spinner<Integer> calendarNotificationMinutesSpinner;
    private ToggleSwitch calendarChangeNotificationsToggle;

    // Tasks
    private ToggleSwitch showDashboardTasksToggle;
    private TextField tasksFileField;
    private TextField taskGroupsField;
    private Spinner<Integer> dashboardTaskLimitSpinner;
    private Spinner<Integer> dashboardTasksRefreshSpinner;

    // GitHub
    private ToggleSwitch showDashboardGithubToggle;
    private PasswordField githubTokenField;
    private Spinner<Integer> dashboardGithubRefreshSpinner;
    private Spinner<Integer> dashboardGithubPrLimitSpinner;
    private ToggleSwitch githubChangeNotificationsToggle;

    // Jira
    private ToggleSwitch showDashboardJiraToggle;
    private TextField jiraEmailField;
    private PasswordField jiraApiTokenField;
    private Spinner<Integer> dashboardJiraRefreshSpinner;
    private Spinner<Integer> dashboardJiraIssueLimitSpinner;
    private ToggleSwitch jiraChangeNotificationsToggle;

    private WorkspaceSettings settings;

    public DashboardSettingsPane(final Runnable backFunction) {
        super(backFunction);
    }

    @Override
    protected void reset() {
        settings = BackendApi.getWorkspaceSettings();

        // General
        showDashboardToggle.setSelected(settings.showDashboard());
        alwaysOnTopToggle.setSelected(settings.keepWindowAlwaysOnTop());

        // Music
        showDashboardMusicToggle.setSelected(settings.showDashboardMusic());
        dashboardMediaRefreshSpinner.getValueFactory().setValue(settings.dashboardMediaRefreshSeconds());

        // Calendar
        showDashboardCalendarToggle.setSelected(settings.showDashboardCalendar());
        calendarHideAllDayToggle.setSelected(settings.calendarHideAllDayEvents());
        calendarShowOnlyCurrentAndFutureToggle.setSelected(settings.calendarShowOnlyCurrentAndFuture());
        dashboardCalendarRefreshSpinner.getValueFactory().setValue(settings.dashboardCalendarRefreshSeconds());
        calendarNotificationsToggle.setSelected(settings.calendarNotificationsEnabled());
        calendarNotificationMinutesSpinner.getValueFactory().setValue(settings.calendarNotificationMinutesBefore());
        calendarChangeNotificationsToggle.setSelected(settings.calendarChangeNotificationsEnabled());

        // Tasks
        showDashboardTasksToggle.setSelected(settings.showDashboardTasks());
        tasksFileField.setText(orEmpty(settings.tasksFile()));
        taskGroupsField.setText(orEmpty(settings.taskGroups()));
        dashboardTaskLimitSpinner.getValueFactory().setValue(settings.dashboardTaskLimit());
        dashboardTasksRefreshSpinner.getValueFactory().setValue(settings.dashboardTasksRefreshSeconds());

        // GitHub
        showDashboardGithubToggle.setSelected(settings.showDashboardGithub());
        githubTokenField.setText(orEmpty(settings.githubToken()));
        dashboardGithubRefreshSpinner.getValueFactory().setValue(settings.dashboardGithubRefreshSeconds());
        dashboardGithubPrLimitSpinner.getValueFactory().setValue(settings.dashboardGithubPrLimit());
        githubChangeNotificationsToggle.setSelected(settings.githubChangeNotificationsEnabled());

        // Jira
        showDashboardJiraToggle.setSelected(settings.showDashboardJira());
        jiraEmailField.setText(orEmpty(settings.jiraEmail()));
        jiraApiTokenField.setText(orEmpty(settings.jiraApiToken()));
        dashboardJiraRefreshSpinner.getValueFactory().setValue(settings.dashboardJiraRefreshSeconds());
        dashboardJiraIssueLimitSpinner.getValueFactory().setValue(settings.dashboardJiraIssueLimit());
        jiraChangeNotificationsToggle.setSelected(settings.jiraChangeNotificationsEnabled());
    }

    @Override
    protected void save() {
        final WorkspaceSettings updated = new WorkspaceSettings(
                showDashboardToggle.isSelected(),
                settings.dashboardCollapsed(),
                alwaysOnTopToggle.isSelected(),
                tasksFileField.getText(),
                dashboardTaskLimitSpinner.getValue(),
                taskGroupsField.getText(),
                settings.openCodeHostname(),
                settings.openCodePort(),
                settings.openCodeWorkingDirectory(),
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
                calendarNotificationsToggle.isSelected(),
                calendarNotificationMinutesSpinner.getValue(),
                calendarChangeNotificationsToggle.isSelected(),
                githubChangeNotificationsToggle.isSelected(),
                jiraChangeNotificationsToggle.isSelected(),
                settings.splitMode()
        );
        BackendApi.updateWorkspaceSettings(updated);
        settings = updated;
    }

    @Override
    protected List<Node> generateContent() {
        settings = BackendApi.getWorkspaceSettings();

        final var header = createTextSeparator("settings.dashboard.title", Styles.TITLE_3);

        // ── General ──
        final var generalHeader = createTextSeparator("settings.dashboard.general.section", Styles.TITLE_4);
        showDashboardToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        alwaysOnTopToggle = createResizableRegion(ToggleSwitch::new, settingsBox);

        // ── Music ──
        final var musicHeader = createTextSeparator("settings.dashboard.music.section", Styles.TITLE_4);
        showDashboardMusicToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        dashboardMediaRefreshSpinner = createResizableEditableSpinner(1, 3600, settings.dashboardMediaRefreshSeconds(), settingsBox);
        final var mediaRefreshTile = createTile("settings.workspace.mediaRefresh.title", "settings.workspace.mediaRefresh.description");
        mediaRefreshTile.setAction(dashboardMediaRefreshSpinner);

        // ── Calendar ──
        final var calendarHeader = createTextSeparator("settings.dashboard.calendar.section", Styles.TITLE_4);
        showDashboardCalendarToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        calendarHideAllDayToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        calendarShowOnlyCurrentAndFutureToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        dashboardCalendarRefreshSpinner = createResizableEditableSpinner(1, 3600, settings.dashboardCalendarRefreshSeconds(), settingsBox);
        final var calendarRefreshTile = createTile("settings.workspace.calendarRefresh.title", "settings.workspace.calendarRefresh.description");
        calendarRefreshTile.setAction(dashboardCalendarRefreshSpinner);
        calendarNotificationsToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        calendarNotificationMinutesSpinner = createResizableEditableSpinner(1, 30, settings.calendarNotificationMinutesBefore(), settingsBox);
        final var calendarNotificationMinutesTile = createTile("settings.workspace.calendarNotificationMinutes.title", "settings.workspace.calendarNotificationMinutes.description");
        calendarNotificationMinutesTile.setAction(calendarNotificationMinutesSpinner);
        calendarChangeNotificationsToggle = createResizableRegion(ToggleSwitch::new, settingsBox);

        // ── Tasks ──
        final var tasksHeader = createTextSeparator("settings.dashboard.tasks.section", Styles.TITLE_4);
        showDashboardTasksToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        tasksFileField = createResizableTextField(settingsBox);
        final Node tasksFilePicker = createFilePickerField(tasksFileField, this::chooseTasksFile);
        taskGroupsField = createResizableTextField(settingsBox);
        dashboardTaskLimitSpinner = createResizableEditableSpinner(1, 50, settings.dashboardTaskLimit(), settingsBox);
        final var taskLimitTile = createTile("settings.workspace.taskLimit.title", "settings.workspace.taskLimit.description");
        taskLimitTile.setAction(dashboardTaskLimitSpinner);
        dashboardTasksRefreshSpinner = createResizableEditableSpinner(1, 3600, settings.dashboardTasksRefreshSeconds(), settingsBox);
        final var tasksRefreshTile = createTile("settings.workspace.tasksRefresh.title", "settings.workspace.tasksRefresh.description");
        tasksRefreshTile.setAction(dashboardTasksRefreshSpinner);

        // ── GitHub ──
        final var githubHeader = createTextSeparator("settings.dashboard.github.section", Styles.TITLE_4);
        showDashboardGithubToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        githubTokenField = createResizablePasswordField(settingsBox);
        dashboardGithubRefreshSpinner = createResizableEditableSpinner(1, 3600, settings.dashboardGithubRefreshSeconds(), settingsBox);
        final var githubRefreshTile = createTile("settings.workspace.githubRefresh.title", "settings.workspace.githubRefresh.description");
        githubRefreshTile.setAction(dashboardGithubRefreshSpinner);
        dashboardGithubPrLimitSpinner = createResizableEditableSpinner(1, 50, settings.dashboardGithubPrLimit(), settingsBox);
        final var githubPrLimitTile = createTile("settings.workspace.githubPrLimit.title", "settings.workspace.githubPrLimit.description");
        githubPrLimitTile.setAction(dashboardGithubPrLimitSpinner);
        githubChangeNotificationsToggle = createResizableRegion(ToggleSwitch::new, settingsBox);

        // ── Jira ──
        final var jiraHeader = createTextSeparator("settings.dashboard.jira.section", Styles.TITLE_4);
        showDashboardJiraToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        jiraEmailField = createResizableTextField(settingsBox);
        jiraApiTokenField = createResizablePasswordField(settingsBox);
        dashboardJiraRefreshSpinner = createResizableEditableSpinner(1, 3600, settings.dashboardJiraRefreshSeconds(), settingsBox);
        final var jiraRefreshTile = createTile("settings.workspace.jiraRefresh.title", "settings.workspace.jiraRefresh.description");
        jiraRefreshTile.setAction(dashboardJiraRefreshSpinner);
        dashboardJiraIssueLimitSpinner = createResizableEditableSpinner(1, 50, settings.dashboardJiraIssueLimit(), settingsBox);
        final var jiraIssueLimitTile = createTile("settings.workspace.jiraIssueLimit.title", "settings.workspace.jiraIssueLimit.description");
        jiraIssueLimitTile.setAction(dashboardJiraIssueLimitSpinner);
        jiraChangeNotificationsToggle = createResizableRegion(ToggleSwitch::new, settingsBox);

        return List.of(
                header,
                // General
                generalHeader,
                tileFor(showDashboardToggle, "settings.workspace.dashboard.title", "settings.workspace.dashboard.description"),
                tileFor(alwaysOnTopToggle, "settings.workspace.ontop.title", "settings.workspace.ontop.description"),
                // Music
                musicHeader,
                tileFor(showDashboardMusicToggle, "settings.workspace.showMusic.title", "settings.workspace.showMusic.description"),
                mediaRefreshTile,
                // Calendar
                calendarHeader,
                tileFor(showDashboardCalendarToggle, "settings.workspace.showCalendar.title", "settings.workspace.showCalendar.description"),
                tileFor(calendarHideAllDayToggle, "settings.workspace.calendarHideAllDay.title", "settings.workspace.calendarHideAllDay.description"),
                tileFor(calendarShowOnlyCurrentAndFutureToggle, "settings.workspace.calendarOnlyFuture.title", "settings.workspace.calendarOnlyFuture.description"),
                calendarRefreshTile,
                tileFor(calendarNotificationsToggle, "settings.workspace.calendarNotifications.title", "settings.workspace.calendarNotifications.description"),
                calendarNotificationMinutesTile,
                tileFor(calendarChangeNotificationsToggle, "settings.workspace.calendarChangeNotifications.title", "settings.workspace.calendarChangeNotifications.description"),
                // Tasks
                tasksHeader,
                tileFor(showDashboardTasksToggle, "settings.workspace.showTasks.title", "settings.workspace.showTasks.description"),
                tileFor(tasksFilePicker, "settings.workspace.tasksFile.title", "settings.workspace.tasksFile.description"),
                tileFor(taskGroupsField, "settings.workspace.taskGroups.title", "settings.workspace.taskGroups.description"),
                taskLimitTile,
                tasksRefreshTile,
                // GitHub
                githubHeader,
                tileFor(showDashboardGithubToggle, "settings.workspace.showGithub.title", "settings.workspace.showGithub.description"),
                tileFor(githubTokenField, "settings.workspace.github.token.title", "settings.workspace.github.token.description"),
                githubRefreshTile,
                githubPrLimitTile,
                tileFor(githubChangeNotificationsToggle, "settings.workspace.githubChangeNotifications.title", "settings.workspace.githubChangeNotifications.description"),
                // Jira
                jiraHeader,
                tileFor(showDashboardJiraToggle, "settings.workspace.showJira.title", "settings.workspace.showJira.description"),
                tileFor(jiraEmailField, "settings.workspace.jira.email.title", "settings.workspace.jira.email.description"),
                tileFor(jiraApiTokenField, "settings.workspace.jira.token.title", "settings.workspace.jira.token.description"),
                jiraRefreshTile,
                jiraIssueLimitTile,
                tileFor(jiraChangeNotificationsToggle, "settings.workspace.jiraChangeNotifications.title", "settings.workspace.jiraChangeNotifications.description")
        );
    }

    private Node tileFor(final Node action, final String titleKey, final String descriptionKey) {
        final var tile = createTile(titleKey, descriptionKey);
        tile.setAction(action);
        return tile;
    }

    private String orEmpty(final String value) {
        return value == null ? "" : value;
    }

    private Node createFilePickerField(final TextField field, final Runnable onPick) {
        final Button browseButton = createButton(Feather.FOLDER, e -> onPick.run());
        browseButton.setFocusTraversable(false);
        final HBox box = new HBox(8, field, browseButton);
        HBox.setHgrow(field, Priority.ALWAYS);
        return box;
    }

    private void chooseTasksFile() {
        final FileChooser chooser = new FileChooser();
        chooser.setTitle("Tasks file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Markdown files", "*.md"));
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        applyInitialDirectory(chooser, tasksFileField.getText());
        final File selected = chooser.showOpenDialog(settingsBox.getScene() == null ? null : settingsBox.getScene().getWindow());
        if (selected != null) {
            tasksFileField.setText(selected.getAbsolutePath());
        }
    }

    private void applyInitialDirectory(final FileChooser chooser, final String currentValue) {
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
        // If it's a file, use its parent directory
        final Path directory = Files.isRegularFile(path) ? path.getParent() : existingDirectory(path);
        return directory != null ? directory : existingDirectory(Path.of(System.getProperty("user.home", ".")));
    }

    private Path existingDirectory(final Path path) {
        if (path == null) {
            return null;
        }
        return Files.isDirectory(path) ? path : null;
    }
}
