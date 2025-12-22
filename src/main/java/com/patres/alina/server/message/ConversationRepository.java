package com.patres.alina.server.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.server.storage.JsonLinesRepository;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Conversation-based chat message repository using JSONL files.
 * Each conversation is stored in a separate .jsonl file: conversations/{threadId}.jsonl
 * New messages are simply appended to the end of the file.
 */
public class ConversationRepository implements ChatMessageStorageRepository {

    private final Path conversationsDir;
    private final ObjectMapper objectMapper;

    public ConversationRepository(final Path conversationsDir, final ObjectMapper objectMapper) {
        this.conversationsDir = conversationsDir;
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(final ChatMessage message) {
        if (message.chatThreadId() == null) {
            throw new IllegalArgumentException("chatThreadId cannot be null");
        }

        final JsonLinesRepository<ChatMessage, String> repo = getConversationRepository(message.chatThreadId());
        repo.save(message);
    }

    @Override
    public List<ChatMessage> findAllByThreadId(final String chatThreadId) {
        return getConversationMessages(chatThreadId);
    }

    @Override
    public List<ChatMessage> findMessagesWithContent(final String chatThreadId, final Set<ChatMessageRole> roles) {
        return getConversationMessages(chatThreadId).stream()
                .filter(this::hasContent)
                .filter(msg -> hasMatchingRole(msg, roles))
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessage> findLastMessagesForContext(final String chatThreadId, final Set<ChatMessageRole> roles, final int limit) {
        return getConversationMessages(chatThreadId).stream()
                .filter(msg -> hasMatchingRole(msg, roles))
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    if (limit <= 0) {
                        return List.of();
                    }
                    final int from = Math.max(0, list.size() - limit);
                    return list.subList(from, list.size());
                }));
    }

    @Override
    public void deleteByThreadId(final String chatThreadId) {
        final JsonLinesRepository<ChatMessage, String> repo = getConversationRepository(chatThreadId);
        repo.deleteAll();
    }

    @Override
    public void deleteMessage(final String chatThreadId, final String messageId) {
        final JsonLinesRepository<ChatMessage, String> repo = getConversationRepository(chatThreadId);
        repo.deleteById(messageId);
    }

    @Override
    public boolean conversationExists(final String chatThreadId) {
        final JsonLinesRepository<ChatMessage, String> repo = getConversationRepository(chatThreadId);
        return repo.count() > 0;
    }

    private JsonLinesRepository<ChatMessage, String> getConversationRepository(final String chatThreadId) {
        final Path conversationFile = conversationsDir.resolve(chatThreadId + ".jsonl");
        return new JsonLinesRepository<>(conversationFile, ChatMessage.class, objectMapper);
    }

    private List<ChatMessage> getConversationMessages(final String chatThreadId) {
        final JsonLinesRepository<ChatMessage, String> repo = getConversationRepository(chatThreadId);
        return repo.findAll();
    }

    private boolean hasContent(final ChatMessage message) {
        return message.content() != null && !message.content().trim().isEmpty();
    }

    private boolean hasMatchingRole(final ChatMessage message, final Set<ChatMessageRole> roles) {
        return message.role() != null && roles.stream()
                .anyMatch(role -> role.getChatMessageRole().equals(message.role().getValue()));
    }
}
