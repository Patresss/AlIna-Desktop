package com.patres.alina.server.thread;

import com.patres.alina.common.thread.ChatThread;
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

    public ChatThread createNewChatThread() {
        return chatThreadService.createNewChatThread();
    }
}
