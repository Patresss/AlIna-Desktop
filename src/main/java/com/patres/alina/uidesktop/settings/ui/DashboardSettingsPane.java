package com.patres.alina.uidesktop.settings.ui;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;

import java.util.List;

import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTile;
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

    // Tasks
    private ToggleSwitch showDashboardTasksToggle;
    private TextField tasksFileField;
    private Spinner<Integer> dashboardTaskLimitSpinner;
    private Spinner<Integer> dashboardTasksRefreshSpinner;

    // GitHub
    private ToggleSwitch showDashboardGithubToggle;
    private TextField githubTokenField;
    private Spinner<Integer> dashboardGithubRefreshSpinner;
    private Spinner<Integer> dashboardGithubPrLimitSpinner;

    // Jira
    private ToggleSwitch showDashboardJiraToggle;
    private TextField jiraEmailField;
    private TextField jiraApiTokenField;
    private Spinner<Integer> dashboardJiraRefreshSpinner;
    private Spinner<Integer> dashboardJiraIssueLimitSpinner;

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

        // Tasks
        showDashboardTasksToggle.setSelected(settings.showDashboardTasks());
        tasksFileField.setText(orEmpty(settings.tasksFile()));
        dashboardTaskLimitSpinner.getValueFactory().setValue(settings.dashboardTaskLimit());
        dashboardTasksRefreshSpinner.getValueFactory().setValue(settings.dashboardTasksRefreshSeconds());

        // GitHub
        showDashboardGithubToggle.setSelected(settings.showDashboardGithub());
        githubTokenField.setText(orEmpty(settings.githubToken()));
        dashboardGithubRefreshSpinner.getValueFactory().setValue(settings.dashboardGithubRefreshSeconds());
        dashboardGithubPrLimitSpinner.getValueFactory().setValue(settings.dashboardGithubPrLimit());

        // Jira
        showDashboardJiraToggle.setSelected(settings.showDashboardJira());
        jiraEmailField.setText(orEmpty(settings.jiraEmail()));
        jiraApiTokenField.setText(orEmpty(settings.jiraApiToken()));
        dashboardJiraRefreshSpinner.getValueFactory().setValue(settings.dashboardJiraRefreshSeconds());
        dashboardJiraIssueLimitSpinner.getValueFactory().setValue(settings.dashboardJiraIssueLimit());
    }

    @Override
    protected void save() {
        final WorkspaceSettings updated = new WorkspaceSettings(
                showDashboardToggle.isSelected(),
                settings.dashboardCollapsed(),
                alwaysOnTopToggle.isSelected(),
                tasksFileField.getText(),
                dashboardTaskLimitSpinner.getValue(),
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
                calendarNotificationMinutesSpinner.getValue()
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
        dashboardMediaRefreshSpinner = createResizableRegion(() -> new Spinner<>(1, 60, settings.dashboardMediaRefreshSeconds()), settingsBox);
        final var mediaRefreshTile = createTile("settings.workspace.mediaRefresh.title", "settings.workspace.mediaRefresh.description");
        mediaRefreshTile.setAction(dashboardMediaRefreshSpinner);

        // ── Calendar ──
        final var calendarHeader = createTextSeparator("settings.dashboard.calendar.section", Styles.TITLE_4);
        showDashboardCalendarToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        calendarHideAllDayToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        calendarShowOnlyCurrentAndFutureToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        dashboardCalendarRefreshSpinner = createResizableRegion(() -> new Spinner<>(60, 1800, settings.dashboardCalendarRefreshSeconds()), settingsBox);
        final var calendarRefreshTile = createTile("settings.workspace.calendarRefresh.title", "settings.workspace.calendarRefresh.description");
        calendarRefreshTile.setAction(dashboardCalendarRefreshSpinner);
        calendarNotificationsToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        calendarNotificationMinutesSpinner = createResizableRegion(() -> new Spinner<>(1, 30, settings.calendarNotificationMinutesBefore()), settingsBox);
        final var calendarNotificationMinutesTile = createTile("settings.workspace.calendarNotificationMinutes.title", "settings.workspace.calendarNotificationMinutes.description");
        calendarNotificationMinutesTile.setAction(calendarNotificationMinutesSpinner);

        // ── Tasks ──
        final var tasksHeader = createTextSeparator("settings.dashboard.tasks.section", Styles.TITLE_4);
        showDashboardTasksToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        tasksFileField = createResizableTextField(settingsBox);
        dashboardTaskLimitSpinner = createResizableRegion(() -> new Spinner<>(1, 12, settings.dashboardTaskLimit()), settingsBox);
        final var taskLimitTile = createTile("settings.workspace.taskLimit.title", "settings.workspace.taskLimit.description");
        taskLimitTile.setAction(dashboardTaskLimitSpinner);
        dashboardTasksRefreshSpinner = createResizableRegion(() -> new Spinner<>(5, 300, settings.dashboardTasksRefreshSeconds()), settingsBox);
        final var tasksRefreshTile = createTile("settings.workspace.tasksRefresh.title", "settings.workspace.tasksRefresh.description");
        tasksRefreshTile.setAction(dashboardTasksRefreshSpinner);

        // ── GitHub ──
        final var githubHeader = createTextSeparator("settings.dashboard.github.section", Styles.TITLE_4);
        showDashboardGithubToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        githubTokenField = createResizableTextField(settingsBox);
        dashboardGithubRefreshSpinner = createResizableRegion(() -> new Spinner<>(10, 600, settings.dashboardGithubRefreshSeconds()), settingsBox);
        final var githubRefreshTile = createTile("settings.workspace.githubRefresh.title", "settings.workspace.githubRefresh.description");
        githubRefreshTile.setAction(dashboardGithubRefreshSpinner);
        dashboardGithubPrLimitSpinner = createResizableRegion(() -> new Spinner<>(1, 50, settings.dashboardGithubPrLimit()), settingsBox);
        final var githubPrLimitTile = createTile("settings.workspace.githubPrLimit.title", "settings.workspace.githubPrLimit.description");
        githubPrLimitTile.setAction(dashboardGithubPrLimitSpinner);

        // ── Jira ──
        final var jiraHeader = createTextSeparator("settings.dashboard.jira.section", Styles.TITLE_4);
        showDashboardJiraToggle = createResizableRegion(ToggleSwitch::new, settingsBox);
        jiraEmailField = createResizableTextField(settingsBox);
        jiraApiTokenField = createResizableTextField(settingsBox);
        dashboardJiraRefreshSpinner = createResizableRegion(() -> new Spinner<>(30, 600, settings.dashboardJiraRefreshSeconds()), settingsBox);
        final var jiraRefreshTile = createTile("settings.workspace.jiraRefresh.title", "settings.workspace.jiraRefresh.description");
        jiraRefreshTile.setAction(dashboardJiraRefreshSpinner);
        dashboardJiraIssueLimitSpinner = createResizableRegion(() -> new Spinner<>(1, 50, settings.dashboardJiraIssueLimit()), settingsBox);
        final var jiraIssueLimitTile = createTile("settings.workspace.jiraIssueLimit.title", "settings.workspace.jiraIssueLimit.description");
        jiraIssueLimitTile.setAction(dashboardJiraIssueLimitSpinner);

        return List.of(
                header,
                // General
                generalHeader,
                tileFor(showDashboardToggle, "settings.workspace.dashboard.title", "settings.workspace.dashboard.description"),
                tileFor(alwaysOnTopToggle, "settings.workspace.ontop.title", "settings.workspace.ontop.description"),
                new Separator(),
                // Music
                musicHeader,
                tileFor(showDashboardMusicToggle, "settings.workspace.showMusic.title", "settings.workspace.showMusic.description"),
                mediaRefreshTile,
                new Separator(),
                // Calendar
                calendarHeader,
                tileFor(showDashboardCalendarToggle, "settings.workspace.showCalendar.title", "settings.workspace.showCalendar.description"),
                tileFor(calendarHideAllDayToggle, "settings.workspace.calendarHideAllDay.title", "settings.workspace.calendarHideAllDay.description"),
                tileFor(calendarShowOnlyCurrentAndFutureToggle, "settings.workspace.calendarOnlyFuture.title", "settings.workspace.calendarOnlyFuture.description"),
                calendarRefreshTile,
                tileFor(calendarNotificationsToggle, "settings.workspace.calendarNotifications.title", "settings.workspace.calendarNotifications.description"),
                calendarNotificationMinutesTile,
                new Separator(),
                // Tasks
                tasksHeader,
                tileFor(showDashboardTasksToggle, "settings.workspace.showTasks.title", "settings.workspace.showTasks.description"),
                tileFor(tasksFileField, "settings.workspace.tasksFile.title", "settings.workspace.tasksFile.description"),
                taskLimitTile,
                tasksRefreshTile,
                new Separator(),
                // GitHub
                githubHeader,
                tileFor(showDashboardGithubToggle, "settings.workspace.showGithub.title", "settings.workspace.showGithub.description"),
                tileFor(githubTokenField, "settings.workspace.github.token.title", "settings.workspace.github.token.description"),
                githubRefreshTile,
                githubPrLimitTile,
                new Separator(),
                // Jira
                jiraHeader,
                tileFor(showDashboardJiraToggle, "settings.workspace.showJira.title", "settings.workspace.showJira.description"),
                tileFor(jiraEmailField, "settings.workspace.jira.email.title", "settings.workspace.jira.email.description"),
                tileFor(jiraApiTokenField, "settings.workspace.jira.token.title", "settings.workspace.jira.token.description"),
                jiraRefreshTile,
                jiraIssueLimitTile
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
}
