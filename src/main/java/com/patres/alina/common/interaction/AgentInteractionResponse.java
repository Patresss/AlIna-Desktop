package com.patres.alina.common.interaction;

public record AgentInteractionResponse(
        AgentInteractionAction action,
        String valuesJson
) {
    public AgentInteractionResponse {
        valuesJson = valuesJson == null || valuesJson.isBlank() ? "{}" : valuesJson;
    }
}
