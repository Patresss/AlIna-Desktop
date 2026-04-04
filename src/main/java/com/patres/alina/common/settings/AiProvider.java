
package com.patres.alina.common.settings;

public enum AiProvider {
    OPENAI("OpenAI", "gpt-4o"),
    ANTHROPIC("Anthropic", "claude-sonnet-4-5-20250514"),
    GOOGLE_GEMINI("Google Gemini", "gemini-2.0-flash");

    private final String displayName;
    private final String defaultModel;

    AiProvider(String displayName, String defaultModel) {
        this.displayName = displayName;
        this.defaultModel = defaultModel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public static AiProvider fromString(String value) {
        if (value == null) return OPENAI;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return OPENAI;
        }
    }

    public static AiProvider detectFromModelName(String model) {
        if (model == null) return null;
        String lower = model.toLowerCase();
        if (lower.startsWith("gpt-") || lower.startsWith("o1") || lower.startsWith("o3") || lower.startsWith("o4") || lower.startsWith("chatgpt")) {
            return OPENAI;
        }
        if (lower.startsWith("claude-")) {
            return ANTHROPIC;
        }
        if (lower.startsWith("gemini-")) {
            return GOOGLE_GEMINI;
        }
        return null;
    }
}
