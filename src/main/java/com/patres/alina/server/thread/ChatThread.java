package com.patres.alina.server.thread;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Document
public record ChatThread(
        @Id
        String id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {

    private static final DateTimeFormatter NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd (HH:mm:ss)");
    private static final DateTimeFormatter ID_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");

    public static ChatThread newChatThread() {
        LocalDateTime now = LocalDateTime.now();
        return new ChatThread(
                now.format(ID_FORMATTER),
                now.format(NAME_FORMATTER),
                now,
                null);
    }
}
