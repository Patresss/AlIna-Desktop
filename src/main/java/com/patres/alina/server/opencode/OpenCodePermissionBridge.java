package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.agent.AgentBackend;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.interaction.AgentInteractionAction;
import com.patres.alina.common.interaction.AgentInteractionApprovalScope;
import com.patres.alina.common.interaction.AgentInteractionKind;
import com.patres.alina.common.interaction.AgentInteractionRequest;
import com.patres.alina.common.interaction.AgentInteractionResolutionModel;
import com.patres.alina.common.interaction.AgentInteractionResponse;
import com.patres.alina.common.storage.OpenCodePaths;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
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

    public void clearForThread(final String threadId) {
        pendingPermissions.entrySet().removeIf(entry -> entry.getValue().threadId().equals(threadId));
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

        final ObjectNode payload = objectMapper.createObjectNode();
        payload.put("permission", permissionKey);
        payload.set("patterns", objectMapper.valueToTree(patterns));
        final AgentInteractionRequest interaction = new AgentInteractionRequest(
                requestId,
                AgentBackend.OPENCODE,
                AgentInteractionKind.APPROVAL,
                permissionTitle(permissionKey),
                permissionMessage(permissionKey, patterns),
                AgentInteractionApprovalScope.PERSISTENT,
                payload.toString()
        );
        final ChatMessageStreamEvent event = ChatMessageStreamEvent.interaction(threadId, interaction);
        return new RegisteredPermission(requestId, event);
    }

    public AgentInteractionResolutionModel resolve(final String requestId,
                                                   final AgentInteractionResponse response,
                                                   final BiConsumer<String, PendingPermission> onResolved) {
        final PendingPermission pendingPermission = pendingPermissions.get(requestId);
        if (pendingPermission == null) {
            return AgentInteractionResolutionModel.missing(LanguageManager.getLanguageString("chat.interaction.missing"));
        }
        if (response == null || !isApprovalAction(response.action())) {
            return AgentInteractionResolutionModel.error(
                    LanguageManager.getLanguageString("chat.interaction.invalidAction")
            );
        }

        final ObjectNode body = objectMapper.createObjectNode();
        switch (response.action()) {
            case APPROVE_ONCE -> {
                body.put("response", "once");
                body.put("remember", false);
            }
            case APPROVE_SCOPED -> {
                body.put("response", "always");
                body.put("remember", true);
            }
            case DENY -> {
                body.put("response", "reject");
                body.put("remember", false);
            }
            default -> throw new IllegalStateException("Unsupported OpenCode approval action: " + response.action());
        }

        try {
            httpClient.post("/session/%s/permissions/%s".formatted(pendingPermission.sessionId(), requestId), body);
            pendingPermissions.remove(requestId, pendingPermission);
            onResolved.accept(requestId, pendingPermission);
            if (response.action() == AgentInteractionAction.DENY) {
                return AgentInteractionResolutionModel.resolved(
                        false,
                        AgentInteractionApprovalScope.NONE,
                        true,
                        LanguageManager.getLanguageString("chat.permission.denied")
                );
            }
            final AgentInteractionApprovalScope scope = response.action() == AgentInteractionAction.APPROVE_SCOPED
                    ? AgentInteractionApprovalScope.PERSISTENT
                    : AgentInteractionApprovalScope.NONE;
            final String message = scope == AgentInteractionApprovalScope.PERSISTENT
                    ? LanguageManager.getLanguageString("chat.permission.approvedAlways")
                    : LanguageManager.getLanguageString("chat.permission.approvedOnce");
            return AgentInteractionResolutionModel.resolved(true, scope, true, message);
        } catch (Exception e) {
            logger.warn("Cannot resolve OpenCode permission request {}", requestId, e);
            return AgentInteractionResolutionModel.error(
                    LanguageManager.getLanguageString("chat.interaction.error", e.getMessage())
            );
        }
    }

    private boolean isApprovalAction(final AgentInteractionAction action) {
        return action == AgentInteractionAction.APPROVE_ONCE
                || action == AgentInteractionAction.APPROVE_SCOPED
                || action == AgentInteractionAction.DENY;
    }

    private String buildPermissionMessage(final String permissionKey, final List<String> patterns) {
        return "OpenCode wymaga zgody dla: " + permissionKey
                + System.lineSeparator()
                + "Patterns: " + patterns;
    }

    private String permissionTitle(final String permission) {
        if ("bash".equalsIgnoreCase(permission)) {
            return LanguageManager.getLanguageString("chat.permission.title.bash", permission);
        }
        if (permission != null && permission.startsWith("mcp_")) {
            return LanguageManager.getLanguageString("chat.permission.title.mcp", permission);
        }
        return LanguageManager.getLanguageString("chat.permission.title.tool", permission);
    }

    private String permissionMessage(final String permissionKey, final List<String> patterns) {
        return LanguageManager.getLanguageString(
                "chat.permission.message",
                buildPermissionMessage(permissionKey, patterns),
                OpenCodePaths.configFile().toString(),
                patterns.isEmpty() ? LanguageManager.getLanguageString("chat.permission.rule.none") : patterns.toString()
        );
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
