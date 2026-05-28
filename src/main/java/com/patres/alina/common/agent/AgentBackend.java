package com.patres.alina.common.agent;

import java.util.Arrays;

public enum AgentBackend {
    OPENCODE("opencode", "OpenCode"),
    CODEX("codex", "Codex");

    private final String id;
    private final String displayName;

    AgentBackend(final String id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static AgentBackend from(final String value) {
        if (value == null || value.isBlank()) {
            return OPENCODE;
        }
        final String normalized = value.trim();
        return Arrays.stream(values())
                .filter(backend -> backend.id.equalsIgnoreCase(normalized) || backend.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(OPENCODE);
    }
}
