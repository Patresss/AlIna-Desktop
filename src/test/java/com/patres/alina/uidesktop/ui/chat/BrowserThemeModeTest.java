package com.patres.alina.uidesktop.ui.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrowserThemeModeTest {

    @Test
    void buildsDarkModeScript() {
        assertThat(Browser.buildThemeModeScript(true))
                .isEqualTo("document.documentElement.classList.toggle('theme-dark', true);");
    }

    @Test
    void buildsLightModeScript() {
        assertThat(Browser.buildThemeModeScript(false))
                .isEqualTo("document.documentElement.classList.toggle('theme-dark', false);");
    }
}
