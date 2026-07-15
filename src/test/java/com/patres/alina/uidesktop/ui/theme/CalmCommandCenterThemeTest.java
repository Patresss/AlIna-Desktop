package com.patres.alina.uidesktop.ui.theme;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalmCommandCenterThemeTest {

    @Test
    void exposesLightAndDarkThemeResources() {
        final var light = new CalmCommandCenterTheme();
        final var dark = new CalmCommandCenterDarkTheme();

        assertThat(light.getName()).isEqualTo("Calm Command Center");
        assertThat(light.isDarkMode()).isFalse();
        assertThat(getClass().getResource(light.getUserAgentStylesheet())).isNotNull();

        assertThat(dark.getName()).isEqualTo("Calm Command Center Dark");
        assertThat(dark.isDarkMode()).isTrue();
        assertThat(getClass().getResource(dark.getUserAgentStylesheet())).isNotNull();
    }

    @Test
    void repositoryOffersBothCommandCenterThemes() {
        assertThat(new ThemeRepository().getAll())
                .extracting(SamplerTheme::getName)
                .contains("Calm Command Center", "Calm Command Center Dark");
    }
}
