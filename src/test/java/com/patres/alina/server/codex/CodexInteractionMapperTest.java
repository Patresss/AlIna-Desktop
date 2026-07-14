package com.patres.alina.server.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.interaction.AgentInteractionAction;
import com.patres.alina.common.interaction.AgentInteractionApprovalScope;
import com.patres.alina.common.interaction.AgentInteractionKind;
import com.patres.alina.common.interaction.AgentInteractionResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodexInteractionMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CodexInteractionMapper mapper = new CodexInteractionMapper(objectMapper);

    @Test
    void shouldMapUserInputRequestAndAnswers() throws Exception {
        final JsonNode params = objectMapper.readTree("""
                {
                  "threadId": "thread-1",
                  "turnId": "turn-1",
                  "itemId": "item-1",
                  "autoResolutionMs": 60000,
                  "questions": [
                    {
                      "id": "target",
                      "header": "Target",
                      "question": "Which target?",
                      "options": [
                        {"label": "Local", "description": "Run locally"},
                        {"label": "CI", "description": "Run in CI"}
                      ]
                    }
                  ]
                }
                """);

        final var request = mapper.toRequest("number:21", CodexInteractionMapper.USER_INPUT, params);
        final JsonNode result = mapper.toResult(
                CodexInteractionMapper.USER_INPUT,
                params,
                new AgentInteractionResponse(AgentInteractionAction.SUBMIT, "{\"target\":[\"CI\"]}")
        );

        assertEquals(AgentInteractionKind.USER_INPUT, request.kind());
        assertEquals(AgentInteractionApprovalScope.NONE, request.approvalScope());
        assertEquals(60000, objectMapper.readTree(request.payloadJson()).path("autoResolutionMs").asInt());
        assertEquals("CI", result.path("answers").path("target").path("answers").get(0).asText());
    }

    @Test
    void shouldMapMcpFormAndTypedContent() throws Exception {
        final JsonNode params = objectMapper.readTree("""
                {
                  "threadId": "thread-1",
                  "serverName": "calendar",
                  "message": "Select event details",
                  "mode": "form",
                  "requestedSchema": {
                    "type": "object",
                    "properties": {
                      "title": {"type": "string"},
                      "reminders": {"type": "integer"},
                      "notify": {"type": "boolean"}
                    },
                    "required": ["title"]
                  }
                }
                """);

        final var request = mapper.toRequest("text:elicitation-1", CodexInteractionMapper.MCP_ELICITATION, params);
        final JsonNode result = mapper.toResult(
                CodexInteractionMapper.MCP_ELICITATION,
                params,
                new AgentInteractionResponse(
                        AgentInteractionAction.SUBMIT,
                        "{\"title\":\"Planning\",\"reminders\":2,\"notify\":true}"
                )
        );

        assertEquals(AgentInteractionKind.MCP_FORM, request.kind());
        assertEquals("accept", result.path("action").asText());
        assertEquals(2, result.path("content").path("reminders").asInt());
        assertEquals(true, result.path("content").path("notify").asBoolean());
    }

    @Test
    void shouldMapMcpDeclineWithoutInventingContent() {
        final ObjectNode params = objectMapper.createObjectNode();

        final JsonNode result = mapper.toResult(
                CodexInteractionMapper.MCP_ELICITATION,
                params,
                new AgentInteractionResponse(AgentInteractionAction.DECLINE, "{}")
        );

        assertEquals("decline", result.path("action").asText());
        assertEquals(true, result.path("content").isNull());
    }

    @Test
    void shouldUseSessionScopeForCodexApproval() throws Exception {
        final JsonNode params = objectMapper.readTree("""
                {"permissions":{"fileSystem":{"read":["/tmp"]}}}
                """);

        final JsonNode result = mapper.toResult(
                CodexInteractionMapper.PERMISSIONS_APPROVAL,
                params,
                new AgentInteractionResponse(AgentInteractionAction.APPROVE_SCOPED, "{}")
        );

        assertEquals("session", result.path("scope").asText());
        assertEquals(params.path("permissions"), result.path("permissions"));
    }

    @Test
    void shouldRejectApprovalActionForUserInput() {
        final ObjectNode params = objectMapper.createObjectNode();
        params.putArray("questions");

        assertThrows(IllegalArgumentException.class, () -> mapper.toResult(
                CodexInteractionMapper.USER_INPUT,
                params,
                new AgentInteractionResponse(AgentInteractionAction.APPROVE_ONCE, "{}")
        ));
    }
}
