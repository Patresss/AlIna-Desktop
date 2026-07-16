package com.patres.alina.server.agent;

import com.patres.alina.common.agent.AgentBackend;
import com.patres.alina.common.agent.AgentRuntimeStatus;
import com.patres.alina.common.interaction.AgentInteractionResolutionModel;
import com.patres.alina.common.interaction.AgentInteractionResponse;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.thread.ChatThread;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

public interface AgentRuntime {

    AgentBackend backend();

    boolean isEnabled();

    Flux<String> sendMessageStream(AgentMessageRequest request);

    void cancelStreaming(String chatThreadId);

    boolean ownsAgentInteraction(String requestId);

    AgentInteractionResolutionModel resolveAgentInteraction(String requestId, AgentInteractionResponse response);

    List<String> getAvailableModels();

    String resolveEffectiveModelIdentifier();

    String getModelUsedForThread(String threadId);

    String getAgentUsedForThread(String threadId);

    long getTokensTotalForThread(String threadId);

    double getCostForThread(String threadId);

    AgentRuntimeStatus getRuntimeStatus();

    void prepareForFreshChat();

    String getSessionExternalUri(String chatThreadId);

    Optional<ChatThread> getChatThread(String chatThreadId);

    List<ChatThread> getChatThreads();

    List<ChatMessageResponseModel> getMessagesByThreadId(String chatThreadId);

    void deleteChatThread(String chatThreadId);

    void renameChatThread(String chatThreadId, String newName);
}
