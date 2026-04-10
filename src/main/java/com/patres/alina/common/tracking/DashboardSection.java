package com.patres.alina.common.tracking;

/**
 * Identifies each trackable section of the dashboard.
 * Used as a key when storing and retrieving section snapshots.
 */
public enum DashboardSection {

    CALENDAR("calendar", "dashboard.changes.section.calendar"),
    JIRA("jira", "dashboard.changes.section.jira"),
    GITHUB("github", "dashboard.changes.section.github");

    private final String id;
    private final String titleKey;

    DashboardSection(final String id, final String titleKey) {
        this.id = id;
        this.titleKey = titleKey;
    }

    /** Stable identifier used as the JSON key in the state snapshot file. */
    public String id() {
        return id;
    }

    /** i18n bundle key for the section title in notifications. */
    public String titleKey() {
        return titleKey;
    }
}
