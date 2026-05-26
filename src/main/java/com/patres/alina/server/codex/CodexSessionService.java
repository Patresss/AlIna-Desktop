package com.patres.alina.server.codex;

import com.patres.alina.common.ai.AiProvider;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.ai.AiSessionService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CodexSessionService implements AiSessionService {

    private final CodexConversationStore conversationStore;

    public CodexSessionService(final CodexConversationStore conversationStore) {
        this.conversationStore = conversationStore;
    }

    @Override
    public AiProvider provider() {
        return AiProvider.CODEX;
    }

    @Override
    public String resolveSessionId(final String chatThreadId) {
        return conversationStore.resolveCodexSessionId(chatThreadId);
    }

    @Override
    public Optional<ChatThread> getSessionByChatThreadId(final String chatThreadId) {
        return conversationStore.getThread(chatThreadId);
    }

    @Override
    public List<ChatThread> getSessions() {
        return conversationStore.getThreads();
    }

    @Override
    public Optional<ChatThread> getSession(final String sessionId) {
        return conversationStore.getThread(sessionId);
    }

    @Override
    public void deleteSession(final String sessionId) {
        conversationStore.delete(sessionId);
    }

    @Override
    public void renameSession(final String sessionId, final String newTitle) {
        conversationStore.rename(sessionId, newTitle);
    }

    @Override
    public List<ChatMessageResponseModel> getMessages(final String sessionId) {
        return conversationStore.getMessages(sessionId);
    }

    public void ensureConversation(final String chatThreadId,
                                   final String title,
                                   final String firstUserMessage) {
        conversationStore.ensureConversation(chatThreadId, title, firstUserMessage);
    }

    public void setCodexSessionId(final String chatThreadId, final String codexSessionId) {
        conversationStore.setCodexSessionId(chatThreadId, codexSessionId);
    }

    public void clearCodexSessionId(final String chatThreadId) {
        conversationStore.clearCodexSessionId(chatThreadId);
    }

    public void addUserMessage(final String chatThreadId, final String content) {
        conversationStore.addMessage(chatThreadId, com.patres.alina.common.message.ChatMessageRole.USER, content);
    }

    public void addAssistantMessage(final String chatThreadId, final String content) {
        conversationStore.addMessage(chatThreadId, com.patres.alina.common.message.ChatMessageRole.ASSISTANT, content);
    }
}
