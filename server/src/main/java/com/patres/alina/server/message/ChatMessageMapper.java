package com.patres.alina.server.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.theokanning.openai.completion.chat.ChatFunctionCall;
import com.theokanning.openai.completion.chat.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

public final class ChatMessageMapper {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageMapper.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();


    public static ChatMessageResponseModel toChatMessageResponseModel(final ChatMessage chatMessage, final ChatMessageSendModel chatMessageSendModel) {
        return new ChatMessageResponseModel(
                chatMessage.getContent(),
                ChatMessageRole.findChatMessageRoleByValue(chatMessage.getRole()),
                LocalDateTime.now(),
                chatMessageSendModel.styleType(),
                chatMessageSendModel.chatThreadId()
        );
    }

    public static ChatMessageResponseModel toChatMessageResponseModel(final ChatMessageEntry chatMessageEntry, final String chatThreadId) {
        return new ChatMessageResponseModel(
                chatMessageEntry.content(),
                chatMessageEntry.role(),
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

    public static ChatMessage toChatMessage(final ChatMessageEntry entity) {
        final ChatMessage chatMessage = new ChatMessage(entity.role().getChatMessageRole(), entity.contentWithContext(), entity.name());
        if (entity.functionArguments() != null && entity.functionName() != null) {
            try {
                final JsonNode jsonNode = MAPPER.readTree(entity.functionArguments());
                chatMessage.setFunctionCall(new ChatFunctionCall(entity.functionName(), jsonNode));
            } catch (JsonProcessingException e) {
                logger.error("Cannot map ChatMessageEntry to ChatMessage", e);
            }
        }
        return chatMessage;
    }

}
