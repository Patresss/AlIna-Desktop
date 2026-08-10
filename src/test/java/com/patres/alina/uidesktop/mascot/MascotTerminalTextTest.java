package com.patres.alina.uidesktop.mascot;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MascotTerminalTextTest {

    private static final Instant NOW = Instant.parse("2026-07-20T16:41:47Z");

    @Test
    void hidesAutomaticThreadTitleAndKeepsMeaningfulTitle() {
        assertThat(MascotTerminalText.detail(terminal(
                MascotNotificationType.COMPLETE,
                "2026-07-20 (16:41:47)",
                ""
        ))).isEmpty();
        assertThat(MascotTerminalText.detail(terminal(
                MascotNotificationType.COMPLETE,
                "Przygotowanie planu wdrożenia",
                ""
        ))).isEqualTo("Przygotowanie planu wdrożenia");
    }

    @Test
    void errorMessageTakesPriorityOverAutomaticTitle() {
        assertThat(MascotTerminalText.detail(terminal(
                MascotNotificationType.ERROR,
                "2026-07-20 (16:41:47)",
                "Backend unavailable"
        ))).isEqualTo("Backend unavailable");
    }

    @Test
    void recognizesOnlyCompleteAutomaticTimestamp() {
        assertThat(MascotTerminalText.isAutomaticThreadTitle(" 2026-07-20 (16:41:47) ")).isTrue();
        assertThat(MascotTerminalText.isAutomaticThreadTitle("Plan 2026-07-20 (16:41:47)")).isFalse();
        assertThat(MascotTerminalText.isAutomaticThreadTitle("2026-07-20")).isFalse();
    }

    private MascotNotification terminal(final MascotNotificationType type,
                                         final String title,
                                         final String message) {
        return MascotNotification.terminal(type, "thread", title, message, NOW);
    }
}
