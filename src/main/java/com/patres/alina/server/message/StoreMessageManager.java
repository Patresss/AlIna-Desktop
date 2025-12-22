package com.patres.alina.server.message;

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
import java.util.UUID;

@Component
public class StoreMessageManager {

    private static final Logger logger = LoggerFactory.getLogger(StoreMessageManager.class);

    private final ChatThreadFacade chatThreadFacade;
    private final ChatMessageStorageRepository chatMessageStorageRepository;

    public StoreMessageManager(final ChatThreadFacade chatThreadFacade,
                               final ChatMessageStorageRepository chatMessageStorageRepository
    ) {
        this.chatThreadFacade = chatThreadFacade;
        this.chatMessageStorageRepository = chatMessageStorageRepository;
    }


    void storeMessage(final ChatResponse chatMessage,
                      final ChatMessageSendModel chatMessageSendMode) {
        final String text = chatMessage.getResult().getOutput().getText();
        final MessageType messageType = chatMessage.getResult().getOutput().getMessageType();
        storeMessage(text, messageType, chatMessageSendMode, text);
    }

    void storeMessage(final String contentWithContext,
                      final MessageType messageType,
                      final ChatMessageSendModel chatMessageSendModel,
                      final String contentToDisplay) {
        final ChatMessageStyleType styleType = chatMessageSendModel.styleType() == null ? ChatMessageStyleType.NONE : chatMessageSendModel.styleType();
        final ChatMessage message = new ChatMessage(
                UUID.randomUUID().toString(),
                chatMessageSendModel.chatThreadId(),
                contentToDisplay,
                messageType,
                styleType,
                LocalDateTime.now(),
                contentWithContext,
                chatMessageSendModel.commandId(),
                "chatMessage.getName()"
        );
        chatMessageStorageRepository.save(message);
        chatThreadFacade.setModifiedAt(chatMessageSendModel.chatThreadId());
    }

    void storeMessage(final AbstractMessage chatMessage,
                      final ChatMessageSendModel chatMessageSendModel,
                      final String contentToDisplay) {
        storeMessage(chatMessage.getText(), chatMessage.getMessageType(), chatMessageSendModel, contentToDisplay);
    }

}
