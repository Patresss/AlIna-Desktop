package com.patres.alina.common.settings;

public record AssistantSettings(
        String chatModel,
        String systemPrompt,
        int numberOfMessagesInContext,
        String openAiApiKey,
        String anthropicApiKey,
        String googleApiKey,
        AiProvider aiProvider,
        int timeoutSeconds) {

    public static final String DEFAULT_CHAT_MODEL = "gpt-4o";
    public static final int DEFAULT_NUMBER_OF_CONTEXTS = 5;
    public static final int DEFAULT_TIMEOUT = 120;
    public static final String DEFAULT_OPENAI_API_KEY = "your-api-key-here";

    public AssistantSettings() {
        this(DEFAULT_CHAT_MODEL, null, DEFAULT_NUMBER_OF_CONTEXTS, DEFAULT_OPENAI_API_KEY, null, null, AiProvider.OPENAI, DEFAULT_TIMEOUT);
    }

    public AssistantSettings(final String chatModel,
                             final String systemPrompt,
                             final int numberOfMessagesInContext,
                             final String openAiApiKey,
                             final String anthropicApiKey,
                             final String googleApiKey,
                             final AiProvider aiProvider,
                             final int timeoutSeconds) {
        this.chatModel = (chatModel != null) ? chatModel : DEFAULT_CHAT_MODEL;
        this.systemPrompt = systemPrompt;
        this.numberOfMessagesInContext = (numberOfMessagesInContext > 0) ? numberOfMessagesInContext : DEFAULT_NUMBER_OF_CONTEXTS;
        this.openAiApiKey = openAiApiKey != null ? openAiApiKey : DEFAULT_OPENAI_API_KEY;
        this.anthropicApiKey = anthropicApiKey;
        this.googleApiKey = googleApiKey;
        this.aiProvider = aiProvider != null ? aiProvider : AiProvider.OPENAI;
        this.timeoutSeconds = (timeoutSeconds > 0) ? timeoutSeconds : DEFAULT_TIMEOUT;
    }

    public AiProvider resolveProvider() {
        AiProvider detected = AiProvider.detectFromModelName(chatModel);
        return detected != null ? detected : aiProvider;
    }

    public String getApiKeyForProvider(AiProvider provider) {
        return switch (provider) {
            case OPENAI -> openAiApiKey;
            case ANTHROPIC -> anthropicApiKey;
            case GOOGLE_GEMINI -> googleApiKey;
        };
    }
}
