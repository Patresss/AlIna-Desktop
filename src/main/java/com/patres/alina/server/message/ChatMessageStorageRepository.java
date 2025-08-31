package com.patres.alina.server.message;

import com.patres.alina.common.message.ChatMessageRole;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

import static com.patres.alina.common.message.ChatMessageRole.*;

/**
 * Interface for chat message storage operations
 */
public interface ChatMessageStorageRepository {
    
    void save(ChatMessageEntry entry);
    
    /**
     * Find messages with content by thread ID and roles
     */
    List<ChatMessageEntry> findMessagesWithContent(String chatThreadId, Set<ChatMessageRole> roles);
    
    /**
     * Find messages to display in UI (USER and ASSISTANT roles with content)
     */
    default List<ChatMessageEntry> findMessagesToDisplay(String chatThreadId) {
        return findMessagesWithContent(chatThreadId, Set.of(USER, ASSISTANT));
    }
    
    /**
     * Find messages for AI context with pagination
     */
    List<ChatMessageEntry> findMessagesForContext(String chatThreadId, Set<ChatMessageRole> roles, Pageable pageable);
            
    /**
     * Find messages for AI context (all roles)
     */
    default List<ChatMessageEntry> findMessagesForContext(String chatThreadId, Pageable pageable) {
        return findMessagesForContext(chatThreadId, Set.of(USER, ASSISTANT, SYSTEM, FUNCTION), pageable);
    }
    
    /**
     * Delete all messages for a conversation thread
     */
    void deleteByThreadId(String chatThreadId);
    
    /**
     * Check if conversation has any messages
     */
    boolean conversationExists(String chatThreadId);
}