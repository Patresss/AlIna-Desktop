package com.patres.alina.uidesktop.ui.dashboard;

import com.patres.alina.common.settings.DashboardLayoutSettings;

/** Responsive modes used by the dashboard bento grid. */
enum DashboardLayoutMode {
    SINGLE_COLUMN,
    TWO_COLUMNS;

    static final double TWO_COLUMN_BREAKPOINT = DashboardLayoutSettings.DEFAULT_TWO_COLUMN_BREAKPOINT;

    static DashboardLayoutMode forWidth(double width) {
        return forWidth(width, DashboardLayoutSettings.DEFAULT_TWO_COLUMN_BREAKPOINT);
    }

    static DashboardLayoutMode forWidth(double width, int twoColumnBreakpoint) {
        return width >= twoColumnBreakpoint ? TWO_COLUMNS : SINGLE_COLUMN;
    }
}
