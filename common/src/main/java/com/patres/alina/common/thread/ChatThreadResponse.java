package com.patres.alina.common.thread;


import java.time.LocalDateTime;

public record ChatThreadResponse(
        String id,
        String name,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {

}
