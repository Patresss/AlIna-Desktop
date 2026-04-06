package com.patres.alina.common.settings;

public record WorkspaceSettings(
        boolean showDashboard,
        boolean dashboardCollapsed,
        boolean keepWindowAlwaysOnTop,
        String tasksFile,
        int dashboardTaskLimit,
        String openCodeHostname,
        int openCodePort,
        String openCodeWorkingDirectory
) {

    public static final String DEFAULT_TASKS_FILE = "profile/default/focus.md";
    public static final int DEFAULT_DASHBOARD_TASK_LIMIT = 6;
    public static final String DEFAULT_OPENCODE_HOSTNAME = "127.0.0.1";
    public static final int DEFAULT_OPENCODE_PORT = 4096;
    public static final String DEFAULT_OPENCODE_WORKING_DIRECTORY = System.getProperty("user.home", ".");

    public WorkspaceSettings() {
        this(
                true,
                false,
                true,
                DEFAULT_TASKS_FILE,
                DEFAULT_DASHBOARD_TASK_LIMIT,
                DEFAULT_OPENCODE_HOSTNAME,
                DEFAULT_OPENCODE_PORT,
                DEFAULT_OPENCODE_WORKING_DIRECTORY
        );
    }

    public WorkspaceSettings {
        tasksFile = defaultIfBlank(tasksFile, DEFAULT_TASKS_FILE);
        dashboardTaskLimit = dashboardTaskLimit > 0 ? dashboardTaskLimit : DEFAULT_DASHBOARD_TASK_LIMIT;
        openCodeHostname = defaultIfBlank(openCodeHostname, DEFAULT_OPENCODE_HOSTNAME);
        openCodePort = openCodePort > 0 ? openCodePort : DEFAULT_OPENCODE_PORT;
        openCodeWorkingDirectory = defaultIfBlank(openCodeWorkingDirectory, DEFAULT_OPENCODE_WORKING_DIRECTORY);
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
                openCodeWorkingDirectory
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
                openCodeWorkingDirectory
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
