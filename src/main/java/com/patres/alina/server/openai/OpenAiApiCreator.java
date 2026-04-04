package com.patres.alina.server.openai;

import com.patres.alina.common.settings.AiProvider;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.stereotype.Component;

import static com.patres.alina.common.settings.AssistantSettings.DEFAULT_OPENAI_API_KEY;

@Component
public class OpenAiApiCreator {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/openai";

    ChatModel createChatModel(final AiProvider provider, final String apiKey, final String model) {
        if (apiKey == null || apiKey.trim().isEmpty() || DEFAULT_OPENAI_API_KEY.equals(apiKey)) {
            return null;
        }
        return switch (provider) {
            case OPENAI -> createOpenAiChatModel(apiKey, model);
            case ANTHROPIC -> createAnthropicChatModel(apiKey, model);
            case GOOGLE_GEMINI -> createGeminiChatModel(apiKey, model);
        };
    }

    private OpenAiChatModel createOpenAiChatModel(final String apiKey, final String model) {
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
        var defaults = OpenAiChatOptions.builder()
                .model(model)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(defaults)
                .build();
    }

    private AnthropicChatModel createAnthropicChatModel(final String apiKey, final String model) {
        AnthropicApi api = AnthropicApi.builder()
                .apiKey(apiKey)
                .build();
        var options = AnthropicChatOptions.builder()
                .model(model)
                .build();
        return AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(options)
                .build();
    }

    private OpenAiChatModel createGeminiChatModel(final String apiKey, final String model) {
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(apiKey)
                .baseUrl(GEMINI_BASE_URL)
                .build();
        var defaults = OpenAiChatOptions.builder()
                .model(model)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(defaults)
                .build();
    }

    OpenAiAudioTranscriptionModel createOpenAiAudioTranscriptionModel(final String apiKey, final String model) {
        if (apiKey == null || apiKey.trim().isEmpty() || DEFAULT_OPENAI_API_KEY.equals(apiKey)) {
            return null;
        }
        OpenAiAudioApi audioApi = OpenAiAudioApi.builder()
                .apiKey(apiKey)
                .build();
        OpenAiAudioTranscriptionOptions options = OpenAiAudioTranscriptionOptions.builder()
                .model(model)
                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.TEXT)
                .build();
        return new OpenAiAudioTranscriptionModel(audioApi, options);
    }
}
