package com.patres.alina.uidesktop.mascot;

import com.patres.alina.common.interaction.AgentInteractionApprovalScope;
import com.patres.alina.common.interaction.AgentInteractionRequest;

import java.time.Instant;

record MascotNotification(
        String id,
        MascotNotificationType type,
        String threadId,
        String requestId,
        String title,
        String message,
        AgentInteractionApprovalScope approvalScope,
        Instant createdAt
) {

    public MascotNotification {
        id = normalize(id);
        type = type == null ? MascotNotificationType.COMPLETE : type;
        threadId = normalize(threadId);
        requestId = normalize(requestId);
        title = normalize(title);
        message = normalize(message);
        approvalScope = approvalScope == null ? AgentInteractionApprovalScope.NONE : approvalScope;
        createdAt = createdAt == null ? Instant.EPOCH : createdAt;
    }

    public static MascotNotification approval(final String threadId,
                                               final AgentInteractionRequest interaction,
                                               final Instant createdAt) {
        final String requestId = interaction == null ? "" : normalize(interaction.requestId());
        return new MascotNotification(
                "approval:" + requestId,
                MascotNotificationType.APPROVAL,
                threadId,
                requestId,
                interaction == null ? "" : interaction.title(),
                interaction == null ? "" : interaction.message(),
                interaction == null ? AgentInteractionApprovalScope.NONE : interaction.approvalScope(),
                createdAt
        );
    }

    public static MascotNotification terminal(final MascotNotificationType type,
                                               final String threadId,
                                               final String title,
                                               final String message,
                                               final Instant createdAt) {
        return new MascotNotification(
                "terminal:" + type + ":" + normalize(threadId) + ":" + createdAt.toEpochMilli(),
                type,
                threadId,
                "",
                title,
                message,
                AgentInteractionApprovalScope.NONE,
                createdAt
        );
    }

    public boolean isApproval() {
        return type == MascotNotificationType.APPROVAL;
    }

    public boolean supportsScopedApproval() {
        return approvalScope != AgentInteractionApprovalScope.NONE;
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }
}
