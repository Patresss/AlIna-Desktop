package com.patres.alina.uidesktop.ui.dashboard;

/** Responsive modes used by the dashboard bento grid. */
enum DashboardLayoutMode {
    SINGLE_COLUMN,
    TWO_COLUMNS;

    static final double TWO_COLUMN_BREAKPOINT = 680;

    static DashboardLayoutMode forWidth(double width) {
        return width >= TWO_COLUMN_BREAKPOINT ? TWO_COLUMNS : SINGLE_COLUMN;
    }
}
