package com.patres.alina.common.command;


import com.patres.alina.common.card.State;

public record CommandDetail(
        String id,
        String name,
        String description,
        String systemPrompt,
        String icon,
        State state
) {
}
