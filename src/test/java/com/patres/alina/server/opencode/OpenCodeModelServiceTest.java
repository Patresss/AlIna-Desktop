package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.settings.AssistantSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class OpenCodeModelServiceTest {

    @Test
    void shouldExposeVariantsAsEffortOptionsForSelectedModel() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final OpenCodeConfigurationService configurationService = mock(OpenCodeConfigurationService.class);
        final OpenCodeHttpClient httpClient = mock(OpenCodeHttpClient.class);
        when(configurationService.assistantSettings()).thenReturn(new AssistantSettings("openai/fallback", "high"));

        final ObjectNode response = objectMapper.createObjectNode();
        final ObjectNode provider = response.putArray("all").addObject().put("id", "openai");
        final ObjectNode variants = provider.putObject("models")
                .putObject("gpt-5")
                .putObject("variants");
        variants.putObject("low");
        variants.putObject("max");
        when(httpClient.get("/provider")).thenReturn(response);

        final OpenCodeModelService service = new OpenCodeModelService(configurationService, httpClient);

        assertEquals(List.of("low", "max"), service.getAvailableEfforts("openai/gpt-5"));
        assertEquals(List.of("low", "max"), service.getAvailableEfforts("openai/gpt-5"));
        verify(httpClient, times(1)).get("/provider");
    }
}
