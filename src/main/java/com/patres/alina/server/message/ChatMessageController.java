package com.patres.alina.server.message;

import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageSendModel;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    public ChatMessageController(final ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    public List<ChatMessageResponseModel> getMessagesByThreadId(final String chatThreadId) {
        return chatMessageService.getMessagesByThreadId(chatThreadId);
    }

    public void sendChatMessagesStream(final ChatMessageSendModel chatMessageSendModel) {
        chatMessageService.sendMessageStream(chatMessageSendModel);
    }

    public void cancelChatMessagesStream(final String chatThreadId) {
        chatMessageService.cancelStreaming(chatThreadId);
    }

    public void regenerateLastAssistantResponse(final String chatThreadId) {
        chatMessageService.regenerateLastAssistantResponse(chatThreadId);
    }

}
