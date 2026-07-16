package com.patres.alina.uidesktop.ui.theme;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class UpcomingEventStyleContractTest {

    @Test
    void definesDenseCardStylesUsingThemeColors() throws Exception {
        final String css = readWorkspaceCss();

        assertThat(css).contains(
                ".workspace-upcoming-event-status",
                ".workspace-upcoming-event-summary",
                ".workspace-upcoming-event-group-label",
                ".workspace-upcoming-event-chip",
                ".workspace-upcoming-event-room-chip",
                ".workspace-upcoming-event-description",
                ".workspace-upcoming-event-attachment",
                ".workspace-upcoming-event-actions",
                ".workspace-upcoming-event-prepare",
                ".workspace-upcoming-event-join",
                ".workspace-upcoming-event-attachments-toggle",
                ".workspace-upcoming-event-stale",
                "-color-accent-subtle",
                "-color-fg-default"
        );
    }

    @Test
    void makesJoinPrimaryAndPrepareSecondary() throws Exception {
        final String css = readWorkspaceCss();

        assertThat(rule(css, ".workspace-upcoming-event-join"))
                .contains(
                        "-fx-border-color: -color-border-default;",
                        "-fx-text-fill: -color-accent-fg;",
                        "-fx-font-size: 11px;",
                        "-fx-padding: 4px 8px;"
                );
        assertThat(rule(css, ".workspace-upcoming-event-prepare,"))
                .contains(
                        ".workspace-upcoming-event-attachments-toggle",
                        "-fx-text-fill: -color-fg-muted;",
                        "-fx-font-size: 10px;",
                        "-fx-padding: 4px 7px;"
                );
    }

    private String readWorkspaceCss() throws Exception {
        try (var stream = getClass().getResourceAsStream(
                "/com/patres/alina/uidesktop/assets/styles/workspace.css"
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
