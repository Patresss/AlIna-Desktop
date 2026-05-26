package com.patres.alina.server.thread;

import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.ai.AiRuntimeRegistry;
import com.patres.alina.server.ai.AiSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.patres.alina.common.thread.ChatThread.newChatThread;

@Service
public class ChatThreadService {

    private static final Logger logger = LoggerFactory.getLogger(ChatThreadService.class);

    private final AiRuntimeRegistry aiRuntimeRegistry;

    public ChatThreadService(final AiRuntimeRegistry aiRuntimeRegistry) {
        this.aiRuntimeRegistry = aiRuntimeRegistry;
    }

    public ChatThread createNewChatThread() {
        logger.info("Creating new chat thread");
        final ChatThread chatThread = newChatThread();
        logger.info("Created new chat thread: id=`{}`, name=`{}`", chatThread.id(), chatThread.name());
        return chatThread;
    }

    public Optional<ChatThread> getChatThread(final String chatThreadId) {
        logger.debug("Getting chat thread: {}", chatThreadId);
        return aiRuntimeRegistry.currentSessionService().getSessionByChatThreadId(chatThreadId);
    }

    public List<ChatThread> getChatThreads() {
        aiRuntimeRegistry.currentRuntime().prepareForHistoryAccess();
        final List<ChatThread> threads = aiRuntimeRegistry.currentSessionService().getSessions();
        logger.info("Found {} chat threads from {}", threads.size(), aiRuntimeRegistry.currentProvider());
        return threads;
    }

    /** No-op: modification time is owned by the active AI runtime/session store. */
    public void setModifiedAt(final String chatThreadId) {
        // intentionally empty
    }

    public void deleteChatThread(final String chatThreadId) {
        final AiSessionService sessionService = aiRuntimeRegistry.currentSessionService();
        final String sessionId = sessionService.resolveSessionId(chatThreadId);
        sessionService.deleteSession(sessionId != null ? sessionId : chatThreadId);
    }

    public void renameChatThread(final String chatThreadId, final String newName) {
        final AiSessionService sessionService = aiRuntimeRegistry.currentSessionService();
        final String sessionId = sessionService.resolveSessionId(chatThreadId);
        sessionService.renameSession(sessionId != null ? sessionId : chatThreadId, newName);
    }
}
