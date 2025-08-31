package com.patres.alina.server.openai;


import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.event.AssistantSettingsUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Arrays;
import java.util.List;

@Component
public class OpenAiApiFacade {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiApiFacade.class);
    public static final String SPEECH_TO_TEXT_MODEL = "whisper-1";

    private final OpenAiApiCreator openAiApiCreator;
    private org.springframework.ai.chat.model.ChatModel chatModel;

    private String model;

    public OpenAiApiFacade(FileManager<AssistantSettings> assistantSettingsManager,
                           OpenAiApiCreator openAiApiCreator,
                           org.springframework.ai.chat.model.ChatModel chatModel
                     ) {
        this.chatModel = chatModel;
        this.openAiApiCreator = openAiApiCreator;
        updateOpenAiService(assistantSettingsManager.getSettings());
        DefaultEventBus.getInstance().subscribe(AssistantSettingsUpdatedEvent.class, e -> {
            updateOpenAiService(e.getSettings());
        });

    }

    public void updateOpenAiService(final AssistantSettings settings) {
        chatModel = openAiApiCreator.createOpenAiChatModel(settings.openAiApiKey(), settings.chatModel());
        model = settings.chatModel();
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

    public reactor.core.publisher.Flux<String> sendMessageStream(final List<AbstractMessage> messages) {
        List<Message> messageList = messages.stream().map(m -> (Message) m).toList();
        return ChatClient.create(chatModel)
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