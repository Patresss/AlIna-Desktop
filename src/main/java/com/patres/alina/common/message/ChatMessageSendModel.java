package com.patres.alina.common.message;

import static com.patres.alina.common.message.ChatMessageStyleType.NONE;

public record ChatMessageSendModel(
        String content,
        String chatThreadId,
        String pluginId,
        ChatMessageStyleType styleType
) {

    public ChatMessageSendModel(
            String content,
            String chatThreadId,
            String pluginId
    ) {
        this(content, chatThreadId, pluginId, NONE);
    }
}
