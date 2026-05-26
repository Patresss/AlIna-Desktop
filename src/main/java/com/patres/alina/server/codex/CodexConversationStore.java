package com.patres.alina.server.codex;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.storage.AppPaths;
import com.patres.alina.common.thread.ChatThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CodexConversationStore {

    private static final Logger logger = LoggerFactory.getLogger(CodexConversationStore.class);
    private static final int TITLE_LIMIT = 80;

    private final ObjectMapper objectMapper;
    private final Path storePath = AppPaths.resolve("conversations/codex-conversations.json");
    private Map<String, CodexConversation> conversations;

    public CodexConversationStore(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public synchronized CodexConversation ensureConversation(final String threadId,
                                                            final String title,
                                                            final String firstUserMessage) {
        final Map<String, CodexConversation> mapping = load();
        CodexConversation conversation = mapping.get(threadId);
        if (conversation != null) {
            return conversation;
        }
        final LocalDateTime now = LocalDateTime.now();
        conversation = new CodexConversation(
                threadId,
                null,
                resolveInitialTitle(title, firstUserMessage, threadId),
                now,
                now,
                new ArrayList<>()
        );
        mapping.put(threadId, conversation);
        save(mapping);
        return conversation;
    }

    public synchronized Optional<CodexConversation> get(final String threadId) {
        return Optional.ofNullable(load().get(threadId));
    }

    public synchronized List<ChatThread> getThreads() {
        return load().values().stream()
                .sorted(Comparator.comparing(CodexConversation::updatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .map(conversation -> new ChatThread(
                        conversation.id(),
                        conversation.title(),
                        conversation.createdAt(),
                        conversation.updatedAt()
                ))
                .toList();
    }

    public synchronized Optional<ChatThread> getThread(final String threadId) {
        return get(threadId)
                .map(conversation -> new ChatThread(
                        conversation.id(),
                        conversation.title(),
                        conversation.createdAt(),
                        conversation.updatedAt()
                ));
    }

    public synchronized void setCodexSessionId(final String threadId, final String codexSessionId) {
        if (threadId == null || threadId.isBlank() || codexSessionId == null || codexSessionId.isBlank()) {
            return;
        }
        final Map<String, CodexConversation> mapping = load();
        final CodexConversation conversation = mapping.get(threadId);
        if (conversation == null || codexSessionId.equals(conversation.codexSessionId())) {
            return;
        }
        mapping.put(threadId, conversation.withCodexSessionId(codexSessionId));
        save(mapping);
    }

    public synchronized void clearCodexSessionId(final String threadId) {
        final Map<String, CodexConversation> mapping = load();
        final CodexConversation conversation = mapping.get(threadId);
        if (conversation == null || conversation.codexSessionId() == null) {
            return;
        }
        mapping.put(threadId, conversation.withCodexSessionId(null));
        save(mapping);
    }

    public synchronized void addMessage(final String threadId,
                                        final ChatMessageRole role,
                                        final String content) {
        if (threadId == null || threadId.isBlank() || role == null || content == null || content.isBlank()) {
            return;
        }
        final Map<String, CodexConversation> mapping = load();
        final CodexConversation existing = mapping.get(threadId);
        if (existing == null) {
            return;
        }
        final List<CodexMessage> messages = new ArrayList<>(existing.messages());
        messages.add(new CodexMessage(content, role, LocalDateTime.now()));
        final String title = shouldReplaceTitle(existing.title(), existing.id(), messages)
                ? buildTitle(content, existing.id())
                : existing.title();
        mapping.put(threadId, existing.withMessages(title, messages, LocalDateTime.now()));
        save(mapping);
    }

    public synchronized List<ChatMessageResponseModel> getMessages(final String threadId) {
        return get(threadId)
                .map(CodexConversation::messages)
                .orElse(List.of())
                .stream()
                .map(message -> new ChatMessageResponseModel(
                        message.content(),
                        message.role(),
                        message.createdAt(),
                        ChatMessageStyleType.NONE,
                        threadId,
                        null
                ))
                .toList();
    }

    public synchronized void delete(final String threadId) {
        final Map<String, CodexConversation> mapping = load();
        if (mapping.remove(threadId) != null) {
            save(mapping);
        }
    }

    public synchronized void rename(final String threadId, final String newTitle) {
        if (newTitle == null || newTitle.isBlank()) {
            return;
        }
        final Map<String, CodexConversation> mapping = load();
        final CodexConversation existing = mapping.get(threadId);
        if (existing == null) {
            return;
        }
        mapping.put(threadId, existing.withTitle(newTitle.trim()));
        save(mapping);
    }

    public synchronized String resolveCodexSessionId(final String threadId) {
        return get(threadId)
                .map(CodexConversation::codexSessionId)
                .orElse(null);
    }

    private Map<String, CodexConversation> load() {
        if (conversations != null) {
            return conversations;
        }
        try {
            final Path parent = storePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(storePath)) {
                conversations = new LinkedHashMap<>();
                return conversations;
            }
            conversations = new LinkedHashMap<>(objectMapper.readValue(
                    storePath.toFile(),
                    new TypeReference<Map<String, CodexConversation>>() {
                    }
            ));
            return conversations;
        } catch (IOException e) {
            logger.warn("Cannot load Codex conversations from {}", storePath, e);
            conversations = new LinkedHashMap<>();
            return conversations;
        }
    }

    private void save(final Map<String, CodexConversation> mapping) {
        try {
            final Path parent = storePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), mapping);
            conversations = new LinkedHashMap<>(mapping);
        } catch (IOException e) {
            logger.warn("Cannot save Codex conversations to {}", storePath, e);
        }
    }

    private String resolveInitialTitle(final String title,
                                       final String firstUserMessage,
                                       final String threadId) {
        if (title != null && !title.isBlank()) {
            return title.trim();
        }
        return buildTitle(firstUserMessage, threadId);
    }

    private String buildTitle(final String content, final String fallback) {
        if (content == null || content.isBlank()) {
            return fallback;
        }
        final String normalized = content.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= TITLE_LIMIT) {
            return normalized;
        }
        return normalized.substring(0, TITLE_LIMIT - 1).trim() + "...";
    }

    private boolean shouldReplaceTitle(final String title,
                                       final String id,
                                       final List<CodexMessage> messages) {
        return messages.size() == 1 && (title == null || title.isBlank() || title.equals(id));
    }

    public record CodexConversation(
            String id,
            String codexSessionId,
            String title,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<CodexMessage> messages
    ) {
        public CodexConversation {
            messages = messages == null ? new ArrayList<>() : new ArrayList<>(messages);
        }

        CodexConversation withCodexSessionId(final String value) {
            return new CodexConversation(id, value, title, createdAt, LocalDateTime.now(), messages);
        }

        CodexConversation withMessages(final String newTitle,
                                       final List<CodexMessage> newMessages,
                                       final LocalDateTime updatedAt) {
            return new CodexConversation(id, codexSessionId, newTitle, createdAt, updatedAt, newMessages);
        }

        CodexConversation withTitle(final String newTitle) {
            return new CodexConversation(id, codexSessionId, newTitle, createdAt, LocalDateTime.now(), messages);
        }
    }

    public record CodexMessage(
            String content,
            ChatMessageRole role,
            LocalDateTime createdAt
    ) {
    }
}
