package com.patres.alina.common.plugin;


public record PluginCreateRequest(
        String name,
        String description,
        String systemPrompt,
        String icon
) {
}
