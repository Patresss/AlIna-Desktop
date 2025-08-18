package com.patres.alina.server.integration.mail.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.patres.alina.server.openai.function.FunctionRequest;

public record SendMailFunctionRequest(
        @JsonPropertyDescription("Subject of the email - generate if it was not provided")
        @JsonProperty(required = true)
        String subject,
        @JsonPropertyDescription("Content of the email")
        @JsonProperty(required = true)
        String content) implements FunctionRequest {
}
