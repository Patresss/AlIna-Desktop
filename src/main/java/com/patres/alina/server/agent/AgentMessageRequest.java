package com.patres.alina.server.agent;

import com.patres.alina.common.message.ImageAttachment;

import java.util.List;

public record AgentMessageRequest(
        String chatThreadId,
        String chatThreadTitle,
        String userMessage,
        String systemPrompt,
        String historySummary,
        String modelOverride,
        String effortOverride,
        boolean forceNewSession,
        List<ImageAttachment> imageAttachments
) {
    public AgentMessageRequest(
            final String chatThreadId,
            final String chatThreadTitle,
            final String userMessage,
            final String systemPrompt,
            final String historySummary,
            final String modelOverride,
            final boolean forceNewSession,
            final List<ImageAttachment> imageAttachments
    ) {
        this(chatThreadId, chatThreadTitle, userMessage, systemPrompt, historySummary, modelOverride, null,
                forceNewSession, imageAttachments);
    }

    public AgentMessageRequest {
        imageAttachments = imageAttachments == null ? List.of() : List.copyOf(imageAttachments);
    }
}
