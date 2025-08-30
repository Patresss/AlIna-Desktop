package com.patres.alina.server.openai.function.dynamic;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.Set;

public record DynamicFunction(
        String name,
        String description,
        DynamicFunctionParameters parameters
) {
}

record DynamicFunctionParameters(
        String type,
        Map<String, DynamicFunctionProperty> properties,
        Set<String> required
) {
}

record DynamicFunctionProperty(
        String type,
        String description,
        @JsonProperty("enum")
        Set<String> enumValues
) {

}