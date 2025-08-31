package com.patres.alina.server.plugin;

import com.patres.alina.common.card.State;
import com.patres.alina.server.storage.Entity;

public record Command(
        String id,              // filename without extension
        String name,            // from frontmatter
        String description,     // from frontmatter  
        String systemPrompt,    // markdown content after frontmatter
        String icon,            // from frontmatter, default "bi-slash"
        State state             // from frontmatter, default ENABLED
) implements Entity<String> {

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

    @Override
    public String getId() {
        return id;
    }

    private static String generateIdFromName(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed-command";
        }
        return name.toLowerCase()
                  .replaceAll("[^a-z0-9\\-_]", "-")
                  .replaceAll("-+", "-")
                  .replaceAll("^-|-$", "");
    }
}