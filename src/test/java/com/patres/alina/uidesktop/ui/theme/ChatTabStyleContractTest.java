package com.patres.alina.uidesktop.ui.theme;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ChatTabStyleContractTest {

    @Test
    void stylesActiveTabAsFloatingCard() throws Exception {
        final String css = readChatShellCss();

        assertThat(rule(css, ".chat-tab-item"))
                .contains(
                        "-fx-background-radius: 14px;",
                        "-fx-border-radius: 14px;",
                        "-fx-border-width: 1px;"
                );
        assertThat(rule(css, ".chat-tab-active"))
                .contains(
                        "-fx-background-color: -color-accent-subtle;",
                        "-fx-border-color: -color-accent-muted;",
                        "-fx-effect: dropshadow(gaussian, -color-shadow-default, 8, 0.08, 0, 2);"
                );
        assertThat(rule(css, ".chat-tab-active .chat-tab-name"))
                .contains(
                        "-fx-font-weight: 700;",
                        "-fx-text-fill: -color-accent-fg;"
                );
    }

    @Test
    void keepsInactiveTabsQuietUntilHovered() throws Exception {
        final String css = readChatShellCss();

        assertThat(rule(css, ".chat-tab-inactive"))
                .contains(
                        "-fx-background-color: transparent;",
                        "-fx-border-color: transparent;",
                        "-fx-effect: none;"
                );
        assertThat(rule(css, ".chat-tab-inactive:hover"))
                .contains("-fx-background-color: -color-bg-subtle;");
    }

    private String readChatShellCss() throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/com/patres/alina/uidesktop/assets/styles/chat-shell.css"
        )) {
            assertThat(stream).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String rule(final String css, final String selector) {
        final int start = css.indexOf(selector);
        assertThat(start).as(selector).isGreaterThanOrEqualTo(0);
        final int end = css.indexOf('}', start);
        assertThat(end).as(selector).isGreaterThan(start);
        return css.substring(start, end + 1);
    }
}
