package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.permission.PermissionApprovalAction;
import com.patres.alina.common.permission.PermissionResolutionModel;
import com.patres.alina.common.storage.OpenCodePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

@Component
public class OpenCodePermissionBridge {

    private static final Logger logger = LoggerFactory.getLogger(OpenCodePermissionBridge.class);

    private final OpenCodeHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, PendingPermission> pendingPermissions = new ConcurrentHashMap<>();

    public OpenCodePermissionBridge(final OpenCodeHttpClient httpClient,
                                    final ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public boolean owns(final String requestId) {
        return pendingPermissions.containsKey(requestId);
    }

    public void register(final String requestId,
                         final String sessionId,
                         final String threadId) {
        pendingPermissions.put(requestId, new PendingPermission(sessionId, threadId));
    }

    public void clear() {
        pendingPermissions.clear();
    }

    public RegisteredPermission registerFromEvent(final JsonNode properties, final String threadId) {
        final String requestId = properties.path("id").asText(null);
        final String sessionId = properties.path("sessionID").asText(null);
        if (requestId == null || requestId.isBlank() || sessionId == null || sessionId.isBlank()) {
            return null;
        }

        final String permissionKey = resolvePermissionKey(properties);
        final List<String> patterns = extractPatterns(properties.path("patterns"));
        register(requestId, sessionId, threadId);

        final ChatMessageStreamEvent event = new ChatMessageStreamEvent(
                threadId,
                mapPermissionType(permissionKey),
                requestId,
                permissionKey,
                "OpenCode " + permissionKey,
                buildPermissionMessage(permissionKey, patterns),
                OpenCodePaths.configFile().toString(),
                patterns.isEmpty() ? "" : patterns.toString()
        );
        return new RegisteredPermission(requestId, event);
    }

    public PermissionResolutionModel resolve(final String requestId,
                                             final PermissionApprovalAction action,
                                             final BiConsumer<String, PendingPermission> onResolved) {
        final PendingPermission pendingPermission = pendingPermissions.remove(requestId);
        if (pendingPermission == null) {
            return PermissionResolutionModel.missing("To zapytanie o zgodę OpenCode nie jest już aktywne.");
        }

        final ObjectNode body = objectMapper.createObjectNode();
        switch (action) {
            case APPROVE_ONCE -> {
                body.put("response", "once");
                body.put("remember", false);
            }
            case APPROVE_ALWAYS -> {
                body.put("response", "always");
                body.put("remember", true);
            }
            case DENY -> {
                body.put("response", "reject");
                body.put("remember", false);
            }
        }

        try {
            httpClient.post("/session/%s/permissions/%s".formatted(pendingPermission.sessionId(), requestId), body);
            onResolved.accept(requestId, pendingPermission);
            if (action == PermissionApprovalAction.DENY) {
                return PermissionResolutionModel.denied("Dostęp odrzucony.");
            }
            final boolean persisted = action == PermissionApprovalAction.APPROVE_ALWAYS;
            final String message = persisted
                    ? "Trwała zgoda została przekazana do OpenCode. OpenCode kontynuuje."
                    : "Jednorazowa zgoda przyjęta. OpenCode kontynuuje.";
            return PermissionResolutionModel.approved(persisted, true, message);
        } catch (Exception e) {
            logger.warn("Cannot resolve OpenCode permission request {}", requestId, e);
            return PermissionResolutionModel.denied("Nie udało się przekazać decyzji do OpenCode: " + e.getMessage());
        }
    }

    private String buildPermissionMessage(final String permissionKey, final List<String> patterns) {
        return "OpenCode wymaga zgody dla: " + permissionKey
                + System.lineSeparator()
                + "Patterns: " + patterns;
    }

    private ChatMessageStreamEvent.PermissionType mapPermissionType(final String permission) {
        if ("bash".equalsIgnoreCase(permission)) {
            return ChatMessageStreamEvent.PermissionType.BASH;
        }
        if (permission != null && permission.startsWith("mcp_")) {
            return ChatMessageStreamEvent.PermissionType.MCP;
        }
        return ChatMessageStreamEvent.PermissionType.TOOL;
    }

    private String resolvePermissionKey(final JsonNode properties) {
        final String permission = properties.path("permission").asText("");
        final String tool = properties.path("tool").asText("");
        if (permission == null || permission.isBlank()) {
            return tool == null || tool.isBlank() ? "tool" : tool;
        }
        if (("tool".equalsIgnoreCase(permission) || "mcp".equalsIgnoreCase(permission)) && tool != null && !tool.isBlank()) {
            return tool;
        }
        return permission;
    }

    private List<String> extractPatterns(final JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        final java.util.ArrayList<String> patterns = new java.util.ArrayList<>();
        node.forEach(item -> {
            final String value = item.asText(null);
            if (value != null && !value.isBlank()) {
                patterns.add(value);
            }
        });
        return List.copyOf(patterns);
    }

    public record RegisteredPermission(String requestId,
                                       ChatMessageStreamEvent event) {
    }

    public record PendingPermission(String sessionId,
                                    String threadId) {
    }
}
