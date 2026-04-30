package com.patres.alina.server.thread;

import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.opencode.OpenCodeSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.patres.alina.common.thread.ChatThread.newChatThread;

@Service
public class ChatThreadService {

    private static final Logger logger = LoggerFactory.getLogger(ChatThreadService.class);

    private final OpenCodeSessionService openCodeSessionService;

    public ChatThreadService(final OpenCodeSessionService openCodeSessionService) {
        this.openCodeSessionService = openCodeSessionService;
    }

    public ChatThread createNewChatThread() {
        logger.info("Creating new chat thread");
        final ChatThread chatThread = newChatThread();
        logger.info("Created new chat thread: id=`{}`, name=`{}`", chatThread.id(), chatThread.name());
        return chatThread;
    }

    public Optional<ChatThread> getChatThread(final String chatThreadId) {
        logger.debug("Getting chat thread: {}", chatThreadId);
        return openCodeSessionService.getSessionByChatThreadId(chatThreadId);
    }

    public List<ChatThread> getChatThreads() {
        final List<ChatThread> threads = openCodeSessionService.getSessions();
        logger.info("Found {} chat threads from OpenCode", threads.size());
        return threads;
    }

    /** No-op: modification time is owned by OpenCode server now. */
    public void setModifiedAt(final String chatThreadId) {
        // intentionally empty — OpenCode tracks session timestamps
    }

    public void deleteChatThread(final String chatThreadId) {
        final String sessionId = openCodeSessionService.resolveSessionId(chatThreadId);
        openCodeSessionService.deleteSession(sessionId != null ? sessionId : chatThreadId);
    }

    public void renameChatThread(final String chatThreadId, final String newName) {
        final String sessionId = openCodeSessionService.resolveSessionId(chatThreadId);
        openCodeSessionService.renameSession(sessionId != null ? sessionId : chatThreadId, newName);
    }
}
