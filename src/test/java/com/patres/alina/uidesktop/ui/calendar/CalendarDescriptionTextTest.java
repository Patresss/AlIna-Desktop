package com.patres.alina.uidesktop.ui.calendar;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarDescriptionTextTest {

    @Test
    void convertsSimpleHtmlToReadablePlainText() {
        assertThat(CalendarDescriptionText.toPlainText(
                "<p>Agenda &amp; goals</p><p>Drugi<br>wiersz</p>"
        )).isEqualTo("Agenda & goals\nDrugi\nwiersz");
    }

    @Test
    void truncatesAtAWordBoundaryAndKeepsUnicode() {
        final String preview = CalendarDescriptionText.preview(
                "Zażółć gęślą jaźń podczas dłuższego spotkania",
                25
        );

        assertThat(preview).isEqualTo("Zażółć gęślą jaźń…");
    }

    @Test
    void returnsShortNormalizedTextWithoutEllipsis() {
        assertThat(CalendarDescriptionText.preview("Ala\nma   kota", 80)).isEqualTo("Ala ma kota");
    }
}
