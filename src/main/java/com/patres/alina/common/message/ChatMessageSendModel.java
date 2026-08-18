package com.patres.alina.common.message;

import java.util.List;

import static com.patres.alina.common.message.ChatMessageStyleType.NONE;

public record ChatMessageSendModel(
        String content,
        String chatThreadId,
        String commandId,
        ChatMessageStyleType styleType,
        OnMessageCompleteCallback onComplete,
        String model,
        String effort,
        List<ImageAttachment> imageAttachments
) {

    public ChatMessageSendModel(
            String content,
            String chatThreadId,
            String commandId,
            ChatMessageStyleType styleType,
            OnMessageCompleteCallback onComplete,
            String model
    ) {
        this(content, chatThreadId, commandId, styleType, onComplete, model, null, List.of());
    }

    public ChatMessageSendModel(
            String content,
            String chatThreadId,
            String commandId,
            ChatMessageStyleType styleType,
            OnMessageCompleteCallback onComplete,
            String model,
            String effort
    ) {
        this(content, chatThreadId, commandId, styleType, onComplete, model, effort, List.of());
    }

    public ChatMessageSendModel(
            String content,
            String chatThreadId,
            String commandId,
            ChatMessageStyleType styleType,
            OnMessageCompleteCallback onComplete,
            String model,
            List<ImageAttachment> imageAttachments
    ) {
        this(content, chatThreadId, commandId, styleType, onComplete, model, null, imageAttachments);
    }

    public ChatMessageSendModel(
            String content,
            String chatThreadId,
            String commandId
    ) {
        this(content, chatThreadId, commandId, NONE, null, null, null, List.of());
    }

    public ChatMessageSendModel(
            String content,
            String chatThreadId,
            String commandId,
            OnMessageCompleteCallback onComplete
    ) {
        this(content, chatThreadId, commandId, NONE, onComplete, null, null, List.of());
    }

    public ChatMessageSendModel(
            String content,
            String chatThreadId,
            String commandId,
            ChatMessageStyleType styleType,
            OnMessageCompleteCallback onComplete
    ) {
        this(content, chatThreadId, commandId, styleType, onComplete, null, null, List.of());
    }
}
