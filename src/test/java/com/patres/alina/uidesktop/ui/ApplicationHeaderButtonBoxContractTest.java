package com.patres.alina.uidesktop.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationHeaderButtonBoxContractTest {

    private static final String HEADER_FXML =
            "/com/patres/alina/uidesktop/fxml/header-bar-button-box.fxml";

    @Test
    void agentSessionActionStartsDisabledAndUsesBackendNeutralContract() throws IOException {
        final String fxml = readResource(HEADER_FXML);

        assertThat(fxml)
                .contains("fx:id=\"agentSessionButton\"")
                .contains("disable=\"true\"")
                .contains("onAction=\"#openAgentSession\"")
                .contains("%header.tooltip.agentSession")
                .doesNotContain("openOpenCodeSession")
                .doesNotContain("header.tooltip.openCodeSession");
    }

    private String readResource(final String resource) throws IOException {
        try (var stream = getClass().getResourceAsStream(resource)) {
            assertThat(stream).as(resource).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
