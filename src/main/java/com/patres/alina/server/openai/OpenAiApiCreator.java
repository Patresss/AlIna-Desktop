package com.patres.alina.server.openai;


import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import static com.patres.alina.common.settings.AssistantSettings.DEFAULT_OPENAI_API_KEY;

@Component
public class OpenAiApiCreator {


    OpenAiChatModel createOpenAiChatModel(final String apiKey, final String model) {
        if (apiKey == null || apiKey.trim().isEmpty() || DEFAULT_OPENAI_API_KEY.equals(apiKey)) {
            return null;
        }
        
        OpenAiApi api = createOpenAiApi(apiKey);
        var defaults = OpenAiChatOptions.builder()
                .model(model)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(defaults)
                .build();
    }

    private OpenAiApi createOpenAiApi(final String apiKey) {
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
    }

}