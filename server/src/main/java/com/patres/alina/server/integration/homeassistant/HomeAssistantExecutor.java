package com.patres.alina.server.integration.homeassistant;

import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.Optional;
import java.util.Set;


public class HomeAssistantExecutor extends AlinaIntegrationExecutor<HomeAssistantIntegrationSettings> {

    private static final Logger logger = LoggerFactory.getLogger(HomeAssistantExecutor.class);
    private static final Set<String> HOME_ASSISTANT_SUCCESS_RESPONSE_TYPES = Set.of("action_done", "query_answer");

    private final RestClient restClient;

    public HomeAssistantExecutor(HomeAssistantIntegrationSettings settings) {
        super(settings);
        this.restClient = RestClient.create();
    }

    public String handleResponse(final HomeAssistantFunctionRequest homeAssistantFunctionRequest) {
        final HomeAssistantResponse response = sendAndGetResponse(homeAssistantFunctionRequest);
        return getSpeech(response)
                .map(this::responseToJson)
                .orElse(homeAssistantFunctionRequest.homeAssistantMessage());
    }

    private static Optional<String> getSpeech(HomeAssistantResponse response) {
        return Optional.ofNullable(response)
                .map(HomeAssistantResponse::response)
                .map(HomeAssistantResponse.Response::speech)
                .map(HomeAssistantResponse.Speech::plain)
                .map(HomeAssistantResponse.Plain::speech);
    }

    private HomeAssistantResponse sendAndGetResponse(final HomeAssistantFunctionRequest homeAssistantFunctionRequest) {
        final HomeAssistantResponse response = sendRequestToHomeAssistant(settings, homeAssistantFunctionRequest.homeAssistantMessage(), null);
        boolean successDefaultAgentResponse = Optional.ofNullable(response)
                .map(HomeAssistantResponse::response)
                .map(HomeAssistantResponse.Response::responseType)
                .map(HOME_ASSISTANT_SUCCESS_RESPONSE_TYPES::contains)
                .orElse(false);
        if (successDefaultAgentResponse) {
            return response;
        }

        if (settings.getAlternativeAgentId() != null) {
            logger.warn("Invalid response from default agent {}, will try using alternative agent", getSpeech(response).orElse("null"));
            return sendRequestToHomeAssistant(settings, homeAssistantFunctionRequest.homeAssistantMessage(), settings.getAlternativeAgentId());
        } else {
            logger.error("Invalid response from default agent {}", getSpeech(response).orElse("null"));
            return response;
        }
    }

    private HomeAssistantResponse sendRequestToHomeAssistant(final HomeAssistantIntegrationSettings settings, String message, String agentId) {
        return restClient.post()
                .uri(settings.getHomeAssistantBaseUrl() + "/api/conversation/process")
                .body(new HomeAssistantRequest(message, agentId))
                .header("Authorization", "Bearer " + settings.getHomeAssistantToken())
                .retrieve()
                .body(HomeAssistantResponse.class);
    }

    private String responseToJson(final String speech) {
        return """
                {
                "homeAssistantResponse" : "$"
                }
                """.replace("$", speech);
    }


}
