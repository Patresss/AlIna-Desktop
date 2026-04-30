package com.patres.alina.common.settings;

public record WorkspaceSettings(
        boolean showDashboard,
        boolean dashboardCollapsed,
        boolean keepWindowAlwaysOnTop,
        String tasksFile,
        int dashboardTaskLimit,
        String taskGroups,
        String openCodeHostname,
        int openCodePort,
        String openCodeWorkingDirectory,
        String githubToken,
        int dashboardTasksRefreshSeconds,
        int dashboardGithubRefreshSeconds,
        int dashboardMediaRefreshSeconds,
        int dashboardGithubPrLimit,
        int dashboardJiraRefreshSeconds,
        int dashboardJiraIssueLimit,
        String jiraEmail,
        String jiraApiToken,
        boolean showDashboardMusic,
        boolean showDashboardTasks,
        boolean showDashboardGithub,
        boolean showDashboardJira,
        boolean showDashboardCalendar,
        int dashboardCalendarRefreshSeconds,
        boolean calendarHideAllDayEvents,
        boolean calendarShowOnlyCurrentAndFuture,
        boolean calendarNotificationsEnabled,
        int calendarNotificationMinutesBefore,
        boolean calendarChangeNotificationsEnabled,
        boolean githubChangeNotificationsEnabled,
        boolean jiraChangeNotificationsEnabled,
        boolean splitMode,
        String calendarAiPrompt,
        String tasksAiPrompt,
        String jiraAiPrompt,
        String githubAiPrompt,
        // Obsidian
        boolean showDashboardObsidian,
        String obsidianCliPath,
        String obsidianVaultName,
        int dashboardObsidianNoteLimit,
        int dashboardObsidianRefreshSeconds,
        boolean obsidianChangeNotificationsEnabled,
        String obsidianAiPrompt,
        String obsidianExcludePatterns
) {

    public static final String DEFAULT_TASKS_FILE = "profile/default/focus.md";
    public static final int DEFAULT_DASHBOARD_TASK_LIMIT = 6;
    public static final String DEFAULT_OPENCODE_HOSTNAME = "127.0.0.1";
    public static final int DEFAULT_OPENCODE_PORT = 4096;
    public static final String DEFAULT_OPENCODE_WORKING_DIRECTORY = System.getProperty("user.home", ".");
    public static final int DEFAULT_DASHBOARD_TASKS_REFRESH_SECONDS = 15;
    public static final int DEFAULT_DASHBOARD_GITHUB_REFRESH_SECONDS = 60;
    public static final int DEFAULT_DASHBOARD_MEDIA_REFRESH_SECONDS = 5;
    public static final int DEFAULT_DASHBOARD_GITHUB_PR_LIMIT = 10;
    public static final int DEFAULT_DASHBOARD_JIRA_REFRESH_SECONDS = 120;
    public static final int DEFAULT_DASHBOARD_JIRA_ISSUE_LIMIT = 10;
    public static final int DEFAULT_DASHBOARD_CALENDAR_REFRESH_SECONDS = 300;
    public static final int DEFAULT_CALENDAR_NOTIFICATION_MINUTES_BEFORE = 1;
    public static final int DEFAULT_DASHBOARD_OBSIDIAN_NOTE_LIMIT = 5;
    public static final int DEFAULT_DASHBOARD_OBSIDIAN_REFRESH_SECONDS = 120;
    public static final String DEFAULT_OBSIDIAN_EXCLUDE_PATTERNS = "AGENTS.md,**/Memory/**,**/Templates/**";

    public WorkspaceSettings() {
        this(
                true,
                false,
                true,
                DEFAULT_TASKS_FILE,
                DEFAULT_DASHBOARD_TASK_LIMIT,
                "",
                DEFAULT_OPENCODE_HOSTNAME,
                DEFAULT_OPENCODE_PORT,
                DEFAULT_OPENCODE_WORKING_DIRECTORY,
                "",
                DEFAULT_DASHBOARD_TASKS_REFRESH_SECONDS,
                DEFAULT_DASHBOARD_GITHUB_REFRESH_SECONDS,
                DEFAULT_DASHBOARD_MEDIA_REFRESH_SECONDS,
                DEFAULT_DASHBOARD_GITHUB_PR_LIMIT,
                DEFAULT_DASHBOARD_JIRA_REFRESH_SECONDS,
                DEFAULT_DASHBOARD_JIRA_ISSUE_LIMIT,
                "",
                "",
                true,
                true,
                true,
                true,
                true,
                DEFAULT_DASHBOARD_CALENDAR_REFRESH_SECONDS,
                false,
                true,
                false,
                DEFAULT_CALENDAR_NOTIFICATION_MINUTES_BEFORE,
                true,
                true,
                true,
                false,
                "",
                "",
                "",
                "",
                // Obsidian defaults
                false,
                "",
                "",
                DEFAULT_DASHBOARD_OBSIDIAN_NOTE_LIMIT,
                DEFAULT_DASHBOARD_OBSIDIAN_REFRESH_SECONDS,
                false,
                "",
                DEFAULT_OBSIDIAN_EXCLUDE_PATTERNS
        );
    }

    public WorkspaceSettings {
        tasksFile = defaultIfBlank(tasksFile, DEFAULT_TASKS_FILE);
        dashboardTaskLimit = dashboardTaskLimit > 0 ? dashboardTaskLimit : DEFAULT_DASHBOARD_TASK_LIMIT;
        taskGroups = taskGroups == null ? "" : taskGroups.trim();
        openCodeHostname = defaultIfBlank(openCodeHostname, DEFAULT_OPENCODE_HOSTNAME);
        openCodePort = openCodePort > 0 ? openCodePort : DEFAULT_OPENCODE_PORT;
        openCodeWorkingDirectory = defaultIfBlank(openCodeWorkingDirectory, DEFAULT_OPENCODE_WORKING_DIRECTORY);
        githubToken = githubToken == null ? "" : githubToken.trim();
        dashboardTasksRefreshSeconds = dashboardTasksRefreshSeconds > 0 ? dashboardTasksRefreshSeconds : DEFAULT_DASHBOARD_TASKS_REFRESH_SECONDS;
        dashboardGithubRefreshSeconds = dashboardGithubRefreshSeconds > 0 ? dashboardGithubRefreshSeconds : DEFAULT_DASHBOARD_GITHUB_REFRESH_SECONDS;
        dashboardMediaRefreshSeconds = dashboardMediaRefreshSeconds > 0 ? dashboardMediaRefreshSeconds : DEFAULT_DASHBOARD_MEDIA_REFRESH_SECONDS;
        dashboardGithubPrLimit = dashboardGithubPrLimit > 0 ? dashboardGithubPrLimit : DEFAULT_DASHBOARD_GITHUB_PR_LIMIT;
        dashboardJiraRefreshSeconds = dashboardJiraRefreshSeconds > 0 ? dashboardJiraRefreshSeconds : DEFAULT_DASHBOARD_JIRA_REFRESH_SECONDS;
        dashboardJiraIssueLimit = dashboardJiraIssueLimit > 0 ? dashboardJiraIssueLimit : DEFAULT_DASHBOARD_JIRA_ISSUE_LIMIT;
        jiraEmail = jiraEmail == null ? "" : jiraEmail.trim();
        jiraApiToken = jiraApiToken == null ? "" : jiraApiToken.trim();
        dashboardCalendarRefreshSeconds = dashboardCalendarRefreshSeconds > 0 ? dashboardCalendarRefreshSeconds : DEFAULT_DASHBOARD_CALENDAR_REFRESH_SECONDS;
        calendarNotificationMinutesBefore = calendarNotificationMinutesBefore > 0 ? calendarNotificationMinutesBefore : DEFAULT_CALENDAR_NOTIFICATION_MINUTES_BEFORE;
        calendarAiPrompt = calendarAiPrompt == null ? "" : calendarAiPrompt.trim();
        tasksAiPrompt = tasksAiPrompt == null ? "" : tasksAiPrompt.trim();
        jiraAiPrompt = jiraAiPrompt == null ? "" : jiraAiPrompt.trim();
        githubAiPrompt = githubAiPrompt == null ? "" : githubAiPrompt.trim();
        // Obsidian
        obsidianCliPath = obsidianCliPath == null ? "" : obsidianCliPath.trim();
        obsidianVaultName = obsidianVaultName == null ? "" : obsidianVaultName.trim();
        dashboardObsidianNoteLimit = dashboardObsidianNoteLimit > 0 ? dashboardObsidianNoteLimit : DEFAULT_DASHBOARD_OBSIDIAN_NOTE_LIMIT;
        dashboardObsidianRefreshSeconds = dashboardObsidianRefreshSeconds > 0 ? dashboardObsidianRefreshSeconds : DEFAULT_DASHBOARD_OBSIDIAN_REFRESH_SECONDS;
        obsidianAiPrompt = obsidianAiPrompt == null ? "" : obsidianAiPrompt.trim();
        obsidianExcludePatterns = obsidianExcludePatterns == null ? "" : obsidianExcludePatterns.trim();
    }

    public WorkspaceSettings withKeepWindowAlwaysOnTop(final boolean value) {
        return new WorkspaceSettings(
                showDashboard, dashboardCollapsed, value, tasksFile, dashboardTaskLimit, taskGroups,
                openCodeHostname, openCodePort, openCodeWorkingDirectory, githubToken,
                dashboardTasksRefreshSeconds, dashboardGithubRefreshSeconds, dashboardMediaRefreshSeconds,
                dashboardGithubPrLimit, dashboardJiraRefreshSeconds, dashboardJiraIssueLimit,
                jiraEmail, jiraApiToken, showDashboardMusic, showDashboardTasks, showDashboardGithub,
                showDashboardJira, showDashboardCalendar, dashboardCalendarRefreshSeconds,
                calendarHideAllDayEvents, calendarShowOnlyCurrentAndFuture, calendarNotificationsEnabled,
                calendarNotificationMinutesBefore, calendarChangeNotificationsEnabled,
                githubChangeNotificationsEnabled, jiraChangeNotificationsEnabled, splitMode,
                calendarAiPrompt, tasksAiPrompt, jiraAiPrompt, githubAiPrompt,
                showDashboardObsidian, obsidianCliPath, obsidianVaultName,
                dashboardObsidianNoteLimit, dashboardObsidianRefreshSeconds,
                obsidianChangeNotificationsEnabled, obsidianAiPrompt, obsidianExcludePatterns
        );
    }

    public WorkspaceSettings withDashboardCollapsed(final boolean value) {
        return new WorkspaceSettings(
                showDashboard, value, keepWindowAlwaysOnTop, tasksFile, dashboardTaskLimit, taskGroups,
                openCodeHostname, openCodePort, openCodeWorkingDirectory, githubToken,
                dashboardTasksRefreshSeconds, dashboardGithubRefreshSeconds, dashboardMediaRefreshSeconds,
                dashboardGithubPrLimit, dashboardJiraRefreshSeconds, dashboardJiraIssueLimit,
                jiraEmail, jiraApiToken, showDashboardMusic, showDashboardTasks, showDashboardGithub,
                showDashboardJira, showDashboardCalendar, dashboardCalendarRefreshSeconds,
                calendarHideAllDayEvents, calendarShowOnlyCurrentAndFuture, calendarNotificationsEnabled,
                calendarNotificationMinutesBefore, calendarChangeNotificationsEnabled,
                githubChangeNotificationsEnabled, jiraChangeNotificationsEnabled, splitMode,
                calendarAiPrompt, tasksAiPrompt, jiraAiPrompt, githubAiPrompt,
                showDashboardObsidian, obsidianCliPath, obsidianVaultName,
                dashboardObsidianNoteLimit, dashboardObsidianRefreshSeconds,
                obsidianChangeNotificationsEnabled, obsidianAiPrompt, obsidianExcludePatterns
        );
    }

    public WorkspaceSettings withSplitMode(final boolean value) {
        return new WorkspaceSettings(
                showDashboard, dashboardCollapsed, keepWindowAlwaysOnTop, tasksFile, dashboardTaskLimit, taskGroups,
                openCodeHostname, openCodePort, openCodeWorkingDirectory, githubToken,
                dashboardTasksRefreshSeconds, dashboardGithubRefreshSeconds, dashboardMediaRefreshSeconds,
                dashboardGithubPrLimit, dashboardJiraRefreshSeconds, dashboardJiraIssueLimit,
                jiraEmail, jiraApiToken, showDashboardMusic, showDashboardTasks, showDashboardGithub,
                showDashboardJira, showDashboardCalendar, dashboardCalendarRefreshSeconds,
                calendarHideAllDayEvents, calendarShowOnlyCurrentAndFuture, calendarNotificationsEnabled,
                calendarNotificationMinutesBefore, calendarChangeNotificationsEnabled,
                githubChangeNotificationsEnabled, jiraChangeNotificationsEnabled, value,
                calendarAiPrompt, tasksAiPrompt, jiraAiPrompt, githubAiPrompt,
                showDashboardObsidian, obsidianCliPath, obsidianVaultName,
                dashboardObsidianNoteLimit, dashboardObsidianRefreshSeconds,
                obsidianChangeNotificationsEnabled, obsidianAiPrompt, obsidianExcludePatterns
        );
    }

    private static String defaultIfBlank(final String value, final String defaultValue) {
        final String normalized = normalize(value);
        return normalized == null ? defaultValue : normalized;
    }

    private static String normalize(final String value) {
        if (value == null) {
            return null;
        }
        final String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
