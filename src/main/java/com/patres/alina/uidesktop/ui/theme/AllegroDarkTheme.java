/* SPDX-License-Identifier: MIT */

package com.patres.alina.uidesktop.ui.theme;

import atlantafx.base.theme.Theme;

public class AllegroDarkTheme implements Theme {

    public AllegroDarkTheme() {
        // Default constructor
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Allegro Dark";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserAgentStylesheet() {
        return "/com/patres/alina/uidesktop/assets/styles/allegro-dark-theme.css";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserAgentStylesheetBSS() {
        return "/com/patres/alina/uidesktop/assets/styles/allegro-dark-theme.bss";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDarkMode() {
        return true;
    }
}
