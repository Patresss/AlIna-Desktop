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
import feign.Headers;
import feign.Param;
import feign.RequestLine;

import java.io.File;
import java.util.List;

public interface AlinaRestClient {

    @RequestLine("GET /assistant-settings")
    AssistantSettings getAssistantSettings();

    @RequestLine("PUT /assistant-settings")
    @Headers("Content-Type: application/json")
    void updateAssistantSettings(AssistantSettings assistantSettings);

    @RequestLine("GET /assistant-settings/chat-models")
    List<String> getChatModels();

    @RequestLine("POST /chat-messages")
    @Headers("Content-Type: application/json")
    ChatMessageResponseModel sendChatMessages(ChatMessageSendModel chatMessageSendModel);

    @RequestLine("GET /chat-messages/{chatThreadId}")
    List<ChatMessageResponseModel> getMessagesByThreadId(@Param("chatThreadId") String chatThreadId);


    @RequestLine("GET /chat-threads")
    List<ChatThreadResponse> getChatThreads();

    @RequestLine("POST /chat-threads/new")
    ChatThreadResponse createChatThread();

    @RequestLine("DELETE /chat-threads/{chatThreadId}")
    void removeChatThread(@Param("chatThreadId") String chatThreadId);

    @RequestLine("PATCH /chat-threads/name")
    @Headers("Content-Type: application/json")
    void renameChatThread(ChatThreadRenameRequest chatThreadRenameRequest);


    @RequestLine("GET /plugins")
    List<CardListItem> getPluginListItems();

    @RequestLine("GET /plugins/details/{pluginId}")
    PluginDetail getPluginDetails(@Param("pluginId") String pluginId);

    @RequestLine("POST /plugins")
    @Headers("Content-Type: application/json")
    void createPluginDetail(PluginCreateRequest pluginCreateRequest);

    @RequestLine("PUT /plugins")
    @Headers("Content-Type: application/json")
    void updatePluginDetail(PluginDetail pluginDetail);

    @RequestLine("DELETE /plugins/{pluginId}")
    void deletePlugin(@Param("pluginId") String pluginId);

    @RequestLine("PATCH /plugins/state")
    @Headers("Content-Type: application/json")
    void updatePluginState(UpdateStateRequest updateStateRequest);


    @RequestLine("GET /integrations/available")
    List<IntegrationToAdd> getAvailableIntegrationToAdd();

    @RequestLine("GET /integrations")
    List<CardListItem> getIntegrationListItem();

    @RequestLine("GET /integrations/{id}")
    IntegrationDetails getIntegrationById(@Param("id") final String id);

    @RequestLine("GET /integrations/types/{integrationType}")
    IntegrationDetails getNewIntegrationByIntegrationType(@Param("integrationType") final String integrationType);

    @RequestLine("POST /integrations")
    @Headers("Content-Type: application/json")
    IntegrationSaved createIntegration(IntegrationCreateRequest integrationCreateRequest);

    @RequestLine("PUT /integrations")
    @Headers("Content-Type: application/json")
    IntegrationSaved updateIntegration(IntegrationUpdateRequest integrationUpdateRequest);

    @RequestLine("DELETE /integrations/{id}")
    void deleteIntegration(@Param("id") final String id);

    @RequestLine("PATCH /integrations/state")
    @Headers("Content-Type: application/json")
    void updateIntegrationState(UpdateStateRequest updateStateRequest);

    @RequestLine("POST /speech")
    @Headers("Content-Type: multipart/form-data")
    SpeechToTextResponse sendChatMessagesAsAudio(@Param("file") File file);

    @RequestLine("GET /logs")
    LogsResponse getLogs();

}