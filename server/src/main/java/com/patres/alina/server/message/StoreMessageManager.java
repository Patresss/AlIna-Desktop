package com.patres.alina.server.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.server.thread.ChatThreadFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatResponse;
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


    void storeMessage(final ChatResponse chatMessage,
                      final ChatMessageSendModel chatMessageSendMode) {
        String text = chatMessage.getResult().getOutput().getText();
        MessageType messageType = chatMessage.getResult().getOutput().getMessageType();
        storeMessage(text, messageType, chatMessageSendMode, text);
    }

    void storeMessage(final String contentWithContext,
                      final MessageType messageType,
                      final ChatMessageSendModel chatMessageSendModel,
                      final String contentToDisplay) {
        final ChatMessageEntry chatMessageResponse = new ChatMessageEntry(
                UUID.randomUUID().toString(),
                chatMessageSendModel.chatThreadId(),
                contentToDisplay,
                messageType,
                ChatMessageStyleType.NONE,
                LocalDateTime.now(),
                contentWithContext,
                chatMessageSendModel.pluginId(),
                "chatMessage.getName()" // TODO
        );
        chatMessageRepository.save(chatMessageResponse);
        chatThreadFacade.setModifiedAt(chatMessageSendModel.chatThreadId());
    }

    void storeMessage(final AbstractMessage chatMessage,
                      final ChatMessageSendModel chatMessageSendModel,
                      final String contentToDisplay) {
        storeMessage(chatMessage.getText(), chatMessage.getMessageType(), chatMessageSendModel, contentToDisplay);
    }

}