package com.patres.alina.server.message;

import com.patres.alina.server.storage.Entity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Chat message entity for local storage.
 * Wraps ChatMessageEntry to implement Entity interface.
 */
public class ChatMessageEntity implements Entity<String> {
    
    private final ChatMessageEntry chatMessage;
    
    @JsonCreator
    public ChatMessageEntity(@JsonProperty("chatMessage") ChatMessageEntry chatMessage) {
        this.chatMessage = chatMessage;
    }
    
    @Override
    public String getId() {
        return chatMessage.id();
    }
    
    public ChatMessageEntry getChatMessage() {
        return chatMessage;
    }
    
    // Convenience methods for direct access to ChatMessageEntry fields
    public String getChatThreadId() {
        return chatMessage.chatThreadId();
    }
    
    public String getContent() {
        return chatMessage.content();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChatMessageEntity that = (ChatMessageEntity) obj;
        return chatMessage.equals(that.chatMessage);
    }
    
    @Override
    public int hashCode() {
        return chatMessage.hashCode();
    }
    
    @Override
    public String toString() {
        return "ChatMessageEntity{" + chatMessage + "}";
    }
}