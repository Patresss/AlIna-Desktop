package com.patres.alina.uidesktop.ui.theme;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationStylesheetContractTest {

    private static final Pattern LITERAL_COLOR = Pattern.compile(
            "(?i)(#[0-9a-f]{3,8}\\b|rgba?\\s*\\(|(?<![-\\w])(white|black)(?![-\\w]))"
    );

    @Test
    void componentStylesUseThemeTokensInsteadOfPaletteLiterals() throws IOException {
        for (String resource : new String[]{
                "/com/patres/alina/uidesktop/assets/styles/index.css",
                "/com/patres/alina/uidesktop/assets/styles/workspace.css",
                "/com/patres/alina/uidesktop/assets/styles/chat-shell.css",
                "/com/patres/alina/uidesktop/assets/styles/settings.css",
                "/com/patres/alina/uidesktop/ui/chat/browser-chat.css",
                "/context-menu.css"
        }) {
            final String css;
            try (var stream = getClass().getResourceAsStream(resource)) {
                assertThat(stream).as(resource).isNotNull();
                css = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertThat(LITERAL_COLOR.matcher(css).find())
                    .as("literal color in %s", resource)
                    .isFalse();
        }
    }
}
