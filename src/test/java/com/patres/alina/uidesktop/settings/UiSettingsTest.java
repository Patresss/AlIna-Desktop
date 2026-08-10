package com.patres.alina.uidesktop.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UiSettingsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void enablesMascotWhenOlderSettingsDoNotContainTheOption() throws Exception {
        final UiSettings settings = objectMapper.readValue("{\"theme\":\"Calm Command Center\"}", UiSettings.class);

        assertThat(settings.isMascotNotificationsEnabled()).isTrue();
    }

    @Test
    void preservesExplicitlyDisabledMascotSetting() throws Exception {
        final UiSettings settings = objectMapper.readValue("{\"mascotNotificationsEnabled\":false}", UiSettings.class);

        assertThat(settings.isMascotNotificationsEnabled()).isFalse();
    }
}
