package com.patres.alina.uidesktop.ui.calendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarRoomDisplayNameTest {

    @Test
    void keepsRoomSuffixAfterBuildingSeparatedByColon() {
        assertThat(CalendarRoomDisplayName.shorten(
                "Warszawa, Fabryka Norblina: Galwan-4-91-Dekiel (2)"
        )).isEqualTo("Warszawa, Galwan-4-91-Dekiel (2)");
    }

    @Test
    void removesLocationCodesAndEquipmentFromHyphenatedRoom() {
        assertThat(CalendarRoomDisplayName.shorten(
                "Kraków, Centrum Biurowe Lubicz-05-40-Świnnica (6) [Digital Whiteboard, Logitech, Whiteboard]"
        )).isEqualTo("Kraków, 05-40-Świnnica (6)");
    }

    @Test
    void usesConservativeFallbacksForSimpleAndBlankNames() {
        assertThat(CalendarRoomDisplayName.shorten("Warszawa, Sala Atlas [TV, Whiteboard]"))
                .isEqualTo("Warszawa, Sala Atlas");
        assertThat(CalendarRoomDisplayName.shorten("Sala A-B")).isEqualTo("Sala A-B");
        assertThat(CalendarRoomDisplayName.shorten("  ")).isEmpty();
    }
}
