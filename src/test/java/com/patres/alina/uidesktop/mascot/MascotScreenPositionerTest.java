package com.patres.alina.uidesktop.mascot;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MascotScreenPositionerTest {

    @Test
    void positionsPopupInBottomRightOfScreenContainingCursor() {
        final var screens = List.of(
                new MascotScreenPositioner.ScreenBounds(0, 0, 1920, 1040),
                new MascotScreenPositioner.ScreenBounds(1920, 0, 1440, 860)
        );

        final var position = MascotScreenPositioner.bottomRight(
                screens, 2200, 400, 360, 150, 18
        );

        assertThat(position.x()).isEqualTo(2982);
        assertThat(position.y()).isEqualTo(692);
    }

    @Test
    void supportsNegativeMonitorCoordinatesAndClampsOversizedPopup() {
        final var screens = List.of(
                new MascotScreenPositioner.ScreenBounds(-1280, -100, 1280, 900)
        );

        final var position = MascotScreenPositioner.bottomRight(
                screens, -500, 200, 1600, 1200, 18
        );

        assertThat(position.x()).isEqualTo(-1280);
        assertThat(position.y()).isEqualTo(-100);
    }
}
