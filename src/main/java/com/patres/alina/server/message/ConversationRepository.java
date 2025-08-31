package com.patres.alina.server.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.server.storage.JsonLinesRepository;
import org.springframework.data.domain.Pageable;

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
    
    public ConversationRepository(Path conversationsDir, ObjectMapper objectMapper) {
        this.conversationsDir = conversationsDir;
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void save(ChatMessageEntry entry) {
        if (entry.chatThreadId() == null) {
            throw new IllegalArgumentException("chatThreadId cannot be null");
        }
        
        JsonLinesRepository<ChatMessageEntity, String> conversationRepo = getConversationRepository(entry.chatThreadId());
        
        ChatMessageEntity entity = new ChatMessageEntity(entry);
        conversationRepo.save(entity);
    }
    
    @Override
    public List<ChatMessageEntry> findMessagesWithContent(String chatThreadId, Set<ChatMessageRole> roles) {
        return getConversationMessages(chatThreadId).stream()
            .filter(this::hasContent)
            .filter(msg -> hasMatchingRole(msg, roles))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<ChatMessageEntry> findMessagesForContext(String chatThreadId, Set<ChatMessageRole> roles, Pageable pageable) {
        List<ChatMessageEntry> filtered = getConversationMessages(chatThreadId).stream()
            .filter(msg -> hasMatchingRole(msg, roles))
            .collect(Collectors.toList());
        
        return applyPagination(filtered, pageable);
    }
    
    @Override
    public void deleteByThreadId(String chatThreadId) {
        JsonLinesRepository<ChatMessageEntity, String> conversationRepo = getConversationRepository(chatThreadId);
        conversationRepo.deleteAll();
    }
    
    @Override
    public boolean conversationExists(String chatThreadId) {
        JsonLinesRepository<ChatMessageEntity, String> conversationRepo = getConversationRepository(chatThreadId);
        return conversationRepo.count() > 0;
    }
    
    /**
     * Get JSONL repository for a specific conversation
     */
    private JsonLinesRepository<ChatMessageEntity, String> getConversationRepository(String chatThreadId) {
        Path conversationFile = conversationsDir.resolve(chatThreadId + ".jsonl");
        return new JsonLinesRepository<>(conversationFile, ChatMessageEntity.class, objectMapper);
    }
    
    /**
     * Get all messages for a conversation (in chronological order from JSONL file)
     */
    private List<ChatMessageEntry> getConversationMessages(String chatThreadId) {
        JsonLinesRepository<ChatMessageEntity, String> conversationRepo = getConversationRepository(chatThreadId);
        
        return conversationRepo.findAll().stream()
            .map(ChatMessageEntity::getChatMessage)
            .collect(Collectors.toList());
    }
    
    /**
     * Check if message has non-empty content
     */
    private boolean hasContent(ChatMessageEntry message) {
        return message.content() != null && !message.content().trim().isEmpty();
    }
    
    /**
     * Check if message role matches any of the specified roles
     */
    private boolean hasMatchingRole(ChatMessageEntry message, Set<ChatMessageRole> roles) {
        return message.role() != null && roles.stream()
            .anyMatch(role -> role.getChatMessageRole().equals(message.role().getValue()));
    }
    
    /**
     * Apply pagination to a list of messages
     */
    private List<ChatMessageEntry> applyPagination(List<ChatMessageEntry> messages, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), messages.size());
        if (start >= messages.size()) {
            return List.of();
        }
        return messages.subList(start, end);
    }
}