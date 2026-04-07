package com.patres.alina.common.settings;

public record WorkspaceSettings(
        boolean showDashboard,
        boolean dashboardCollapsed,
        boolean keepWindowAlwaysOnTop,
        String tasksFile,
        int dashboardTaskLimit,
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
        boolean showDashboardJira
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

    public WorkspaceSettings() {
        this(
                true,
                false,
                true,
                DEFAULT_TASKS_FILE,
                DEFAULT_DASHBOARD_TASK_LIMIT,
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
                true
        );
    }

    public WorkspaceSettings {
        tasksFile = defaultIfBlank(tasksFile, DEFAULT_TASKS_FILE);
        dashboardTaskLimit = dashboardTaskLimit > 0 ? dashboardTaskLimit : DEFAULT_DASHBOARD_TASK_LIMIT;
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
    }

    public WorkspaceSettings withKeepWindowAlwaysOnTop(final boolean value) {
        return new WorkspaceSettings(
                showDashboard,
                dashboardCollapsed,
                value,
                tasksFile,
                dashboardTaskLimit,
                openCodeHostname,
                openCodePort,
                openCodeWorkingDirectory,
                githubToken,
                dashboardTasksRefreshSeconds,
                dashboardGithubRefreshSeconds,
                dashboardMediaRefreshSeconds,
                dashboardGithubPrLimit,
                dashboardJiraRefreshSeconds,
                dashboardJiraIssueLimit,
                jiraEmail,
                jiraApiToken,
                showDashboardMusic,
                showDashboardTasks,
                showDashboardGithub,
                showDashboardJira
        );
    }

    public WorkspaceSettings withDashboardCollapsed(final boolean value) {
        return new WorkspaceSettings(
                showDashboard,
                value,
                keepWindowAlwaysOnTop,
                tasksFile,
                dashboardTaskLimit,
                openCodeHostname,
                openCodePort,
                openCodeWorkingDirectory,
                githubToken,
                dashboardTasksRefreshSeconds,
                dashboardGithubRefreshSeconds,
                dashboardMediaRefreshSeconds,
                dashboardGithubPrLimit,
                dashboardJiraRefreshSeconds,
                dashboardJiraIssueLimit,
                jiraEmail,
                jiraApiToken,
                showDashboardMusic,
                showDashboardTasks,
                showDashboardGithub,
                showDashboardJira
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
