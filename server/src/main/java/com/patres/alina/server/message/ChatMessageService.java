package com.patres.alina.server.message;

import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.plugin.PluginDetail;
import com.patres.alina.common.thread.ChatThreadResponse;
import com.patres.alina.server.openai.OpenAiApiFacade;
import com.patres.alina.server.plugin.PluginService;
import com.patres.alina.server.thread.ChatThreadFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.patres.alina.common.message.ChatMessageRole.USER;
import static com.patres.alina.server.message.ChatMessageMapper.toChatMessageResponseModel;
import static com.patres.alina.server.message.ChatMessageMapper.toChatMessageResponseModels;

@Service
public class ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageService.class);
    private static final PageRequest CONTEXT_PAGEABLE = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));

    private final ChatMessageRepository chatMessageRepository;
    private final PluginService pluginService;
    private final OpenAiApiFacade openAiApi;
    private final StoreMessageManager storeMessageManager;
    private final ChatThreadFacade chatThreadFacade;

    public ChatMessageService(final ChatMessageRepository chatMessageRepository,
                              final PluginService pluginService,
                              final OpenAiApiFacade openAiApi,
                              final StoreMessageManager storeMessageManager,
                              final ChatThreadFacade chatThreadFacade) {
        this.chatMessageRepository = chatMessageRepository;
        this.openAiApi = openAiApi;
        this.pluginService = pluginService;
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
        final List<ChatMessageEntry> chatMessageEntries = chatMessageRepository.findChatMessagesToDisplay(chatThreadId);
        return toChatMessageResponseModels(chatMessageEntries, chatThreadId);
    }

    public void deleteMessagesByChatThreadId(final String chatThreadId) {
        chatMessageRepository.deleteByChatThreadId(chatThreadId);
    }

    private String calculateContentWithPluginPrompt(final String content, final String pluginId) {
        final Optional<PluginDetail> pluginDetail = Optional.ofNullable(pluginId)
                .flatMap(pluginService::getPluginDetails);

        final String pluginContent = pluginDetail
                .map(PluginDetail::systemPrompt)
                .orElse("");

        if (pluginContent.isBlank()) {
            return content;
        }

        if (pluginContent.contains("${message}")) {
            return pluginContent.replaceAll("\\$\\{message}", content);
        } else {
            return pluginContent + System.lineSeparator() + content;
        }
    }

    private List<AbstractMessage> loadMessages(final String chatThreadId) {
        final List<ChatMessageEntry> messages = chatMessageRepository.findChatMessagesForContext(chatThreadId, CONTEXT_PAGEABLE).reversed();
        return messages.stream()
                .map(ChatMessageMapper::toAbstractMessage)
                .collect(Collectors.toList());
    }

}