package com.patres.alina.server.command;

import com.patres.alina.common.card.State;

public record Command(
        String id,
        String name,
        String description,
        String systemPrompt,
        String icon,
        State state
) {

    public Command(String name,
                   String description,
                   String systemPrompt,
                   String icon) {
        this(
                generateIdFromName(name),
                name,
                description,
                systemPrompt,
                icon != null && !icon.isBlank() ? icon : "bi-slash",
                State.ENABLED
        );
    }

    public static String generateIdFromName(final String name) {
        if (name == null || name.isBlank()) {
            return "unnamed-command";
        }
        return name.toLowerCase()
                  .replaceAll("[^a-z0-9\\-_]", "-")
                  .replaceAll("-+", "-")
                  .replaceAll("^-|-$", "");
    }
}
