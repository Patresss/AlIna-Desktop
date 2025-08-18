package com.patres.alina.server.integration.todoist.addtask;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.patres.alina.server.openai.function.FunctionRequest;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TodoistAddTaskFunctionRequest(
        @JsonPropertyDescription("Content of the task. Present it in imperative form. For example, change the task \"kupić jabłka\" to \"kup jabłka\".")
        @JsonProperty(required = true)
        String taskContent,

        @JsonPropertyDescription("Description of the task if needed (if content is too long)")
        @JsonProperty
        String description,

        @JsonPropertyDescription("Labels if is provided. It should start with a capital letter. For example: 'standup' -> 'Standup'")
        @JsonProperty
        List<String> labels,

        @JsonPropertyDescription("Task priority from 1 (normal) to 4 (urgent).")
        @JsonProperty
        Integer priority
) implements FunctionRequest {
}