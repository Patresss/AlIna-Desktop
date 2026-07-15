package com.patres.alina.uidesktop.ui.theme;

import atlantafx.base.theme.Theme;

/** Calm Command Center structure with a restrained Allegro light palette. */
public final class AllegroCommandCenterTheme implements Theme {

    @Override
    public String getName() {
        return "Allegro Command Center";
    }

    @Override
    public String getUserAgentStylesheet() {
        return "/com/patres/alina/uidesktop/assets/styles/allegro-command-center-theme.css";
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
