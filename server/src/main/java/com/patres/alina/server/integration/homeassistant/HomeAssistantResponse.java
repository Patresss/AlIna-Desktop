package com.patres.alina.server.integration.homeassistant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HomeAssistantResponse(Response response,
                                    @JsonProperty("conversation_id")
                                                String conversationId) {

    public record ResponseWrapper(Response response,
                                  @JsonProperty("conversation_id")
                                  String conversationId) {
    }

    record Response(Speech speech,
                    @JsonProperty("response_type")
                    String responseType) {

    }

    record Speech(Plain plain) {
    }

    record Plain(
            String speech,
            @JsonProperty("extra_data")
            Object extraData) {
    }

}
