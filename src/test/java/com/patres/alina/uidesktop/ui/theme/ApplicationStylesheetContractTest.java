package com.patres.alina.uidesktop.ui.theme;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStylesheetContractTest {

    private static final String[] COMPONENT_STYLESHEETS = {
            "/com/patres/alina/uidesktop/assets/styles/index.css",
            "/com/patres/alina/uidesktop/assets/styles/workspace.css",
            "/com/patres/alina/uidesktop/assets/styles/chat-shell.css",
            "/com/patres/alina/uidesktop/assets/styles/settings.css",
            "/com/patres/alina/uidesktop/ui/chat/browser-chat.css",
            "/context-menu.css"
    };

    private static final Pattern LITERAL_COLOR = Pattern.compile(
            "(?i)(#[0-9a-f]{3,8}\\b|rgba?\\s*\\(|(?<![-\\w])(white|black)(?![-\\w]))"
    );

    @Test
    void componentStylesUseThemeTokensInsteadOfPaletteLiterals() throws IOException {
        for (String resource : COMPONENT_STYLESHEETS) {
            final String css = readStylesheet(resource);
            assertThat(LITERAL_COLOR.matcher(css).find())
                    .as("literal color in %s", resource)
                    .isFalse();
        }
    }

    @Test
    void elevationEffectsUseSemanticShadowTokens() throws IOException {
        for (String resource : COMPONENT_STYLESHEETS) {
            for (String line : readStylesheet(resource).lines().toList()) {
                if (line.contains("box-shadow:") && !line.contains("box-shadow: none")) {
                    assertThat(line)
                            .as("WebView shadow in %s", resource)
                            .contains("--color-shadow-default");
                }
                if (line.contains("dropshadow(")) {
                    assertThat(line)
                            .as("JavaFX shadow in %s", resource)
                            .contains("-color-shadow-default");
                }
            }
        }
    }

    @Test
    void darkModeUsesInsetColoredShorterShadows() throws IOException {
        assertThat(readStylesheet("/com/patres/alina/uidesktop/assets/styles/index.css"))
                .contains(".root:dark {")
                .contains("-color-shadow-default: -color-bg-inset;");

        assertThat(readStylesheet("/com/patres/alina/uidesktop/ui/chat/browser-chat.css"))
                .contains(":root.theme-dark {")
                .contains("--color-shadow-default: var(--color-bg-inset, var(--color-border-default));")
                .contains(":root.theme-dark .chat-message {")
                .contains("box-shadow: 0 4px 11px var(--color-shadow-default");
    }

    private String readStylesheet(final String resource) throws IOException {
        try (var stream = getClass().getResourceAsStream(resource)) {
            assertThat(stream).as(resource).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
