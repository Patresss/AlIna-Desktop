package com.patres.alina.server.codex;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.settings.FileManager;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class CodexAgentRuntimeEffortTest {

    @SuppressWarnings("unchecked")
    @Test
    void shouldExposeReasoningEffortsAdvertisedByCodexModel() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        final CodexAppServerClient client = mock(CodexAppServerClient.class);
        final ObjectNode response = objectMapper.createObjectNode();
        final ObjectNode model = response.putArray("data").addObject().put("id", "gpt-5.4");
        model.putArray("supportedReasoningEfforts")
                .addObject().put("reasoningEffort", "low");
        model.withArray("supportedReasoningEfforts")
                .addObject().put("reasoningEffort", "high");
        when(client.request(eq("model/list"), any())).thenReturn(response);

        final CodexAgentRuntime runtime = new CodexAgentRuntime(
                client,
                mock(FileManager.class),
                mock(FileManager.class),
                objectMapper
        );

        assertEquals(List.of("low", "high"), runtime.getAvailableEfforts("openai/gpt-5.4"));
        assertEquals(List.of("low", "high"), runtime.getAvailableEfforts("openai/gpt-5.4"));
        verify(client, times(1)).request(eq("model/list"), any());
    }
}
