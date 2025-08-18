package com.patres.alina.server.integration.todoist.addtask;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TodoistAddTaskResponse(String content,
                                     String description,
                                     List<String> labels,
                                     Integer priority,
                                     @JsonProperty("project_id") String projectId,
                                     @JsonProperty("section_id") String sectionId,
                                     String url) {
}
