package com.patres.alina.server.thread;

import com.patres.alina.common.thread.ChatThreadResponse;
import org.springframework.stereotype.Component;

@Component
public class ChatThreadFacade {

    private final ChatThreadService chatThreadService;

    public ChatThreadFacade(final ChatThreadService chatThreadService) {
        this.chatThreadService = chatThreadService;
    }

    public void setModifiedAt(final String chatThreadId) {
        chatThreadService.setModifiedAt(chatThreadId);
    }

    public ChatThreadResponse createNewChatThread() {
        return chatThreadService.createNewChatThread();
    }
}
