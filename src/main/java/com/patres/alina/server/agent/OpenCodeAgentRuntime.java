package com.patres.alina.server.agent;

import com.patres.alina.common.agent.AgentBackend;
import com.patres.alina.common.agent.AgentRuntimeStatus;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.opencode.OpenCodeRuntimeStatus;
import com.patres.alina.common.permission.PermissionApprovalAction;
import com.patres.alina.common.permission.PermissionResolutionModel;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.opencode.OpenCodeRuntimeService;
import com.patres.alina.server.opencode.OpenCodeSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

@Component
public class OpenCodeAgentRuntime implements AgentRuntime {

    private static final Logger logger = LoggerFactory.getLogger(OpenCodeAgentRuntime.class);

    private final OpenCodeRuntimeService runtimeService;
    private final OpenCodeSessionService sessionService;

    public OpenCodeAgentRuntime(final OpenCodeRuntimeService runtimeService,
                                final OpenCodeSessionService sessionService) {
        this.runtimeService = runtimeService;
        this.sessionService = sessionService;
    }

    @Override
    public AgentBackend backend() {
        return AgentBackend.OPENCODE;
    }

    @Override
    public boolean isEnabled() {
        return runtimeService.isEnabled();
    }

    @Override
    public Flux<String> sendMessageStream(final AgentMessageRequest request) {
        return runtimeService.sendMessageStream(
                request.chatThreadId(),
                request.chatThreadTitle(),
                request.userMessage(),
                request.systemPrompt(),
                request.historySummary(),
                request.modelOverride(),
                request.forceNewSession(),
                request.imageAttachments()
        );
    }

    @Override
    public void cancelStreaming(final String chatThreadId) {
        runtimeService.cancelStreaming(chatThreadId);
    }

    @Override
    public boolean ownsPermissionRequest(final String requestId) {
        return runtimeService.ownsPermissionRequest(requestId);
    }

    @Override
    public PermissionResolutionModel resolvePermissionRequest(final String requestId,
                                                             final PermissionApprovalAction action) {
        return runtimeService.resolvePermissionRequest(requestId, action);
    }

    @Override
    public List<String> getAvailableModels() {
        return runtimeService.getAvailableModels();
    }

    @Override
    public String resolveEffectiveModelIdentifier() {
        return runtimeService.resolveEffectiveModelIdentifier();
    }

    @Override
    public String getModelUsedForThread(final String threadId) {
        return runtimeService.getModelUsedForThread(threadId);
    }

    @Override
    public String getAgentUsedForThread(final String threadId) {
        return runtimeService.getAgentUsedForThread(threadId);
    }

    @Override
    public long getTokensTotalForThread(final String threadId) {
        return runtimeService.getTokensTotalForThread(threadId);
    }

    @Override
    public double getCostForThread(final String threadId) {
        return runtimeService.getCostForThread(threadId);
    }

    @Override
    public AgentRuntimeStatus getRuntimeStatus() {
        final OpenCodeRuntimeStatus status = runtimeService.getRuntimeStatus();
        return new AgentRuntimeStatus(
                AgentBackend.OPENCODE,
                AgentBackend.OPENCODE.displayName(),
                "HTTP/SSE",
                status.hostname(),
                status.port(),
                status.baseUrl(),
                "opencode serve",
                status.workingDirectory(),
                status.workingDirectoryExists(),
                status.processRunning(),
                status.healthy(),
                status.version(),
                status.statusMessage()
        );
    }

    @Override
    public void prepareForFreshChat() {
        runtimeService.prepareForFreshChat();
    }

    @Override
    public String getSessionWebUrl(final String chatThreadId) {
        return runtimeService.getSessionWebUrl(chatThreadId);
    }

    @Override
    public Optional<ChatThread> getChatThread(final String chatThreadId) {
        return sessionService.getSessionByChatThreadId(chatThreadId);
    }

    @Override
    public List<ChatThread> getChatThreads() {
        try {
            runtimeService.ensureRunning();
        } catch (Exception e) {
            logger.warn("Failed to auto-start OpenCode server", e);
        }
        return sessionService.getSessions();
    }

    @Override
    public List<ChatMessageResponseModel> getMessagesByThreadId(final String chatThreadId) {
        String sessionId = sessionService.resolveSessionId(chatThreadId);
        if (sessionId == null) {
            sessionId = chatThreadId;
        }
        return sessionService.getMessages(sessionId);
    }

    @Override
    public void deleteChatThread(final String chatThreadId) {
        final String sessionId = sessionService.resolveSessionId(chatThreadId);
        sessionService.deleteSession(sessionId != null ? sessionId : chatThreadId);
    }

    @Override
    public void renameChatThread(final String chatThreadId, final String newName) {
        final String sessionId = sessionService.resolveSessionId(chatThreadId);
        sessionService.renameSession(sessionId != null ? sessionId : chatThreadId, newName);
    }
}
