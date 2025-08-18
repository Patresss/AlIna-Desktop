package com.patres.alina.server.openai.function.rest;

import com.patres.alina.server.openai.function.OpenAiFunction;
import com.patres.alina.server.openai.function.ResponseFunction;
import com.theokanning.openai.completion.chat.ChatFunction;
import org.springframework.web.client.RestClient;


public abstract class RestFunction<T> extends OpenAiFunction {

    private final RestClient restClient;
    private final RestFunctionProperties<T> restFunctionProperties;

    public RestFunction(final RestFunctionProperties<T> restFunctionProperties) {
        this.restClient = RestClient.create();
        this.restFunctionProperties = restFunctionProperties;
    }

    @Override
    public ChatFunction createChatFunction() {
        return ChatFunction.builder()
                .name(restFunctionProperties.functionName())
                .description(restFunctionProperties.functionDescription())
                .executor(restFunctionProperties.requestClass(), this::handleResponse)
                .build();

    }

    private Object handleResponse(final T request) {
        final String responseMessage = restClient.method(restFunctionProperties.httpMethod())
                .uri(restFunctionProperties.url())
                .body(request)
                .retrieve()
                .body(String.class);
        return new ResponseFunction(responseMessage);
    }


}