package com.patres.alina.common.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssistantSettingsTest {

    @Test
    void shouldUseAStableDefaultEffortForOldAndEmptySettings() {
        assertEquals(AssistantSettings.DEFAULT_EFFORT, new AssistantSettings().effort());
        assertEquals(AssistantSettings.DEFAULT_EFFORT, new AssistantSettings("gpt-5", null).effort());
        assertEquals(AssistantSettings.DEFAULT_EFFORT, new AssistantSettings("gpt-5", " ").effort());
    }

    @Test
    void shouldPreserveExplicitModelAndEffort() {
        final AssistantSettings settings = new AssistantSettings(" openai/gpt-5 ", " xhigh ");

        assertEquals("openai/gpt-5", settings.chatModel());
        assertEquals("xhigh", settings.effort());
        assertEquals("openai/gpt-5", settings.resolveModelIdentifier());
    }
}
