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
        boolean forceNewSession,
        List<ImageAttachment> imageAttachments
) {
    public AgentMessageRequest {
        imageAttachments = imageAttachments == null ? List.of() : List.copyOf(imageAttachments);
    }
}
