package com.patres.alina.common.interaction;

public record AgentInteractionResolutionModel(
        Status status,
        boolean accepted,
        AgentInteractionApprovalScope approvalScope,
        boolean autoContinues,
        String message
) {
    public enum Status {
        RESOLVED,
        MISSING,
        ERROR
    }

    public static AgentInteractionResolutionModel missing(final String message) {
        return new AgentInteractionResolutionModel(
                Status.MISSING,
                false,
                AgentInteractionApprovalScope.NONE,
                false,
                message
        );
    }

    public static AgentInteractionResolutionModel error(final String message) {
        return new AgentInteractionResolutionModel(
                Status.ERROR,
                false,
                AgentInteractionApprovalScope.NONE,
                false,
                message
        );
    }

    public static AgentInteractionResolutionModel resolved(final boolean accepted,
                                                           final AgentInteractionApprovalScope approvalScope,
                                                           final boolean autoContinues,
                                                           final String message) {
        return new AgentInteractionResolutionModel(Status.RESOLVED, accepted, approvalScope, autoContinues, message);
    }
}
