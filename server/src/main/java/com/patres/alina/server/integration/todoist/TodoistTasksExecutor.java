package com.patres.alina.server.integration.todoist;

import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationExecutor;
import com.patres.alina.server.integration.todoist.addtask.TodoistAddTaskFunctionRequest;
import com.patres.alina.server.integration.todoist.addtask.TodoistAddTaskRequest;
import com.patres.alina.server.integration.todoist.addtask.TodoistAddTaskResponse;
import com.patres.alina.server.integration.todoist.closetask.TodoistCloseTaskFunctionRequest;
import com.patres.alina.server.integration.todoist.gettasks.TodoistGetTaskResponse;
import com.patres.alina.server.integration.todoist.gettasks.TodoistGetTasksFunctionRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Optional;

public class TodoistTasksExecutor extends AlinaIntegrationExecutor<TodoistIntegrationSettings> {

    private static final Logger logger = LoggerFactory.getLogger(TodoistTasksExecutor.class);


    private static final String URL = "https://api.todoist.com/rest/v2/";

    protected final RestClient restClient;

    public TodoistTasksExecutor(final TodoistIntegrationSettings settings) {
        super(settings);
        this.restClient = RestClient.builder()
                .baseUrl(URL)
                .build();
    }

    public TodoistAddTaskResponse addTask(TodoistAddTaskFunctionRequest todoistAddTaskFunctionRequest) {
        final TodoistAddTaskRequest todoistAddTaskRequest = new TodoistAddTaskRequest(
                todoistAddTaskFunctionRequest.taskContent(),
                todoistAddTaskFunctionRequest.description(),
                todoistAddTaskFunctionRequest.labels(),
                todoistAddTaskFunctionRequest.priority(),
                settings.getProjectId(),
                settings.getSectionId()
        );
        try {
            return restClient.post()
                    .uri("tasks")
                    .body(todoistAddTaskRequest)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.getApiToken())
                    .retrieve()
                    .body(TodoistAddTaskResponse.class);
        } catch (final RestClientException e) {
            logger.error("Cannot create task", e);
            throw e;
        }
    }


    public String closeTask(TodoistCloseTaskFunctionRequest functionRequest) {

        try {
            return restClient.post()
                    .uri("tasks/" + functionRequest.taskId() + "/close")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.getApiToken())
                    .retrieve()
                    .body(String.class);
        } catch (final RestClientException e) {
            logger.error("Cannot close task by id {}", functionRequest.taskId(), e);
            throw e;
        }
    }

    public List<TodoistGetTaskResponse> getTasks(TodoistGetTasksFunctionRequest functionRequest) {
        try {
            return restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("tasks")
                            .queryParamIfPresent("section_id", Optional.ofNullable(settings.getSectionId()))
                            .queryParamIfPresent("project_id", Optional.ofNullable(settings.getProjectId()))
                            .build())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + settings.getApiToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (final RestClientException e) {
            logger.error("Cannot show list of tasks", e);
            throw e;
        }
    }

}