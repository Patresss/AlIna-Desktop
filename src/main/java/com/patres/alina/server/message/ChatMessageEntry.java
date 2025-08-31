package com.patres.alina.server.message;

import com.patres.alina.common.message.ChatMessageStyleType;
import org.springframework.ai.chat.messages.MessageType;

import java.time.LocalDateTime;

public record ChatMessageEntry(
        String id,
        String chatThreadId,
        String content,
        MessageType role,
        ChatMessageStyleType styleType,
        LocalDateTime createdAt,
        String contentWithContext,
        String commandId,
        String name
) {
}
