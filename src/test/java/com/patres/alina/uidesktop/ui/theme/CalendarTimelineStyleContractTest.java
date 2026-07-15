package com.patres.alina.uidesktop.ui.theme;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarTimelineStyleContractTest {

    @Test
    void usesSquareTimelineMarkerInsteadOfRoundedCurrentRowBorder() throws IOException {
        final String css;
        try (var stream = getClass().getResourceAsStream(
                "/com/patres/alina/uidesktop/assets/styles/workspace.css"
        )) {
            assertThat(stream).isNotNull();
            css = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertThat(css).contains(
                ".workspace-calendar-timeline",
                ".workspace-calendar-timeline-current",
                ".workspace-calendar-row-content",
                "-fx-padding: 6px 8px;",
                "-fx-background-radius: 0;"
        );
        assertThat(css).doesNotContain("-fx-border-width: 0 0 0 3px");
    }
}
