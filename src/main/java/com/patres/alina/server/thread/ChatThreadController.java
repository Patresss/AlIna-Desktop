package com.patres.alina.server.thread;

import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.common.thread.ChatThreadRenameRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class ChatThreadController {

    private final ChatThreadService chatThreadService;

    public ChatThreadController(final ChatThreadService chatThreadService) {
        this.chatThreadService = chatThreadService;
    }

    public Optional<ChatThread> getChatThread(final String chatThreadId) {
        return chatThreadService.getChatThread(chatThreadId);
    }

    public List<ChatThread> getChatThreads() {
        return chatThreadService.getChatThreads();
    }

    public ChatThread createNewChatThread() {
        return chatThreadService.createNewChatThread();
    }

    public void deleteChatThread(final String chatThreadId) {
        chatThreadService.deleteChatThread(chatThreadId);
    }

    public void renameChatThread(final ChatThreadRenameRequest chatThreadRenameRequest) {
        chatThreadService.renameChatThread(chatThreadRenameRequest.id(), chatThreadRenameRequest.name());
    }
}
