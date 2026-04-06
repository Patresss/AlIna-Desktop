package com.patres.alina.server.openai;

import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.stereotype.Component;

import static com.patres.alina.common.settings.AssistantSettings.DEFAULT_OPENAI_API_KEY;

@Component
public class OpenAiApiCreator {

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
