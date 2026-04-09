package com.patres.alina.server.message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.server.storage.Entity;

import java.time.LocalDateTime;

@JsonSerialize
public record ChatMessage(
        String id,
        String chatThreadId,
        String content,
        ChatMessageRole role,
        ChatMessageStyleType styleType,
        LocalDateTime createdAt,
        String contentWithContext,
        String commandId,
        String name
) implements Entity<String> {

    @Override
    public String getId() {
        return id;
    }
}
