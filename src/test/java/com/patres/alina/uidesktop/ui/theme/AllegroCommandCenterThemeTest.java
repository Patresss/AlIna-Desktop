package com.patres.alina.uidesktop.ui.theme;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class AllegroCommandCenterThemeTest {

    @Test
    void exposesLightAndDarkThemeResources() {
        final var light = new AllegroCommandCenterTheme();
        final var dark = new AllegroCommandCenterDarkTheme();

        assertThat(light.getName()).isEqualTo("Allegro Command Center");
        assertThat(light.isDarkMode()).isFalse();
        assertThat(getClass().getResource(light.getUserAgentStylesheet())).isNotNull();

        assertThat(dark.getName()).isEqualTo("Allegro Command Center Dark");
        assertThat(dark.isDarkMode()).isTrue();
        assertThat(getClass().getResource(dark.getUserAgentStylesheet())).isNotNull();
    }

    @Test
    void repositoryOffersBothThemes() {
        assertThat(new ThemeRepository().getAll())
                .extracting(SamplerTheme::getName)
                .contains("Allegro Command Center", "Allegro Command Center Dark");
    }

    @Test
    void registersBothThemesAsProjectThemes() {
        assertThat(ThemeManager.PROJECT_THEMES)
                .contains(AllegroCommandCenterTheme.class, AllegroCommandCenterDarkTheme.class);
    }

    @Test
    void exposesOfficialAccentAndExpectedSurfaceTokensToWebView() throws IOException {
        final var lightColors = new SamplerTheme(new AllegroCommandCenterTheme()).parseColors();
        final var darkColors = new SamplerTheme(new AllegroCommandCenterDarkTheme()).parseColors();

        assertThat(lightColors)
                .containsKeys(
                        "-color-accent-5",
                        "-color-accent-fg",
                        "-color-accent-emphasis",
                        "-color-accent-muted",
                        "-color-bg-default",
                        "-color-bg-inset"
                );
        assertThat(lightColors.get("-color-accent-5").trim()).isEqualTo("#ff5a00");
        assertThat(lightColors.get("-color-accent-fg").trim()).isEqualTo("#ff5a00");
        assertThat(lightColors.get("-color-accent-emphasis").trim()).isEqualTo("#ff5a00");
        assertThat(lightColors.get("-color-accent-muted").trim()).isEqualTo("rgba(255, 90, 0, 0.38)");
        assertThat(lightColors.get("-color-bg-default").trim()).isEqualTo("#f7f7f8");
        assertThat(lightColors.get("-color-bg-inset").trim()).isEqualTo("#e6e6e6");

        assertThat(darkColors)
                .containsKeys(
                        "-color-accent-5",
                        "-color-accent-fg",
                        "-color-accent-emphasis",
                        "-color-accent-muted",
                        "-color-bg-default",
                        "-color-bg-inset"
                );
        assertThat(darkColors.get("-color-accent-5").trim()).isEqualTo("#ff5a00");
        assertThat(darkColors.get("-color-accent-fg").trim()).isEqualTo("#ff5a00");
        assertThat(darkColors.get("-color-accent-emphasis").trim()).isEqualTo("#ff5a00");
        assertThat(darkColors.get("-color-accent-muted").trim()).isEqualTo("rgba(255, 90, 0, 0.42)");
        assertThat(darkColors.get("-color-bg-default").trim()).isEqualTo("#111112");
        assertThat(darkColors.get("-color-bg-inset").trim()).isEqualTo("#090909");
    }
}
