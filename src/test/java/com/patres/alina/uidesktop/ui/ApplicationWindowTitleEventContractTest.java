package com.patres.alina.uidesktop.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationWindowTitleEventContractTest {

    private static final Path APPLICATION_WINDOW = Path.of(
            "src/main/java/com/patres/alina/uidesktop/ui/ApplicationWindow.java"
    );

    @Test
    void titleNotificationUpdatesTabOnFxThreadWithoutWritingBackToBackend() throws IOException {
        final String source = Files.readString(APPLICATION_WINDOW);
        final int subscriptionStart = source.indexOf("ChatThreadTitleUpdatedEvent.class");
        final int subscriptionEnd = source.indexOf("// Initialize scheduler task executor", subscriptionStart);

        assertThat(subscriptionStart).isGreaterThanOrEqualTo(0);
        assertThat(subscriptionEnd).isGreaterThan(subscriptionStart);
        assertThat(source.substring(subscriptionStart, subscriptionEnd))
                .contains("Platform.runLater")
                .contains("chatTabBar.updateTabName")
                .doesNotContain("BackendApi.renameChatThread")
                .doesNotContain("ChatThreadRenameRequest");
    }
}
