package com.patres.alina.server.openai.function;

import com.patres.alina.server.integration.Integration;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationExecutor;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationFunction;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationSettings;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationType;
import com.theokanning.openai.completion.chat.ChatFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;


public final class ChatFunctionCreator {

    private static final Logger logger = LoggerFactory.getLogger(ChatFunctionCreator.class);


    public static <S extends AlinaIntegrationSettings, E extends AlinaIntegrationExecutor<S>> List<ChatFunction> createChatFunctions(
            AlinaIntegrationType<S, E> alinaIntegrationType,
            Integration integration) {
        try {
            S settings = alinaIntegrationType.createSettings(integration);
            E executor = alinaIntegrationType.createExecutor(settings);
            return alinaIntegrationType.getIntegrationFunctionList().stream()
                    .map(function -> getChatFunction(executor, function, alinaIntegrationType, integration))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        } catch (Exception e) {
            logger.error("Cannot create chat settings and executor: {}", alinaIntegrationType.getTypeName(), e);
            return List.of();
        }
    }

    private static <F extends FunctionRequest, S extends AlinaIntegrationSettings, E extends AlinaIntegrationExecutor<S>> Optional<ChatFunction> getChatFunction(
            E executor,
            AlinaIntegrationFunction<E, F> function,
            AlinaIntegrationType<S, E> alinaIntegrationType,
            Integration integration) {

        try {
            ChatFunction chatFunction = ChatFunction.builder()
                    .name(alinaIntegrationType.getTypeName() + "_" + function.getFunctionName() + "_" + integration.id())
                    .description(integration.description() + ". " + function.getFunctionSuffixDescription())
                    .executor(function.getFunctionRequest(), function.createExecutor(executor))
                    .build();
            return Optional.ofNullable(chatFunction);
        } catch (Exception e) {
            logger.error("Cannot create chat function: {}", alinaIntegrationType.getTypeName(), e);
            return Optional.empty();
        }
    }

}
