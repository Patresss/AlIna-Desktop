package com.patres.alina.server.ai;

import com.patres.alina.common.ai.AiProvider;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.thread.ChatThread;

import java.util.List;
import java.util.Optional;

public interface AiSessionService {

    AiProvider provider();

    String resolveSessionId(String chatThreadId);

    Optional<ChatThread> getSessionByChatThreadId(String chatThreadId);

    List<ChatThread> getSessions();

    Optional<ChatThread> getSession(String sessionId);

    void deleteSession(String sessionId);

    void renameSession(String sessionId, String newTitle);

    List<ChatMessageResponseModel> getMessages(String sessionId);
}
