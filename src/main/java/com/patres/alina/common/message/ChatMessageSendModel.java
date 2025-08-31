package com.patres.alina.common.message;

import static com.patres.alina.common.message.ChatMessageStyleType.NONE;

public record ChatMessageSendModel(
        String content,
        String chatThreadId,
        String commandId,
        ChatMessageStyleType styleType
) {

    public ChatMessageSendModel(
            String content,
            String chatThreadId,
            String commandId
    ) {
        this(content, chatThreadId, commandId, NONE);
    }
}
