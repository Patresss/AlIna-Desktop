package com.patres.alina.server.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.server.thread.ChatThreadFacade;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class StoreMessageManager {

    private static final Logger logger = LoggerFactory.getLogger(StoreMessageManager.class);

    private final ChatThreadFacade chatThreadFacade;
    private final ChatMessageRepository chatMessageRepository;

    public StoreMessageManager(final ChatThreadFacade chatThreadFacade,
                               final ChatMessageRepository chatMessageRepository) {
        this.chatThreadFacade = chatThreadFacade;
        this.chatMessageRepository = chatMessageRepository;
    }


    void storeMessage(final ChatMessage chatMessage,
                      final ChatMessageSendModel chatMessageSendMode) {
        storeMessage(chatMessage, chatMessageSendMode, chatMessage.getContent());
    }

    void storeMessage(final ChatMessage chatMessage,
                      final ChatMessageSendModel chatMessageSendModel,
                      final String contentToDisplay) {
        final ChatMessageRole role = ChatMessageRole.findChatMessageRoleByValue(chatMessage.getRole());
        final ChatFunctionCall functionCall = chatMessage.getFunctionCall();


        final ChatMessageEntry chatMessageResponse = new ChatMessageEntry(
                UUID.randomUUID().toString(),
                chatMessageSendModel.chatThreadId(),
                contentToDisplay,
                role,
                ChatMessageStyleType.NONE,
                LocalDateTime.now(),
                chatMessage.getContent(),
                chatMessageSendModel.pluginId(),
                chatMessage.getName(),
                Optional.ofNullable(functionCall).map(ChatFunctionCall::getName).orElse(null),
                Optional.ofNullable(functionCall).map(ChatFunctionCall::getArguments).map(JsonNode::toString).orElse(null)
        );
        chatMessageRepository.save(chatMessageResponse);
        chatThreadFacade.setModifiedAt(chatMessageSendModel.chatThreadId());
    }

}