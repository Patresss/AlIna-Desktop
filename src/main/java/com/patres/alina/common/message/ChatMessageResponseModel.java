package com.patres.alina.common.message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;

@JsonSerialize
public record ChatMessageResponseModel(
        String content,
        ChatMessageRole seder,
        LocalDateTime createdAt,
        ChatMessageStyleType styleType,
        String chatThreadId,
        CommandUsageInfo commandUsageInfo
){
}
