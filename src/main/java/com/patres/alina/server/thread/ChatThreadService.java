package com.patres.alina.server.thread;

import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.agent.AgentRuntime;
import com.patres.alina.server.agent.AgentRuntimeSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.patres.alina.common.thread.ChatThread.newChatThread;

@Service
public class ChatThreadService {

    private static final Logger logger = LoggerFactory.getLogger(ChatThreadService.class);

    private final AgentRuntimeSelector agentRuntimeSelector;

    public ChatThreadService(final AgentRuntimeSelector agentRuntimeSelector) {
        this.agentRuntimeSelector = agentRuntimeSelector;
    }

    public ChatThread createNewChatThread() {
        logger.info("Creating new chat thread");
        final ChatThread chatThread = newChatThread();
        logger.info("Created new chat thread: id=`{}`, name=`{}`", chatThread.id(), chatThread.name());
        return chatThread;
    }

    public Optional<ChatThread> getChatThread(final String chatThreadId) {
        logger.debug("Getting chat thread: {}", chatThreadId);
        return agentRuntimeSelector.active().getChatThread(chatThreadId);
    }

    public List<ChatThread> getChatThreads() {
        final AgentRuntime runtime = agentRuntimeSelector.active();
        final List<ChatThread> threads = runtime.getChatThreads();
        logger.info("Found {} chat threads from {}", threads.size(), runtime.backend().displayName());
        return threads;
    }

    /** No-op: modification time is owned by OpenCode server now. */
    public void setModifiedAt(final String chatThreadId) {
        // intentionally empty — OpenCode tracks session timestamps
    }

    public void deleteChatThread(final String chatThreadId) {
        agentRuntimeSelector.active().deleteChatThread(chatThreadId);
    }

    public void renameChatThread(final String chatThreadId, final String newName) {
        agentRuntimeSelector.active().renameChatThread(chatThreadId, newName);
    }
}
