package com.patres.alina.common.command;


public record CommandCreateRequest(
        String name,
        String description,
        String systemPrompt,
        String icon
) {
}
