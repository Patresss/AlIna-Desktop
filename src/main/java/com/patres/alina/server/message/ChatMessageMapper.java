package com.patres.alina.server.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.messages.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public final class ChatMessageMapper {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageMapper.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();


    public static ChatMessageResponseModel toChatMessageResponseModel(final ChatResponse chatMessage, final ChatMessageSendModel chatMessageSendModel) {
        MessageType messageType = chatMessage.getResult().getOutput().getMessageType();
        return new ChatMessageResponseModel(
                chatMessage.getResult().getOutput().getText(),
                ChatMessageRole.findChatMessageRoleByValue(messageType.getValue()), // TODO replace it with MessageType
                LocalDateTime.now(),
                chatMessageSendModel.styleType(),
                chatMessageSendModel.chatThreadId()
        );
    }

    public static ChatMessageResponseModel toChatMessageResponseModel(final ChatMessageEntry chatMessageEntry, final String chatThreadId) {
        return new ChatMessageResponseModel(
                chatMessageEntry.content(),
                ChatMessageRole.findChatMessageRoleByValue(chatMessageEntry.role().getValue()),
                chatMessageEntry.createdAt(),
                chatMessageEntry.styleType(),
                chatThreadId
        );
    }

    public static List<ChatMessageResponseModel> toChatMessageResponseModels(final List<ChatMessageEntry> chatMessageEntries, final String chatThreadId) {
        return chatMessageEntries.stream()
                .map(chatMessageEntry -> toChatMessageResponseModel(chatMessageEntry, chatThreadId))
                .toList();
    }

    public static AbstractMessage toAbstractMessage(final ChatMessageEntry entity) {
        return switch (entity.role()) {
            case USER -> new UserMessage(entity.contentWithContext());
            case ASSISTANT -> new AssistantMessage(entity.contentWithContext());
            case SYSTEM -> new SystemMessage(entity.contentWithContext());
//            case TOOL: TODO implement ToolMessage
//                return new ToolResponseMessage(entity.contentWithContext());
            default -> throw new IllegalArgumentException("Unsupported message role: " + entity.role());
        };
    }

}
