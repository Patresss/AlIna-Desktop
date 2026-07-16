package com.patres.alina.uidesktop.ui.theme;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class UpcomingEventStyleContractTest {

    @Test
    void definesDenseCardStylesUsingThemeColors() throws Exception {
        final String css;
        try (var stream = getClass().getResourceAsStream(
                "/com/patres/alina/uidesktop/assets/styles/workspace.css"
        )) {
            assertThat(stream).isNotNull();
            css = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

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
}
