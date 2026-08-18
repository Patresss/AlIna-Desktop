package com.patres.alina.common.settings;

public record AssistantSettings(
        String chatModel,
        String effort) {

    public static final String DEFAULT_CHAT_MODEL = "gpt-4o";
    public static final String DEFAULT_EFFORT = "high";

    public AssistantSettings() {
        this(DEFAULT_CHAT_MODEL, DEFAULT_EFFORT);
    }

    public AssistantSettings(final String chatModel) {
        this(chatModel, DEFAULT_EFFORT);
    }

    public AssistantSettings {
        chatModel = chatModel == null || chatModel.isBlank() ? DEFAULT_CHAT_MODEL : chatModel.trim();
        effort = effort == null || effort.isBlank() ? DEFAULT_EFFORT : effort.trim();
    }

    public String resolveModelIdentifier() {
        if (chatModel != null && chatModel.contains("/")) {
            return chatModel;
        }
        return "openai/" + chatModel;
    }

    public String resolveProviderId() {
        if (chatModel != null && chatModel.contains("/")) {
            final String prefix = chatModel.substring(0, chatModel.indexOf('/')).trim();
            if (!prefix.isBlank()) {
                return prefix;
            }
        }
        return "openai";
    }

    public String resolveModelId() {
        if (chatModel != null && chatModel.contains("/")) {
            final String suffix = chatModel.substring(chatModel.indexOf('/') + 1).trim();
            if (!suffix.isBlank()) {
                return suffix;
            }
        }
        return chatModel;
    }
}
