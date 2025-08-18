package com.patres.alina.server.thread;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Document
public record ChatThread(
        @Id
        String id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {

    private static final DateTimeFormatter NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd (HH:mm:ss)");

    public static ChatThread newChatThread() {
        LocalDateTime now = LocalDateTime.now();
        return new ChatThread(
                UUID.randomUUID().toString(),
                now.format(NAME_FORMATTER),
                now,
                null);
    }
}
