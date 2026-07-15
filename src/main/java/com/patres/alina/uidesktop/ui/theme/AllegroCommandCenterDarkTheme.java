package com.patres.alina.uidesktop.ui.theme;

import atlantafx.base.theme.Theme;

/** Dark companion palette for {@link AllegroCommandCenterTheme}. */
public final class AllegroCommandCenterDarkTheme implements Theme {

    @Override
    public String getName() {
        return "Allegro Command Center Dark";
    }

    @Override
    public String getUserAgentStylesheet() {
        return "/com/patres/alina/uidesktop/assets/styles/allegro-command-center-dark-theme.css";
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
