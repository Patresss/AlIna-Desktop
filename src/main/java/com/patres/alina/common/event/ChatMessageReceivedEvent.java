package com.patres.alina.common.event;

import com.patres.alina.common.message.ChatMessageResponseModel;

public final class ChatMessageReceivedEvent extends Event {

    public ChatMessageReceivedEvent(ChatMessageResponseModel chatMessage) {
        this.chatMessage = chatMessage;
    }

    private final ChatMessageResponseModel chatMessage;

    public ChatMessageResponseModel getChatMessage() {
        return chatMessage;
    }

}
