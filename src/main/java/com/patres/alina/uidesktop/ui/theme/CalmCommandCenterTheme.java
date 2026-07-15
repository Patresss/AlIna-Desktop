package com.patres.alina.uidesktop.ui.theme;

import atlantafx.base.theme.Theme;

/**
 * AlIna's calm, high-contrast light palette built on the complete Atlantafx
 * control foundation shipped with the application.
 */
public final class CalmCommandCenterTheme implements Theme {

    @Override
    public String getName() {
        return "Calm Command Center";
    }

    @Override
    public String getUserAgentStylesheet() {
        return "/com/patres/alina/uidesktop/assets/styles/calm-command-center-theme.css";
    }

    @Override
    public String getUserAgentStylesheetBSS() {
        return null;
    }

    @Override
    public boolean isDarkMode() {
        return false;
    }
}
