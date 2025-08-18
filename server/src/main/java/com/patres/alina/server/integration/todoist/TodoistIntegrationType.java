package com.patres.alina.server.integration.todoist;

import com.patres.alina.common.field.FormField;
import com.patres.alina.common.field.FormFieldType;
import com.patres.alina.common.field.UiForm;
import com.patres.alina.server.integration.Integration;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationFunction;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationType;
import com.patres.alina.server.integration.todoist.addtask.TodoistAddTaskFunctionRequest;
import com.patres.alina.server.integration.todoist.closetask.TodoistCloseTaskFunctionRequest;
import com.patres.alina.server.integration.todoist.gettasks.TodoistGetTasksFunctionRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import static com.patres.alina.server.integration.todoist.TodoistIntegrationSettings.*;

@Component
public class TodoistIntegrationType extends AlinaIntegrationType<TodoistIntegrationSettings, TodoistTasksExecutor> {

    private static final String INTEGRATION_TYPE_NAME = "todoistTask";
    private static final String INTEGRATION_DEFAULT_NAME_TO_DISPLAY = "Todoist";
    private static final String INTEGRATION_DESCRIPTION = """
            Manage your TODO list (TODOIST). Add, get, close your tasks""";
    private static final String ICON = "mdal-add_task";

    private static final UiForm UI_FORM = new UiForm(List.of(
            new FormField(API_TOKEN, INTEGRATION_TYPE_NAME + ".apiToken.name", "API Token", null, null, true, FormFieldType.TEXT_FIELD),
            new FormField(PROJECT_ID, INTEGRATION_TYPE_NAME + ".projectId.name", "Project id", null, null, false, FormFieldType.TEXT_FIELD),
            new FormField(SECTION_ID, INTEGRATION_TYPE_NAME + ".sectionId.name", "Section id", null, null, false, FormFieldType.TEXT_FIELD)
    ));

    public TodoistIntegrationType() {
        super(INTEGRATION_TYPE_NAME, INTEGRATION_DEFAULT_NAME_TO_DISPLAY, INTEGRATION_DESCRIPTION, ICON, UI_FORM,
                List.of(
                        new AlinaIntegrationFunction<>("addTask", "Add task to list", TodoistTasksExecutor::addTask, TodoistAddTaskFunctionRequest.class),
                        new AlinaIntegrationFunction<>("getTasks", "Get list of tasks.", TodoistTasksExecutor::getTasks, TodoistGetTasksFunctionRequest.class),
                        new AlinaIntegrationFunction<>("deleteTaskByID", "Close/delete task by id", TodoistTasksExecutor::closeTask, TodoistCloseTaskFunctionRequest.class)
                ));
    }

    @Override
    public TodoistTasksExecutor createExecutor(TodoistIntegrationSettings settings) {
        return new TodoistTasksExecutor(settings);
    }

    @Override
    public TodoistIntegrationSettings createSettings(Integration integration) {
        final Map<String, Object> settings = integration.integrationSettings();
        return new TodoistIntegrationSettings(integration.id(), integration.state(), settings);
    }
}