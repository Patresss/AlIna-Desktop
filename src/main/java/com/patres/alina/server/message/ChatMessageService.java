package com.patres.alina.server.message;

import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.command.CommandDetail;
import com.patres.alina.common.thread.ChatThreadResponse;
import com.patres.alina.server.openai.OpenAiApiFacade;
import com.patres.alina.server.command.CommandFileService;
import com.patres.alina.server.thread.ChatThreadFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.patres.alina.server.message.ChatMessageMapper.toChatMessageResponseModel;
import static com.patres.alina.server.message.ChatMessageMapper.toChatMessageResponseModels;

import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;

@Service
public class ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageService.class);
    private static final PageRequest CONTEXT_PAGEABLE = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

    private final ChatMessageStorageRepository chatMessageRepository;
    private final CommandFileService commandFileService;
    private final OpenAiApiFacade openAiApi;
    private final StoreMessageManager storeMessageManager;
    private final ChatThreadFacade chatThreadFacade;

    public ChatMessageService(final ChatMessageStorageRepository chatMessageRepository,
                              final CommandFileService commandFileService,
                              final OpenAiApiFacade openAiApi,
                              final StoreMessageManager storeMessageManager,
                              final ChatThreadFacade chatThreadFacade) {
        this.chatMessageRepository = chatMessageRepository;
        this.openAiApi = openAiApi;
        this.commandFileService = commandFileService;
        this.storeMessageManager = storeMessageManager;
        this.chatThreadFacade = chatThreadFacade;
    }

    public synchronized ChatMessageResponseModel sendMessage(final ChatMessageSendModel chatMessageSendModel) {
        if (chatMessageSendModel.chatThreadId() == null) {
            final ChatThreadResponse newChatThread = chatThreadFacade.createNewChatThread();
            final ChatMessageSendModel chatMessageSendModelWithNewThread = new ChatMessageSendModel(
                    chatMessageSendModel.content(),
                    newChatThread.id(),
                    chatMessageSendModel.pluginId(),
                    chatMessageSendModel.styleType()
            );
            return sendMessageWithChatThread(chatMessageSendModelWithNewThread);
        }
        return sendMessageWithChatThread(chatMessageSendModel);
    }

    public synchronized void sendMessageStream(final ChatMessageSendModel chatMessageSendModel) {
        if (chatMessageSendModel.chatThreadId() == null) {
            final ChatThreadResponse newChatThread = chatThreadFacade.createNewChatThread();
            final ChatMessageSendModel chatMessageSendModelWithNewThread = new ChatMessageSendModel(
                    chatMessageSendModel.content(),
                    newChatThread.id(),
                    chatMessageSendModel.pluginId(),
                    chatMessageSendModel.styleType()
            );
            sendMessageStreamWithChatThread(chatMessageSendModelWithNewThread);
        } else {
            sendMessageStreamWithChatThread(chatMessageSendModel);
        }
    }

    public synchronized void sendMessageStreamWithChatThread(final ChatMessageSendModel chatMessageSendModel) {
        final String chatThreadId = chatMessageSendModel.chatThreadId();
        logger.info("Sending streaming '{}' content, threadId={} ...", chatMessageSendModel.content(), chatThreadId);

        final List<AbstractMessage> contextMessages = loadMessages(chatMessageSendModel.chatThreadId());
        
        final String chatContent = calculateContentWithPluginPrompt(chatMessageSendModel.content(), chatMessageSendModel.pluginId());
        final AbstractMessage userChatMessage = new UserMessage(chatContent);
        
        // Store the user message first
        storeMessageManager.storeMessage(userChatMessage, chatMessageSendModel, chatMessageSendModel.content());
        
        sendStreamingMessage(contextMessages, userChatMessage, chatMessageSendModel);
    }

    private void sendStreamingMessage(final List<AbstractMessage> contextMessages,
                                     final AbstractMessage message,
                                     final ChatMessageSendModel chatMessageSendModel) {
        contextMessages.add(message);
        
        final StringBuilder fullResponse = new StringBuilder();
        
        try {
            reactor.core.publisher.Flux<String> stream = openAiApi.sendMessageStream(contextMessages);
            
            stream.subscribe(
                token -> {
                    fullResponse.append(token);
                    DefaultEventBus.getInstance().publish(
                        new ChatMessageStreamEvent(chatMessageSendModel.chatThreadId(), token)
                    );
                },
                error -> {
                    logger.error("Error in streaming response", error);
                    DefaultEventBus.getInstance().publish(
                        new ChatMessageStreamEvent(
                            chatMessageSendModel.chatThreadId(), 
                            ChatMessageStreamEvent.StreamEventType.ERROR,
                            error.getMessage()
                        )
                    );
                },
                () -> {
                    logger.info("Streaming completed for threadId: {}", chatMessageSendModel.chatThreadId());
                    
                    // Create a ChatResponse-like object to store in database
                    // We need to create an AssistantMessage for storage
                    final AbstractMessage assistantMessage = new AssistantMessage(fullResponse.toString());
                    storeMessageManager.storeMessage(assistantMessage, chatMessageSendModel, fullResponse.toString());
                    
                    // Publish completion event
                    DefaultEventBus.getInstance().publish(
                        new ChatMessageStreamEvent(
                            chatMessageSendModel.chatThreadId(), 
                            ChatMessageStreamEvent.StreamEventType.COMPLETE
                        )
                    );
                }
            );
            
        } catch (Exception e) {
            logger.error("Error starting streaming", e);
            DefaultEventBus.getInstance().publish(
                new ChatMessageStreamEvent(
                    chatMessageSendModel.chatThreadId(), 
                    ChatMessageStreamEvent.StreamEventType.ERROR,
                    e.getMessage()
                )
            );
        }
    }

    public synchronized ChatMessageResponseModel sendMessageWithChatThread(final ChatMessageSendModel chatMessageSendModel) {
        final String chatThreadId = chatMessageSendModel.chatThreadId();
        logger.info("Sending '{}' content, threadId={} ...", chatMessageSendModel.content(), chatThreadId);

        final List<AbstractMessage> contextMessages = loadMessages(chatMessageSendModel.chatThreadId());


        final String chatContent = calculateContentWithPluginPrompt(chatMessageSendModel.content(), chatMessageSendModel.pluginId());
        final AbstractMessage userChatMessage =  new UserMessage(chatContent);

        logger.info("Processing first request '{}' content, threadId={} ...", chatMessageSendModel.content(), chatThreadId);
        final ChatResponse response = sendUserMessage(contextMessages, userChatMessage, chatMessageSendModel, chatMessageSendModel.content());

//        final ChatResponse functionableResponse = getFunctionableResponse(contextMessages, response, chatMessageSendModel);
//        logger.info("Received content for threadId {}: '{}'", chatThreadId, functionableResponse.getContent());
        return toChatMessageResponseModel(response, chatMessageSendModel);
    }
