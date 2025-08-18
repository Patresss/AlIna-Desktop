package com.patres.alina.server.integration.spotify.pause;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.patres.alina.server.openai.function.FunctionRequest;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpotifySearchPlaylistFunctionRequest(
        @JsonPropertyDescription("Search description of playlist that should be played by Spotify")
        @JsonProperty(required = true)
        String searchQuery
) implements FunctionRequest {

}