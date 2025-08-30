package com.patres.alina.common.plugin;


import com.patres.alina.common.card.State;

public record PluginDetail(
        String id,
        String name,
        String description,
        String systemPrompt,
        String icon,
        State state
) {
}
