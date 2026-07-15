package com.patres.alina.common.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpcomingEventCardSettingsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void exposesUsefulDefaults() {
        assertThat(new UpcomingEventCardSettings()).isEqualTo(
                new UpcomingEventCardSettings(true, 4, 240, true)
        );
    }

    @Test
    void normalizesMissingAndOutOfRangeValues() throws Exception {
        final UpcomingEventCardSettings missing = objectMapper.readValue(
                "{}",
                UpcomingEventCardSettings.class
        );
        final UpcomingEventCardSettings clamped = new UpcomingEventCardSettings(false, 99, 20, false);

        assertThat(missing.attendeePreviewLimit()).isEqualTo(4);
        assertThat(missing.descriptionPreviewCharacters()).isEqualTo(240);
        assertThat(missing.visible()).isTrue();
        assertThat(missing.showAttachments()).isTrue();
        assertThat(clamped.attendeePreviewLimit()).isEqualTo(12);
        assertThat(clamped.descriptionPreviewCharacters()).isEqualTo(80);
    }
}
