package com.patres.alina.uidesktop.ui.dashboard;

/** Content-aware vertical allocation for the dashboard above the chat workbench. */
public final class DashboardHeightPolicy {

    public static final double COLLAPSED_HEIGHT = 30;
    public static final double MINIMUM_CHAT_WORKSPACE = 210;

    private DashboardHeightPolicy() {
    }

    public static double resolve(double availableHeight, double preferredContentHeight, boolean collapsed) {
        if (collapsed) {
            return COLLAPSED_HEIGHT;
        }
        final double usableForDashboard = Math.max(COLLAPSED_HEIGHT, availableHeight - MINIMUM_CHAT_WORKSPACE);
        return Math.max(COLLAPSED_HEIGHT, Math.min(preferredContentHeight, usableForDashboard));
    }
}
