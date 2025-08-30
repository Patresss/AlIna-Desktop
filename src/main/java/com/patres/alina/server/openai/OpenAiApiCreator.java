package com.patres.alina.server.openai;


import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

@Component
public class OpenAiApiCreator {


    OpenAiChatModel createOpenAiChatModel(final String apiKey, final String model) {
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