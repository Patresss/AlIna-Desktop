package com.patres.alina.uidesktop.ui.theme;

import atlantafx.base.theme.Theme;

/** Dark companion palette for {@link CalmCommandCenterTheme}. */
public final class CalmCommandCenterDarkTheme implements Theme {

    @Override
    public String getName() {
        return "Calm Command Center Dark";
    }

    @Override
    public String getUserAgentStylesheet() {
        return "/com/patres/alina/uidesktop/assets/styles/calm-command-center-dark-theme.css";
    }

    @Override
    public String getUserAgentStylesheetBSS() {
        return null;
    }

    @Override
    public boolean isDarkMode() {
        return true;
    }
}
