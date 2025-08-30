package com.patres.alina.server.openai.function.dynamic;//package com.patres.alina.server.openai.function.dynamic;
//
//import com.patres.alina.server.openai.function.ResponseFunction;
//import com.theokanning.openai.completion.chat.ChatFunctionDynamic;
//import com.theokanning.openai.completion.chat.ChatFunctionProperty;
//import org.springframework.web.client.RestClient;
//
//import java.util.List;
//import java.util.Set;
//
//
//public class WebhookFunction<T> {
//
//    private final RestClient restClient;
//    final DynamicFunction dynamicFunction;
//    final String url;
//
//    public WebhookFunction(final DynamicFunction dynamicFunction,
//                           final String url) {
//        this.restClient = RestClient.create();
//        this.dynamicFunction = dynamicFunction;
//        this.url = url;
//    }
//
//    public ChatFunctionDynamic createChatFunction() {
//        final ChatFunctionDynamic.Builder builder = ChatFunctionDynamic.builder()
//                .name(dynamicFunction.name())
//                .description(dynamicFunction.description());
//        final List<ChatFunctionProperty> chatFunctionProperties = dynamicFunction.parameters().properties().entrySet().stream()
//                .map(entry -> createChatFunctionProperty(entry.getKey(), entry.getValue(), dynamicFunction.parameters().required()))
//                .toList();
//        chatFunctionProperties.forEach(builder::addProperty);
//        return builder.build();
//
//    }
//
//    private ChatFunctionProperty createChatFunctionProperty(final String property,
//                                                            final DynamicFunctionProperty propertyValues,
//                                                            final Set<String> required) {
//        return ChatFunctionProperty.builder()
//                .name(property)
//                .type(propertyValues.type())
//                .description(propertyValues.description())
//                .enumValues(propertyValues.enumValues())
//                .required(required != null && required.contains(property))
//                .build();
//    }
//
//
//    public ResponseFunction handleResponse(final Object request) {
//        final String responseMessage = restClient.post()
//                .uri(url)
//                .body(request)
//                .retrieve()
//                .body(String.class);
//        return new ResponseFunction(responseMessage);
//    }
//
//
//}