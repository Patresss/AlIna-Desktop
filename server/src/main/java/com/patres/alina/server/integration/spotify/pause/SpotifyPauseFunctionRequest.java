package com.patres.alina.server.integration.spotify.pause;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.patres.alina.server.openai.function.FunctionRequest;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpotifyPauseFunctionRequest(
) implements FunctionRequest {
}