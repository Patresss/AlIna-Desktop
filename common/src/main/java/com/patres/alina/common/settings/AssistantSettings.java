package com.patres.alina.common.settings;

public record AssistantSettings(
        String chatModel,
        String systemPrompt,
        int numberOfMessagesInContext,
        String openAiApiKey,
        int timeoutSeconds) {

    public static final String DEFAULT_CHAT_MODEL = "gpt-3.5-turbo-0613";
    public static final int DEFAULT_NUMBER_OF_CONTEXTS = 5;
    public static final int DEFAULT_TIMEOUT = 120;

    public AssistantSettings() {
        this(DEFAULT_CHAT_MODEL, null, DEFAULT_NUMBER_OF_CONTEXTS, null, DEFAULT_TIMEOUT);
    }

    public AssistantSettings(final String chatModel,
                             final String systemPrompt,
                             final int numberOfMessagesInContext,
                             final String openAiApiKey,
                             final int timeoutSeconds) {
        this.chatModel = (chatModel != null) ? chatModel : DEFAULT_CHAT_MODEL;
        this.systemPrompt = systemPrompt;
        this.numberOfMessagesInContext = (numberOfMessagesInContext > 0) ? numberOfMessagesInContext : DEFAULT_NUMBER_OF_CONTEXTS;
        this.openAiApiKey = openAiApiKey;
        this.timeoutSeconds = (timeoutSeconds > 0) ? timeoutSeconds : DEFAULT_TIMEOUT;
    }

}
