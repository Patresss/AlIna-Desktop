package com.patres.alina.server.integration.homeassistant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.patres.alina.server.openai.function.FunctionRequest;

public record HomeAssistantFunctionRequest(
        @JsonPropertyDescription("Message that should be send to Home Assistant")
        @JsonProperty(required = true)
        String homeAssistantMessage) implements FunctionRequest {
}
