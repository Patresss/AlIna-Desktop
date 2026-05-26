package com.patres.alina.server.ai;

import com.patres.alina.common.ai.AiProvider;
import com.patres.alina.common.ai.AiRuntimeStatus;
import com.patres.alina.common.message.ImageAttachment;
import com.patres.alina.common.permission.PermissionApprovalAction;
import com.patres.alina.common.permission.PermissionResolutionModel;
import reactor.core.publisher.Flux;

import java.util.List;

public interface AiRuntime {

    AiProvider provider();

    default boolean isEnabled() {
        return true;
    }

    Flux<String> sendMessageStream(String chatThreadId,
                                   String chatThreadTitle,
                                   String userMessage,
                                   String systemPrompt,
                                   String historySummary,
                                   String modelOverride,
                                   boolean forceNewSession,
                                   List<ImageAttachment> imageAttachments);

    void cancelStreaming(String chatThreadId);

    List<String> getAvailableModels();

    AiRuntimeStatus getRuntimeStatus();

    default void prepareForFreshChat() {
    }

    default void prepareForHistoryAccess() {
    }

    default String getSessionWebUrl(final String chatThreadId) {
        return null;
    }

    default boolean ownsPermissionRequest(final String requestId) {
        return false;
    }

    default PermissionResolutionModel resolvePermissionRequest(final String requestId,
                                                              final PermissionApprovalAction action) {
        return PermissionResolutionModel.missing("Permission request is not available.");
    }

    default String getModelUsedForThread(final String threadId) {
        return null;
    }

    default String getAgentUsedForThread(final String threadId) {
        return null;
    }

    default long getTokensTotalForThread(final String threadId) {
        return 0;
    }

    default double getCostForThread(final String threadId) {
        return 0.0;
    }
}
