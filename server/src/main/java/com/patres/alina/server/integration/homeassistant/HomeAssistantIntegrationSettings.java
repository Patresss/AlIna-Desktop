package com.patres.alina.server.integration.homeassistant;

import com.patres.alina.common.card.State;
import com.patres.alina.server.integration.Integration;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationSettings;

import java.util.Map;

public class HomeAssistantIntegrationSettings extends AlinaIntegrationSettings {


    final static String ALTERNATIVE_AGENT_ID_KEY = "alternativeAgentId";
    final static String HOME_ASSISTANT_BASE_URL = "homeAssistantBaseUrl";
    final static String HOME_ASSISTANT_TOKEN = "homeAssistantToken";

    private final String alternativeAgentId;
    private final String homeAssistantBaseUrl;
    private final String homeAssistantToken;

    public HomeAssistantIntegrationSettings(Integration integration) {
        this(integration.id(), integration.state(), integration.integrationSettings());
    }

    public HomeAssistantIntegrationSettings(String id, State state, Map<String, Object> settings) {
        super(id, state);
        this.alternativeAgentId = getSettingsStringValue(settings, ALTERNATIVE_AGENT_ID_KEY);
        this.homeAssistantBaseUrl = getSettingsMandatoryStringValue(settings, HOME_ASSISTANT_BASE_URL);
        this.homeAssistantToken = getSettingsMandatoryStringValue(settings, HOME_ASSISTANT_TOKEN);
    }

    public String getAlternativeAgentId() {
        return alternativeAgentId;
    }

    public String getHomeAssistantBaseUrl() {
        return homeAssistantBaseUrl;
    }

    public String getHomeAssistantToken() {
        return homeAssistantToken;
    }
}
