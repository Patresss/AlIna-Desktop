package com.patres.alina.server.message;

import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ContextMessage;
import com.patres.alina.server.thread.ChatThreadFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    void storeMessage(final String contentWithContext,
                      final ChatMessageRole role,
                      final ChatMessageSendModel chatMessageSendModel,
                      final String contentToDisplay) {
        final ChatMessageStyleType styleType = chatMessageSendModel.styleType() == null ? ChatMessageStyleType.NONE : chatMessageSendModel.styleType();
        final ChatMessage message = new ChatMessage(
                UUID.randomUUID().toString(),
                chatMessageSendModel.chatThreadId(),
                contentToDisplay,
                role,
                styleType,
                LocalDateTime.now(),
                contentWithContext,
                chatMessageSendModel.commandId(),
                "chatMessage.getName()"
        );
        chatMessageStorageRepository.save(message);
        chatThreadFacade.setModifiedAt(chatMessageSendModel.chatThreadId());
    }

    void storeMessage(final ContextMessage contextMessage,
                      final ChatMessageSendModel chatMessageSendModel,
                      final String contentToDisplay) {
        storeMessage(contextMessage.text(), contextMessage.role(), chatMessageSendModel, contentToDisplay);
    }

}
