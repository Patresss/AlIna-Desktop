package com.patres.alina.uidesktop.backend;


import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.integration.*;
import com.patres.alina.common.logs.LogsResponse;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.SpeechToTextResponse;
import com.patres.alina.common.plugin.PluginCreateRequest;
import com.patres.alina.common.plugin.PluginDetail;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.thread.ChatThreadRenameRequest;
import com.patres.alina.common.thread.ChatThreadResponse;
import com.patres.alina.server.message.ChatMessageController;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.io.File;
import java.util.List;

public class BackendClient {

    private final ChatMessageController chatMessageController;

    public BackendClient(ChatMessageController chatMessageController) {
        this.chatMessageController = chatMessageController;
    }

    public ChatMessageResponseModel sendChatMessages(ChatMessageSendModel chatMessageSendModel) {
        return chatMessageController.sendChatMessages(chatMessageSendModel);
    }

    public List<ChatMessageResponseModel> getMessagesByThreadId(String chatThreadId) {
        return chatMessageController.getMessagesByThreadId(chatThreadId);
    }

}