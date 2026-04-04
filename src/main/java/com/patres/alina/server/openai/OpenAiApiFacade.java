package com.patres.alina.server.openai;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.SpeechToTextErrorType;
import com.patres.alina.common.settings.AiProvider;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.event.AssistantSettingsUpdatedEvent;
import com.patres.alina.server.mcp.McpToolIntegrationService;
import com.patres.alina.server.message.exception.CannotConvertSpeechToTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class OpenAiApiFacade {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiApiFacade.class);
    public static final String SPEECH_TO_TEXT_MODEL = "whisper-1";

    private final OpenAiApiCreator openAiApiCreator;
    private ChatModel chatModel;
    private OpenAiAudioTranscriptionModel audioTranscriptionModel;
    private final McpToolIntegrationService mcpToolIntegrationService;

    public OpenAiApiFacade(FileManager<AssistantSettings> assistantSettingsManager,
                           OpenAiApiCreator openAiApiCreator,
                           McpToolIntegrationService mcpToolIntegrationService) {
        this.openAiApiCreator = openAiApiCreator;
        this.mcpToolIntegrationService = mcpToolIntegrationService;
        updateAiService(assistantSettingsManager.getSettings());
        DefaultEventBus.getInstance().subscribe(AssistantSettingsUpdatedEvent.class, e -> {
            updateAiService(e.getSettings());
        });
    }

    public void updateAiService(final AssistantSettings settings) {
        AiProvider provider = settings.resolveProvider();
        String apiKey = settings.getApiKeyForProvider(provider);

        try {
            chatModel = openAiApiCreator.createChatModel(provider, apiKey, settings.chatModel());
            if (chatModel == null) {
                logger.warn("Chat model is null - API key for {} not properly configured.", provider.getDisplayName());
            } else {
                logger.info("Chat model updated successfully with provider: {} model: {}", provider.getDisplayName(), settings.chatModel());
            }
        } catch (Exception e) {
            logger.error("Failed to update chat model for provider: {}", provider.getDisplayName(), e);
            chatModel = null;
        }

        // Speech-to-text always uses OpenAI (Whisper)
        try {
            audioTranscriptionModel = openAiApiCreator.createOpenAiAudioTranscriptionModel(settings.openAiApiKey(), SPEECH_TO_TEXT_MODEL);
            if (audioTranscriptionModel == null) {
                logger.warn("OpenAI audio transcription model is null - OpenAI API key not properly configured.");
            } else {
                logger.info("OpenAI audio transcription model updated successfully");
            }
        } catch (Exception e) {
            logger.error("Failed to update OpenAI audio transcription model", e);
            audioTranscriptionModel = null;
        }
    }

    public String speechToText(final File audio) {
        if (audioTranscriptionModel == null) {
            throw new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.MISSING_API_KEY,
                    "OpenAI API key not configured. Please set a valid API key in assistant settings."
            );
        }
        return audioTranscriptionModel.call(new FileSystemResource(audio));
    }

    public Flux<String> sendMessageStream(final List<AbstractMessage> messages) {
        if (chatModel == null) {
            logger.warn("Cannot send message - chat model is not configured. Please set a valid API key in assistant settings.");
            return Flux.just("Error: AI API key not configured. Please set a valid API key in assistant settings.");
        }

        List<Message> messageList = messages.stream().map(m -> (Message) m).toList();
        ChatClient client = ChatClient
                .builder(chatModel)
                .defaultToolCallbacks(mcpToolIntegrationService.getToolCallbacks())
                .build();
        return client
                .prompt(new Prompt(messageList))
                .stream()
                .content();
    }

    public List<String> getChatModels(AssistantSettings settings) {
        List<String> models = new ArrayList<>();
        if (isValidApiKey(settings.openAiApiKey())) {
            Arrays.stream(OpenAiApi.ChatModel.values())
                    .map(OpenAiApi.ChatModel::getValue)
                    .sorted()
                    .forEach(models::add);
        }
        if (isValidApiKey(settings.anthropicApiKey())) {
            Arrays.stream(AnthropicApi.ChatModel.values())
                    .map(AnthropicApi.ChatModel::getValue)
                    .sorted()
                    .forEach(models::add);
        }
        if (isValidApiKey(settings.googleApiKey())) {
            models.addAll(List.of("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.0-flash", "gemini-2.0-flash-lite", "gemini-1.5-pro", "gemini-1.5-flash"));
        }
        return models;
    }

    private static boolean isValidApiKey(String key) {
        return key != null && !key.isBlank() && !AssistantSettings.DEFAULT_OPENAI_API_KEY.equals(key);
    }
}
