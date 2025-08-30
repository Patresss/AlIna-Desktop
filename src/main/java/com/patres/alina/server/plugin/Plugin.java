package com.patres.alina.server.plugin;

import com.patres.alina.common.card.State;
import org.springframework.data.annotation.Id;

import java.util.UUID;

public record Plugin(
        @Id
        String id,
        String name,
        String description,
        String systemPrompt,
        String icon,
        State state
) {

    public Plugin(String name,
                  String description,
                  String systemPrompt,
                  String icon) {
        this(
                UUID.randomUUID().toString(),
                name,
                description,
                systemPrompt,
                icon,
                State.ENABLED
        );
    }
}
