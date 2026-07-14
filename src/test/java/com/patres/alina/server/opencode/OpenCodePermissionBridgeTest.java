package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.common.interaction.AgentInteractionAction;
import com.patres.alina.common.interaction.AgentInteractionApprovalScope;
import com.patres.alina.common.interaction.AgentInteractionResolutionModel;
import com.patres.alina.common.interaction.AgentInteractionResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenCodePermissionBridgeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldKeepPendingPermissionAfterTransportError() throws Exception {
        final OpenCodeHttpClient httpClient = mock(OpenCodeHttpClient.class);
        final OpenCodePermissionBridge bridge = new OpenCodePermissionBridge(httpClient, objectMapper);
        bridge.register("permission-1", "session-1", "thread-1");
        doThrow(new IllegalStateException("offline")).when(httpClient).post(anyString(), any(JsonNode.class));

        final var resolution = bridge.resolve(
                "permission-1",
                new AgentInteractionResponse(AgentInteractionAction.APPROVE_ONCE, "{}"),
                (_, _) -> { }
        );

        assertEquals(AgentInteractionResolutionModel.Status.ERROR, resolution.status());
        assertTrue(bridge.owns("permission-1"));
    }

    @Test
    void shouldRemovePermissionAfterPersistentApprovalSucceeds() throws Exception {
        final OpenCodeHttpClient httpClient = mock(OpenCodeHttpClient.class);
        final OpenCodePermissionBridge bridge = new OpenCodePermissionBridge(httpClient, objectMapper);
        bridge.register("permission-2", "session-2", "thread-2");
        when(httpClient.post(anyString(), any(JsonNode.class))).thenReturn(objectMapper.createObjectNode());
        final AtomicBoolean callbackCalled = new AtomicBoolean(false);

        final var resolution = bridge.resolve(
                "permission-2",
                new AgentInteractionResponse(AgentInteractionAction.APPROVE_SCOPED, "{}"),
                (_, _) -> callbackCalled.set(true)
        );

        final ArgumentCaptor<JsonNode> bodyCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(httpClient).post(anyString(), bodyCaptor.capture());
        assertEquals("always", bodyCaptor.getValue().path("response").asText());
        assertTrue(bodyCaptor.getValue().path("remember").asBoolean());
        assertEquals(AgentInteractionApprovalScope.PERSISTENT, resolution.approvalScope());
        assertTrue(callbackCalled.get());
        assertFalse(bridge.owns("permission-2"));
    }

    @Test
    void shouldClearOnlyPermissionsOwnedByCancelledThread() {
        final OpenCodePermissionBridge bridge = new OpenCodePermissionBridge(mock(OpenCodeHttpClient.class), objectMapper);
        bridge.register("permission-a", "session-a", "thread-a");
        bridge.register("permission-b", "session-b", "thread-b");

        bridge.clearForThread("thread-a");

        assertFalse(bridge.owns("permission-a"));
        assertTrue(bridge.owns("permission-b"));
    }
}
