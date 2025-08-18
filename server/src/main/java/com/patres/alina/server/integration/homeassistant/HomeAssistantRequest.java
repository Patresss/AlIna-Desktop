package com.patres.alina.server.integration.homeassistant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HomeAssistantRequest(String text,
                                   @JsonProperty("agent_id")
                                   String agentId) {
}
