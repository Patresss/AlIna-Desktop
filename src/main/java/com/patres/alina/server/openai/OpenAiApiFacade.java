package com.patres.alina.server.openai;


import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.event.AssistantSettingsUpdatedEvent;
import com.patres.alina.server.mcp.McpToolIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Component
public class OpenAiApiFacade {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiApiFacade.class);
    public static final String SPEECH_TO_TEXT_MODEL = "whisper-1";

    private final OpenAiApiCreator openAiApiCreator;
    private ChatModel chatModel;
    private final McpToolIntegrationService mcpToolIntegrationService;

    public OpenAiApiFacade(FileManager<AssistantSettings> assistantSettingsManager,
                           OpenAiApiCreator openAiApiCreator,
                           com.patres.alina.server.mcp.McpToolIntegrationService mcpToolIntegrationService) {
        this.openAiApiCreator = openAiApiCreator;
        this.mcpToolIntegrationService = mcpToolIntegrationService;
        updateOpenAiService(assistantSettingsManager.getSettings());
        DefaultEventBus.getInstance().subscribe(AssistantSettingsUpdatedEvent.class, e -> {
            updateOpenAiService(e.getSettings());
        });
    }

    public void updateOpenAiService(final AssistantSettings settings) {
        try {
            chatModel = openAiApiCreator.createOpenAiChatModel(settings.openAiApiKey(), settings.chatModel());
            if (chatModel == null) {
                logger.warn("OpenAI chat model is null - API key not properly configured. Please set a valid API key in assistant settings.");
            } else {
                logger.info("OpenAI chat model updated successfully");
            }
        } catch (Exception e) {
            logger.error("Failed to update OpenAI service", e);
            chatModel = null;
        }
    }

    public String speechToText(final File audio) {
//        final TranscriptionResult transcription = service.createTranscription(
//                CreateTranscriptionRequest.builder()
//                        .model(SPEECH_TO_TEXT_MODEL)
//                        .language("pl") // TODO do not hardcode
//                        .build(),
//                audio.getPath());
        return "transcription.getText();";
    }

    public Flux<String> sendMessageStream(final List<AbstractMessage> messages) {
        if (chatModel == null) {
            logger.warn("Cannot send message - OpenAI chat model is not configured. Please set a valid API key in assistant settings.");
            return Flux.just("Error: OpenAI API key not configured. Please set a valid API key in assistant settings.");
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

    public List<String> getChatModels() {
        try {
            return Arrays.stream(OpenAiApi.ChatModel.values())
                    .map(OpenAiApi.ChatModel::getValue)
                    .sorted()
                    .toList();
        } catch (Exception e) {
            logger.error("Cannot receive models, e");
            return List.of();
        }
    }

}
