package com.patres.alina.server.integration.todoist;

import com.patres.alina.common.card.State;
import com.patres.alina.server.integration.Integration;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationSettings;

import java.util.Map;

public class TodoistIntegrationSettings extends AlinaIntegrationSettings {

    final static String API_TOKEN = "apiToken";
    final static String PROJECT_ID = "projectId";
    final static String SECTION_ID = "sectionId";

    private final String apiToken;
    private final String projectId;
    private final String sectionId;

    public TodoistIntegrationSettings(Integration integration) {
        this(integration.id(), integration.state(), integration.integrationSettings());
    }

    public TodoistIntegrationSettings(String id, State state, Map<String, Object> settings) {
        super(id, state);
        this.apiToken = getSettingsMandatoryStringValue(settings, API_TOKEN);
        this.projectId = getSettingsStringValue(settings, PROJECT_ID);
        this.sectionId = getSettingsStringValue(settings, SECTION_ID);
    }

    public String getApiToken() {
        return apiToken;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getSectionId() {
        return sectionId;
    }
}