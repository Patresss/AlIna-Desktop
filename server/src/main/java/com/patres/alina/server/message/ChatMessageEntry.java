package com.patres.alina.server.message;

import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageStyleType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
public record ChatMessageEntry(
        @Id
        String id,
        @Indexed
        String chatThreadId,
        String content,
        ChatMessageRole role,
        ChatMessageStyleType styleType,
        @Indexed
        LocalDateTime createdAt,
        String contentWithContext,
        String pluginId,
        String name,
        String functionName,
        String functionArguments
) {
}
