/* SPDX-License-Identifier: MIT */

package com.patres.alina.uidesktop.ui.theme;

import atlantafx.base.theme.Theme;

public class AllegroTheme implements Theme {

    public AllegroTheme() {
        // Default constructor
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "Allegro";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserAgentStylesheet() {
        return "/com/patres/alina/uidesktop/assets/styles/allegro-theme.css";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserAgentStylesheetBSS() {
        return "/com/patres/alina/uidesktop/assets/styles/allegro-theme.bss";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDarkMode() {
        return false;
    }
}
