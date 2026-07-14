package com.patres.alina.common.interaction;

import com.patres.alina.common.agent.AgentBackend;

public record AgentInteractionRequest(
        String requestId,
        AgentBackend source,
        AgentInteractionKind kind,
        String title,
        String message,
        AgentInteractionApprovalScope approvalScope,
        String payloadJson
) {
    public AgentInteractionRequest {
        approvalScope = approvalScope == null ? AgentInteractionApprovalScope.NONE : approvalScope;
        payloadJson = payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson;
    }
}
