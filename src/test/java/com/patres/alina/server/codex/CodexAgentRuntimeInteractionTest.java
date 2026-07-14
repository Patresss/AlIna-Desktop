package com.patres.alina.server.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.patres.alina.common.interaction.AgentInteractionAction;
import com.patres.alina.common.interaction.AgentInteractionResolutionModel;
import com.patres.alina.common.interaction.AgentInteractionResponse;
import com.patres.alina.common.settings.FileManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("unchecked")
class CodexAgentRuntimeInteractionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldKeepPendingInteractionWhenResponseFailsAndRemoveItAfterSuccess() throws Exception {
        final CodexAppServerClient client = mock(CodexAppServerClient.class);
        final CodexAgentRuntime runtime = runtime(client);
        final ObjectNode params = objectMapper.createObjectNode();
        params.putArray("command").add("pwd");
        final String requestId = runtime.storePendingInteraction(
                TextNode.valueOf("request-a"),
                CodexInteractionMapper.COMMAND_APPROVAL,
                "chat-1",
                "codex-1",
                params
        );
        final AgentInteractionResponse response = new AgentInteractionResponse(
                AgentInteractionAction.APPROVE_ONCE,
                "{}"
        );
        doThrow(new IllegalStateException("closed")).when(client).respond(any(JsonNode.class), any(JsonNode.class));

        final var failed = runtime.resolveAgentInteraction(requestId, response);

        assertTrue(runtime.ownsAgentInteraction(requestId));
        assertTrue(failed.status() == AgentInteractionResolutionModel.Status.ERROR);

        doNothing().when(client).respond(any(JsonNode.class), any(JsonNode.class));
        final var resolved = runtime.resolveAgentInteraction(requestId, response);

        assertFalse(runtime.ownsAgentInteraction(requestId));
        assertTrue(resolved.status() == AgentInteractionResolutionModel.Status.RESOLVED);
    }

    @Test
    void shouldKeepTextAndNumericJsonRpcIdsDistinct() {
        final CodexAgentRuntime runtime = runtime(mock(CodexAppServerClient.class));
        final ObjectNode params = objectMapper.createObjectNode();

        final String textId = runtime.storePendingInteraction(
                TextNode.valueOf("1"), CodexInteractionMapper.USER_INPUT, "chat", "codex", params
        );
        final String numericId = runtime.storePendingInteraction(
                objectMapper.getNodeFactory().numberNode(1), CodexInteractionMapper.USER_INPUT, "chat", "codex", params
        );

        assertNotEquals(textId, numericId);
        assertTrue(runtime.ownsAgentInteraction(textId));
        assertTrue(runtime.ownsAgentInteraction(numericId));
    }

    @Test
    void shouldRejectUnroutableServerRequestWithOriginalTextId() throws Exception {
        final CodexAppServerClient client = mock(CodexAppServerClient.class);
        final ArgumentCaptor<Consumer<JsonNode>> listenerCaptor = ArgumentCaptor.forClass(Consumer.class);
        final CodexAgentRuntime runtime = runtime(client);
        verify(client).addMessageListener(listenerCaptor.capture());

        final ObjectNode message = objectMapper.createObjectNode();
        message.put("id", "request-text-id");
        message.put("method", CodexInteractionMapper.USER_INPUT);
        message.putObject("params").put("threadId", "unknown-thread");
        listenerCaptor.getValue().accept(message);

        verify(client).respondError(
                eq(TextNode.valueOf("request-text-id")),
                eq(-32602),
                any(String.class)
        );
        assertFalse(runtime.ownsAgentInteraction("text:request-text-id"));
    }

    private CodexAgentRuntime runtime(final CodexAppServerClient client) {
        return new CodexAgentRuntime(
                client,
                mock(FileManager.class),
                mock(FileManager.class),
                objectMapper
        );
    }
}