//
//    private ChatResponse sendFunctionMessage(final List<ChatMessage> contextMessages,
//                                            final ChatMessage message,
//                                            final ChatMessageSendModel chatMessageSendModel) {
//        contextMessages.add(message);
//        final ChatMessage response = openAiApi.sendFunctionMessage(message);
//        storeMessageManager.storeMessage(response, chatMessageSendModel);
//        return response;
//    }

    private ChatResponse sendMessage(final List<AbstractMessage> contextMessages,
                                    final AbstractMessage message,
                                    final ChatMessageSendModel chatMessageSendModel) {
        contextMessages.add(message);
        final ChatResponse response = openAiApi.sendMessage(contextMessages);
        storeMessageManager.storeMessage(response, chatMessageSendModel);
        return response;
    }

    private ChatResponse sendUserMessage(final List<AbstractMessage> contextMessages,
                                        final AbstractMessage message,
                                        final ChatMessageSendModel chatMessageSendModel,
                                        final String contentToDisplay) {
        storeMessageManager.storeMessage(message, chatMessageSendModel, contentToDisplay);
        return sendMessage(contextMessages, message, chatMessageSendModel);
    }

    public List<ChatMessageResponseModel> getMessagesByThreadId(final String chatThreadId) {
        final List<ChatMessageEntry> chatMessageEntries = chatMessageRepository.findMessagesToDisplay(chatThreadId);
        return toChatMessageResponseModels(chatMessageEntries, chatThreadId);
    }

    public void deleteMessagesByChatThreadId(final String chatThreadId) {
        chatMessageRepository.deleteByThreadId(chatThreadId);
    }

    private String calculateContentWithPluginPrompt(final String content, final String pluginId) {
        final Optional<CommandDetail> commandDetail = Optional.ofNullable(pluginId)
                .flatMap(commandFileService::getCommandDetails);

        final String commandContent = commandDetail
                .map(CommandDetail::systemPrompt)
                .orElse("");

        if (commandContent.isBlank()) {
            return content;
        }

        if (commandContent.contains("${message}")) {
            return commandContent.replaceAll("\\\\$\\\\{message}", content);
        } else {
            return commandContent + System.lineSeparator() + content;
        }
    }

    private List<AbstractMessage> loadMessages(final String chatThreadId) {
        final List<ChatMessageEntry> messages = chatMessageRepository.findMessagesForContext(chatThreadId, CONTEXT_PAGEABLE).reversed();
        return messages.stream()
                .map(ChatMessageMapper::toAbstractMessage)
                .collect(Collectors.toList());
    }

}