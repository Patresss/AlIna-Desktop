package com.patres.alina.common.ai;

public enum AiProvider {
    OPENCODE("OpenCode"),
    CODEX("Codex");

    private final String displayName;

    AiProvider(final String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public static AiProvider from(final String value) {
        if (value == null || value.isBlank()) {
            return OPENCODE;
        }
        for (final AiProvider provider : values()) {
            if (provider.name().equalsIgnoreCase(value.trim())
                    || provider.displayName.equalsIgnoreCase(value.trim())) {
                return provider;
            }
        }
        return OPENCODE;
    }
}
