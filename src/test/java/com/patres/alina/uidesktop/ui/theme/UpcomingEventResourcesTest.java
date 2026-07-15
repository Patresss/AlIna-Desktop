package com.patres.alina.uidesktop.ui.theme;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;

class UpcomingEventResourcesTest {

    private static final List<String> REQUIRED_KEYS = List.of(
            "settings.dashboard.upcomingEvent.section",
            "settings.workspace.showUpcomingEvent.title",
            "settings.workspace.upcomingEventAttendeeLimit.title",
            "settings.workspace.upcomingEventDescriptionLimit.title",
            "settings.workspace.upcomingEventAttachments.title",
            "dashboard.upcomingEvent.title",
            "dashboard.upcomingEvent.status.running",
            "dashboard.upcomingEvent.attendees",
            "dashboard.upcomingEvent.description",
            "dashboard.upcomingEvent.attachments",
            "dashboard.upcomingEvent.join",
            "dashboard.upcomingEvent.prepare",
            "dashboard.upcomingEvent.prepare.noPrompt"
    );

    @Test
    void providesEnglishAndPolishCopyForTheCardAndSettings() {
        final ResourceBundle english = ResourceBundle.getBundle("language.Bundle", Locale.ENGLISH);
        final ResourceBundle polish = ResourceBundle.getBundle("language.Bundle", Locale.forLanguageTag("pl"));

        for (final String key : REQUIRED_KEYS) {
            assertThat(english.containsKey(key)).as("English key %s", key).isTrue();
            assertThat(polish.containsKey(key)).as("Polish key %s", key).isTrue();
            assertThat(english.getString(key)).isNotBlank();
            assertThat(polish.getString(key)).isNotBlank();
        }
    }
}
