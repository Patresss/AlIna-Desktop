package com.patres.alina.common.settings;

public record AssistantSettings(
        String chatModel) {

    public static final String DEFAULT_CHAT_MODEL = "gpt-4o";

    public AssistantSettings() {
        this(DEFAULT_CHAT_MODEL);
    }

    public AssistantSettings(final String chatModel) {
        this.chatModel = (chatModel != null) ? chatModel : DEFAULT_CHAT_MODEL;
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
