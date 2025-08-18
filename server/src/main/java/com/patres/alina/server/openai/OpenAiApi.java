package com.patres.alina.server.openai;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.event.AssistantSettingsUpdatedEvent;
import com.patres.alina.server.openai.function.FunctionService;
import com.theokanning.openai.OpenAiHttpException;
import com.theokanning.openai.audio.CreateTranscriptionRequest;
import com.theokanning.openai.audio.TranscriptionResult;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.model.Model;
import com.theokanning.openai.service.FunctionExecutor;
import com.theokanning.openai.service.OpenAiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class OpenAiApi {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiApi.class);
    private static final String CHAT_MODEL_PATTERN = "gpt";
    public static final String SPEECH_TO_TEXT_MODEL = "whisper-1";

    private final FunctionService functionService;
    private OpenAiService service;
    private String model;

    public OpenAiApi(FileManager<AssistantSettings> assistantSettingsManager, FunctionService functionService) {
        this.functionService = functionService;
        updateOpenAiService(assistantSettingsManager.getSettings());
        DefaultEventBus.getInstance().subscribe(AssistantSettingsUpdatedEvent.class, e -> {
            updateOpenAiService(e.getSettings());
        });

    }

    public void updateOpenAiService(final AssistantSettings settings) {
        service = new OpenAiService(settings.openAiApiKey(), Duration.ofSeconds(settings.timeoutSeconds()));
        model = settings.chatModel();
    }

    public String speechToText(final File audio) {
        final TranscriptionResult transcription = service.createTranscription(
                CreateTranscriptionRequest.builder()
                        .model(SPEECH_TO_TEXT_MODEL)
                        .language("pl") // TODO do not hardcode
                        .build(),
                audio.getPath());
        return transcription.getText();
    }

    public ChatMessage sendMessage(final List<ChatMessage> messages, final ChatMessage currentChatMessage) {
        messages.add(currentChatMessage);
        final ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model(model)
                .messages(messages)
                .functions(functionService.getChatFunctions())
                .build();
        return sendMessage(chatCompletionRequest);
    }

    public ChatMessage sendMessage(final List<ChatMessage> messages) {
        final ChatCompletionRequest chatCompletionRequest = ChatCompletionRequest
                .builder()
                .model(model)
                .messages(messages)
                .functions(functionService.getChatFunctions())
                .build();
        return sendMessage(chatCompletionRequest);
    }

    private ChatMessage sendMessage(final ChatCompletionRequest chatCompletionRequest) {
        try {
            return service.createChatCompletion(chatCompletionRequest).getChoices().getFirst().getMessage();
        } catch (OpenAiHttpException e) {
            logger.error("OpenAi Http exception when sending chat completion request. {}", e.getMessage(), e);
            throw e;
        }
    }

    public ChatMessage sendFunctionMessage(final ChatMessage functionMessage) {
        final ChatFunctionCall functionCall = functionMessage.getFunctionCall();
        final FunctionExecutor functionExecutor = new FunctionExecutor(functionService.getChatFunctions());
        return functionExecutor.executeAndConvertToMessageHandlingExceptions(functionCall);
    }

    public ChatMessage paraphraseFunctionMessage(final ChatMessage functionMessage, final List<ChatMessage> messages) {
        final List<ChatMessage> chatMessagesWithFunction = new ArrayList<>(messages);
        chatMessagesWithFunction.add(functionMessage);
        var functionRequest = ChatCompletionRequest
                .builder()
                .model(model)
                .messages(chatMessagesWithFunction)
                .functions(functionService.getChatFunctions())
                .build();
        return sendMessage(functionRequest);
    }


    public boolean isFunctionMessage(ChatMessage chatMessage) {
        return chatMessage.getFunctionCall() != null;
    }

    public List<String> getChatModels() {
        try {
            return service.listModels().stream()
                    .map(Model::getId)
                    .filter(model -> model.toLowerCase().contains(CHAT_MODEL_PATTERN))
                    .sorted()
                    .toList();
        } catch (Exception e) {
            logger.error("Cannot receive models, e");
            return List.of();
        }
    }

}