package com.patres.alina.common.settings;

/** Stable dashboard card identifiers with legacy-compatible layout defaults. */
public enum DashboardCardId {
    MUSIC("music", 10, false),
    TASKS("tasks", 20, false),
    UPCOMING_EVENT("upcomingEvent", 25, true),
    CALENDAR("calendar", 30, true),
    GITHUB("github", 40, true),
    JIRA("jira", 50, true),
    OBSIDIAN("obsidian", 60, true);

    private final String key;
    private final int defaultOrder;
    private final boolean defaultCanUseHalfWidth;

    DashboardCardId(String key, int defaultOrder, boolean defaultCanUseHalfWidth) {
        this.key = key;
        this.defaultOrder = defaultOrder;
        this.defaultCanUseHalfWidth = defaultCanUseHalfWidth;
    }

    public String key() {
        return key;
    }

    public int defaultOrder() {
        return defaultOrder;
    }

    public boolean defaultCanUseHalfWidth() {
        return defaultCanUseHalfWidth;
    }
}
