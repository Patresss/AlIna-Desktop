package com.patres.alina.server.message;

import com.patres.alina.common.message.ChatMessageRole;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

import static com.patres.alina.common.message.ChatMessageRole.*;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessageEntry, String> {

    List<ChatMessageEntry> findChatMessageEntriesByChatThreadIdAndContentIsNotNullAndRoleInOrderByCreatedAt(String chatThreadId, Set<ChatMessageRole> role);

    default List<ChatMessageEntry> findChatMessagesToDisplay(String chatThreadId) {
        return findChatMessageEntriesByChatThreadIdAndContentIsNotNullAndRoleInOrderByCreatedAt(chatThreadId, Set.of(USER, ASSISTANT));
    }


    List<ChatMessageEntry> findChatMessageEntriesByChatThreadIdAndRoleInOrderByCreatedAt(String chatThreadId, Set<ChatMessageRole> role, Pageable pageable);
    default List<ChatMessageEntry> findChatMessagesForContext(String chatThreadId, Pageable pageable) {
        return findChatMessageEntriesByChatThreadIdAndRoleInOrderByCreatedAt(chatThreadId, Set.of(USER, ASSISTANT, SYSTEM, FUNCTION), pageable);
    }

    void deleteByChatThreadId(String chatThreadId);

}