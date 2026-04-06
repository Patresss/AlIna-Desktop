package com.patres.alina.common.settings;

public record AssistantSettings(
        String chatModel,
        String systemPrompt,
        int numberOfMessagesInContext,
        String openAiApiKey,
        String anthropicApiKey,
        String googleApiKey,
        int timeoutSeconds) {

    public static final String DEFAULT_CHAT_MODEL = "gpt-4o";
    public static final int DEFAULT_NUMBER_OF_CONTEXTS = 5;
    public static final int DEFAULT_TIMEOUT = 120;
    public static final String DEFAULT_OPENAI_API_KEY = "your-api-key-here";

    public AssistantSettings() {
        this(DEFAULT_CHAT_MODEL, null, DEFAULT_NUMBER_OF_CONTEXTS, DEFAULT_OPENAI_API_KEY, null, null, DEFAULT_TIMEOUT);
    }

    public AssistantSettings(final String chatModel,
                             final String systemPrompt,
                             final int numberOfMessagesInContext,
                             final String openAiApiKey,
                             final String anthropicApiKey,
                             final String googleApiKey,
                             final int timeoutSeconds) {
        this.chatModel = (chatModel != null) ? chatModel : DEFAULT_CHAT_MODEL;
        this.systemPrompt = systemPrompt;
        this.numberOfMessagesInContext = (numberOfMessagesInContext > 0) ? numberOfMessagesInContext : DEFAULT_NUMBER_OF_CONTEXTS;
        this.openAiApiKey = openAiApiKey != null ? openAiApiKey : DEFAULT_OPENAI_API_KEY;
        this.anthropicApiKey = anthropicApiKey;
        this.googleApiKey = googleApiKey;
        this.timeoutSeconds = (timeoutSeconds > 0) ? timeoutSeconds : DEFAULT_TIMEOUT;
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
