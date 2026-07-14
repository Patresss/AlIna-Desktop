package com.patres.alina.server.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.agent.AgentBackend;
import com.patres.alina.common.interaction.AgentInteractionAction;
import com.patres.alina.common.interaction.AgentInteractionApprovalScope;
import com.patres.alina.common.interaction.AgentInteractionKind;
import com.patres.alina.common.interaction.AgentInteractionRequest;
import com.patres.alina.common.interaction.AgentInteractionResponse;
import com.patres.alina.uidesktop.ui.language.LanguageManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class CodexInteractionMapper {

    static final String COMMAND_APPROVAL = "item/commandExecution/requestApproval";
    static final String FILE_CHANGE_APPROVAL = "item/fileChange/requestApproval";
    static final String PERMISSIONS_APPROVAL = "item/permissions/requestApproval";
    static final String USER_INPUT = "item/tool/requestUserInput";
    static final String MCP_ELICITATION = "mcpServer/elicitation/request";

    private final ObjectMapper objectMapper;

    CodexInteractionMapper(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    AgentInteractionRequest toRequest(final String requestId,
                                      final String method,
                                      final JsonNode params) {
        final AgentInteractionKind kind = interactionKind(method, params);
        final ObjectNode payload = objectMapper.createObjectNode();
        final AgentInteractionApprovalScope approvalScope;

        switch (kind) {
            case APPROVAL -> {
                approvalScope = AgentInteractionApprovalScope.SESSION;
                payload.put("value", permissionValue(method, params));
            }
            case USER_INPUT -> {
                approvalScope = AgentInteractionApprovalScope.NONE;
                payload.set("questions", params.path("questions").deepCopy());
                if (params.hasNonNull("autoResolutionMs")) {
                    payload.set("autoResolutionMs", params.path("autoResolutionMs").deepCopy());
                }
            }
            case MCP_FORM -> {
                approvalScope = AgentInteractionApprovalScope.NONE;
                payload.put("serverName", params.path("serverName").asText("MCP"));
                payload.put("mode", params.path("mode").asText("form"));
                payload.set("requestedSchema", params.path("requestedSchema").deepCopy());
            }
            case MCP_URL -> {
                approvalScope = AgentInteractionApprovalScope.NONE;
                payload.put("serverName", params.path("serverName").asText("MCP"));
                payload.put("url", params.path("url").asText(""));
            }
            default -> throw new IllegalStateException("Unsupported Codex interaction kind: " + kind);
        }

        return new AgentInteractionRequest(
                requestId,
                AgentBackend.CODEX,
                kind,
                interactionTitle(method, params, kind),
                interactionMessage(method, params, kind),
                approvalScope,
                payload.toString()
        );
    }

    JsonNode toResult(final String method,
                      final JsonNode params,
                      final AgentInteractionResponse response) {
        if (response == null || response.action() == null) {
            throw new IllegalArgumentException("Interaction response action is required");
        }
        if (PERMISSIONS_APPROVAL.equals(method)) {
            requireApprovalAction(response.action());
            final ObjectNode result = objectMapper.createObjectNode();
            if (response.action() == AgentInteractionAction.DENY) {
                result.set("permissions", objectMapper.createObjectNode());
                return result;
            }
            result.set("permissions", params.path("permissions").deepCopy());
            result.put("scope", response.action() == AgentInteractionAction.APPROVE_SCOPED ? "session" : "turn");
            return result;
        }
        if (MCP_ELICITATION.equals(method)) {
            return mcpResult(response);
        }
        if (USER_INPUT.equals(method)) {
            return userInputResult(params, response);
        }
        requireApprovalAction(response.action());
        final ObjectNode result = objectMapper.createObjectNode();
        final String decision = switch (response.action()) {
            case APPROVE_ONCE -> "accept";
            case APPROVE_SCOPED -> "acceptForSession";
            case DENY -> "decline";
            default -> throw new IllegalArgumentException("Unsupported approval action: " + response.action());
        };
        result.put("decision", decision);
        return result;
    }

    private JsonNode userInputResult(final JsonNode params, final AgentInteractionResponse response) {
        if (response.action() != AgentInteractionAction.SUBMIT) {
            throw new IllegalArgumentException("Codex user input requires SUBMIT");
        }
        final JsonNode values = readValues(response);
        final ObjectNode answers = objectMapper.createObjectNode();
        for (final JsonNode question : params.path("questions")) {
            final String id = question.path("id").asText("");
            if (id.isBlank() || !values.has(id)) {
                continue;
            }
            final ArrayNode answerValues = objectMapper.createArrayNode();
            final JsonNode value = values.path(id);
            if (value.isArray()) {
                value.forEach(item -> answerValues.add(item.asText("")));
            } else if (!value.isNull() && !value.isMissingNode()) {
                answerValues.add(value.asText(""));
            }
            answers.putObject(id).set("answers", answerValues);
        }
        return objectMapper.createObjectNode().set("answers", answers);
    }

    private JsonNode mcpResult(final AgentInteractionResponse response) {
        final ObjectNode result = objectMapper.createObjectNode();
        switch (response.action()) {
            case SUBMIT -> {
                result.put("action", "accept");
                result.set("content", readValues(response));
            }
            case DECLINE -> {
                result.put("action", "decline");
                result.putNull("content");
            }
            case CANCEL -> {
                result.put("action", "cancel");
                result.putNull("content");
            }
            default -> throw new IllegalArgumentException("Unsupported MCP elicitation action: " + response.action());
        }
        return result;
    }

    private JsonNode readValues(final AgentInteractionResponse response) {
        try {
            final JsonNode values = objectMapper.readTree(response.valuesJson());
            if (values == null || !values.isObject()) {
                throw new IllegalArgumentException("Interaction values must be a JSON object");
            }
            return values;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse interaction values", e);
        }
    }

    private void requireApprovalAction(final AgentInteractionAction action) {
        if (action != AgentInteractionAction.APPROVE_ONCE
                && action != AgentInteractionAction.APPROVE_SCOPED
                && action != AgentInteractionAction.DENY) {
            throw new IllegalArgumentException("Unsupported approval action: " + action);
        }
    }

    private AgentInteractionKind interactionKind(final String method, final JsonNode params) {
        if (USER_INPUT.equals(method)) {
            return AgentInteractionKind.USER_INPUT;
        }
        if (MCP_ELICITATION.equals(method)) {
            return "url".equals(params.path("mode").asText())
                    ? AgentInteractionKind.MCP_URL
                    : AgentInteractionKind.MCP_FORM;
        }
        return AgentInteractionKind.APPROVAL;
    }

    private String interactionTitle(final String method,
                                    final JsonNode params,
                                    final AgentInteractionKind kind) {
        if (kind == AgentInteractionKind.USER_INPUT) {
            return LanguageManager.getLanguageString("chat.interaction.userInput.title");
        }
        if (kind == AgentInteractionKind.MCP_FORM || kind == AgentInteractionKind.MCP_URL) {
            return LanguageManager.getLanguageString(
                    "chat.interaction.mcp.title",
                    params.path("serverName").asText("MCP")
            );
        }
        final String value = permissionValue(method, params);
        if (COMMAND_APPROVAL.equals(method)) {
            return LanguageManager.getLanguageString("chat.permission.title.bash", value);
        }
        return LanguageManager.getLanguageString("chat.permission.title.tool", value);
    }

    private String interactionMessage(final String method,
                                      final JsonNode params,
                                      final AgentInteractionKind kind) {
        if (kind == AgentInteractionKind.USER_INPUT) {
            return LanguageManager.getLanguageString("chat.interaction.userInput.message");
        }
        if (kind == AgentInteractionKind.MCP_FORM || kind == AgentInteractionKind.MCP_URL) {
            return params.path("message").asText("");
        }
        final String rule = permissionMatchedRule(method, params);
        return LanguageManager.getLanguageString(
                "chat.permission.message",
                permissionReason(method, params),
                codexConfigPath(),
                rule.isBlank() ? LanguageManager.getLanguageString("chat.permission.rule.none") : rule
        );
    }

    private String permissionReason(final String method, final JsonNode params) {
        final List<String> details = new ArrayList<>();
        final String reason = params.path("reason").asText("");
        final String cwd = params.path("cwd").asText("");
        if (!reason.isBlank()) {
            details.add(reason);
        }
        if (!cwd.isBlank()) {
            details.add("Working directory: " + cwd);
        }
        if (COMMAND_APPROVAL.equals(method)) {
            details.add("Command: " + commandFromParams(params));
        }
        if (PERMISSIONS_APPROVAL.equals(method)) {
            details.add("Permissions: " + params.path("permissions"));
        }
        return details.isEmpty()
                ? LanguageManager.getLanguageString("chat.permission.reason.unknown")
                : String.join(System.lineSeparator(), details);
    }

    private String permissionValue(final String method, final JsonNode params) {
        if (COMMAND_APPROVAL.equals(method)) {
            final String command = commandFromParams(params);
            return command.isBlank() ? "command" : command;
        }
        if (FILE_CHANGE_APPROVAL.equals(method)) {
            return "file changes";
        }
        return "permissions";
    }

    private String permissionMatchedRule(final String method, final JsonNode params) {
        if (COMMAND_APPROVAL.equals(method)) {
            final JsonNode amendment = params.path("proposedExecpolicyAmendment");
            return amendment.isMissingNode() || amendment.isNull() ? "" : amendment.toString();
        }
        if (FILE_CHANGE_APPROVAL.equals(method)) {
            return params.path("grantRoot").asText("");
        }
        return "";
    }

    private String codexConfigPath() {
        return Path.of(System.getProperty("user.home", "."), ".codex", "config.toml")
                .toAbsolutePath()
                .normalize()
                .toString();
    }

    private String commandFromParams(final JsonNode params) {
        final JsonNode command = params.path("command");
        if (command.isArray()) {
            final List<String> parts = new ArrayList<>();
            command.forEach(part -> parts.add(part.asText("")));
            return String.join(" ", parts).trim();
        }
        return command.asText("");
    }
}
