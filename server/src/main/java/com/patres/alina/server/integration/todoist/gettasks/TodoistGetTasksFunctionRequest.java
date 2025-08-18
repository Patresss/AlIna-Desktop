package com.patres.alina.server.integration.todoist.gettasks;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.patres.alina.server.openai.function.FunctionRequest;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TodoistGetTasksFunctionRequest(
) implements FunctionRequest {
}