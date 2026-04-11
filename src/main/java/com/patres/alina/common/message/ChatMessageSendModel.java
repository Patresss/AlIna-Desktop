package com.patres.alina.common.message;

import static com.patres.alina.common.message.ChatMessageStyleType.NONE;

public record ChatMessageSendModel(
        String content,
        String chatThreadId,
        String commandId,
        ChatMessageStyleType styleType,
        OnMessageCompleteCallback onComplete,
        String model
) {

    public ChatMessageSendModel(
            String content,
            String chatThreadId,
            String commandId
    ) {
        this(content, chatThreadId, commandId, NONE, null, null);
    }

    public ChatMessageSendModel(
            String content,
            String chatThreadId,
            String commandId,
            OnMessageCompleteCallback onComplete
    ) {
        this(content, chatThreadId, commandId, NONE, onComplete, null);
    }

    public ChatMessageSendModel(
            String content,
            String chatThreadId,
            String commandId,
            ChatMessageStyleType styleType,
            OnMessageCompleteCallback onComplete
    ) {
        this(content, chatThreadId, commandId, styleType, onComplete, null);
    }
}
