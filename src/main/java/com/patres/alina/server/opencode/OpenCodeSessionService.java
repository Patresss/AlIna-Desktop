package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.thread.ChatThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.patres.alina.server.opencode.OpenCodeSessionRegistry;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Provides session and message data sourced directly from the OpenCode server API.
 * Replaces local .jsonl file storage for thread listing, history display, and lifecycle management.
 */
@Service
public class OpenCodeSessionService {

    private static final Logger logger = LoggerFactory.getLogger(OpenCodeSessionService.class);

    private final OpenCodeHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final OpenCodeSessionRegistry sessionRegistry;

    public OpenCodeSessionService(final OpenCodeHttpClient httpClient,
                                  final ObjectMapper objectMapper,
                                  final OpenCodeSessionRegistry sessionRegistry) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Resolves the OpenCode sessionId for a given AlIna chatThreadId.
     * Returns null if no mapping exists yet.
     */
    public String resolveSessionId(final String chatThreadId) {
        return sessionRegistry.get(chatThreadId);
    }

    /**
     * Fetches a ChatThread by AlIna chatThreadId, resolving via the session registry.
     */
    public Optional<ChatThread> getSessionByChatThreadId(final String chatThreadId) {
        final String sessionId = sessionRegistry.get(chatThreadId);
        if (sessionId == null) {
            return Optional.empty();
        }
        return getSession(sessionId);
    }

    /**
     * Returns all sessions visible to the current workspace, sorted by most recently updated.
     */
    public List<ChatThread> getSessions() {
        try {
            final JsonNode response = httpClient.get("/session");
            final List<ChatThread> threads = new ArrayList<>();
            if (response.isArray()) {
                for (final JsonNode node : response) {
                    toChatThread(node).ifPresent(threads::add);
                }
            }
            // API already returns sessions sorted by updated desc; preserve that order
            return threads;
        } catch (Exception e) {
            logger.warn("Failed to fetch sessions from OpenCode", e);
            return Collections.emptyList();
        }
    }

    /**
     * Returns a single session by its OpenCode session ID (which equals the AlIna chatThreadId).
     */
    public Optional<ChatThread> getSession(final String sessionId) {
        try {
            final JsonNode response = httpClient.get("/session/" + sessionId);
            return toChatThread(response);
        } catch (Exception e) {
            logger.warn("Failed to fetch session {} from OpenCode", sessionId, e);
            return Optional.empty();
        }
    }

    /**
     * Deletes a session from the OpenCode server.
     */
    public void deleteSession(final String sessionId) {
        try {
            httpClient.delete("/session/" + sessionId);
        } catch (Exception e) {
            logger.warn("Failed to delete session {} from OpenCode", sessionId, e);
        }
    }

    /**
     * Renames a session title via the OpenCode server.
     */
    public void renameSession(final String sessionId, final String newTitle) {
        try {
            final JsonNode body = objectMapper.createObjectNode().put("title", newTitle);
            httpClient.patch("/session/" + sessionId, body);
        } catch (Exception e) {
            logger.warn("Failed to rename session {} to '{}' in OpenCode", sessionId, newTitle, e);
        }
    }

    /**
     * Returns the displayable messages (user + assistant text) for a session,
     * in conversation order.
     */
    public List<ChatMessageResponseModel> getMessages(final String sessionId) {
        try {
            final JsonNode response = httpClient.get("/session/" + sessionId + "/message");
            final List<ChatMessageResponseModel> result = new ArrayList<>();
            if (!response.isArray()) {
                return result;
            }
            for (final JsonNode messageNode : response) {
                toMessageModel(messageNode, sessionId).ifPresent(result::add);
            }
            return result;
        } catch (Exception e) {
            logger.warn("Failed to fetch messages for session {} from OpenCode", sessionId, e);
            return Collections.emptyList();
        }
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private Optional<ChatThread> toChatThread(final JsonNode node) {
        final String id = node.path("id").asText(null);
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        final String title = node.path("title").asText(id);
        final long createdMs = node.path("time").path("created").asLong(0);
        final long updatedMs = node.path("time").path("updated").asLong(0);
        final LocalDateTime createdAt = createdMs > 0 ? toLocalDateTime(createdMs) : null;
        final LocalDateTime updatedAt = updatedMs > 0 ? toLocalDateTime(updatedMs) : null;
        return Optional.of(new ChatThread(id, title, createdAt, updatedAt));
    }

    private Optional<ChatMessageResponseModel> toMessageModel(final JsonNode messageNode, final String sessionId) {
        final JsonNode info = messageNode.path("info");
        final String roleStr = info.path("role").asText("");
        final ChatMessageRole role = parseRole(roleStr);
        if (role == null) {
            return Optional.empty();
        }

        // Collect only text parts, skip step-start, tool, patch, etc.
        final StringBuilder textBuilder = new StringBuilder();
        final JsonNode parts = messageNode.path("parts");
        if (parts.isArray()) {
            for (final JsonNode part : parts) {
                if ("text".equals(part.path("type").asText())) {
                    final String text = part.path("text").asText("");
                    if (!text.isBlank()) {
                        textBuilder.append(text);
                    }
                }
            }
        }

        final String text = textBuilder.toString();
        if (text.isBlank()) {
            return Optional.empty();
        }

        final long createdMs = info.path("time").path("created").asLong(0);
        final LocalDateTime createdAt = createdMs > 0 ? toLocalDateTime(createdMs) : null;

        return Optional.of(new ChatMessageResponseModel(
                text,
                role,
                createdAt,
                ChatMessageStyleType.NONE,
                sessionId,
                null
        ));
    }

    private ChatMessageRole parseRole(final String role) {
        return switch (role) {
            case "user" -> ChatMessageRole.USER;
            case "assistant" -> ChatMessageRole.ASSISTANT;
            default -> null;
        };
    }

    private LocalDateTime toLocalDateTime(final long epochMillis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
    }
}
