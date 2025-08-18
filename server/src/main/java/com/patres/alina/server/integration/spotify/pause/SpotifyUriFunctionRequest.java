package com.patres.alina.server.integration.spotify.pause;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.patres.alina.server.openai.function.FunctionRequest;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpotifyUriFunctionRequest(
        @JsonPropertyDescription("Spotify uri of playlist")
        @JsonProperty(required = true)
        String uri
) implements FunctionRequest {

}