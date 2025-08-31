package com.patres.alina.uidesktop.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.patres.alina.AppLauncher;
import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.SpeechToTextResponse;
import com.patres.alina.common.command.CommandCreateRequest;
import com.patres.alina.common.command.CommandDetail;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.thread.ChatThreadRenameRequest;
import com.patres.alina.common.thread.ChatThreadResponse;
import com.patres.alina.server.message.ChatMessageController;
import com.patres.alina.server.command.CommandController;
import com.patres.alina.server.settings.SettingsController;
import com.patres.alina.server.speech.SpeechToTextController;
import com.patres.alina.server.thread.ChatThreadController;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.auth.BasicAuthRequestInterceptor;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

import static com.patres.alina.uidesktop.settings.SettingsMangers.SERVER_SETTINGS;

public class BackendApi {

    public static ChatMessageResponseModel sendChatMessages(ChatMessageSendModel chatMessageSendModel) {
        return AppLauncher.getBean(ChatMessageController.class).sendChatMessages(chatMessageSendModel);
    }

    public static void sendChatMessagesStream(ChatMessageSendModel chatMessageSendModel) {
        AppLauncher.getBean(ChatMessageController.class).sendChatMessagesStream(chatMessageSendModel);
    }

    public static List<ChatMessageResponseModel> getMessagesByThreadId(String chatThreadId) {
        return AppLauncher.getBean(ChatMessageController.class).getMessagesByThreadId(chatThreadId);
    }


    public static List<ChatThreadResponse> getChatThreads() {
        return AppLauncher.getBean(ChatThreadController.class).getChatThreads();
    }

    public static  ChatThreadResponse createChatThread() {
        return AppLauncher.getBean(ChatThreadController.class).createNewChatThread();
    }

    public static void deleteChatThread(String chatThreadId) {
        AppLauncher.getBean(ChatThreadController.class).deleteChatThread(chatThreadId);
    }
    public static void renameChatThread(ChatThreadRenameRequest chatThreadRenameRequest) {
        AppLauncher.getBean(ChatThreadController.class).renameChatThread(chatThreadRenameRequest);
    }



    public static List<CardListItem> getCommandListItems() {
        return AppLauncher.getBean(CommandController.class).getCommandListItems();
    }

    public static CommandDetail getCommandDetails(String commandId) {
        return AppLauncher.getBean(CommandController.class).getCommandDetails(commandId);
    }

    public static void createCommandDetail(CommandCreateRequest commandCreateRequest) {
         AppLauncher.getBean(CommandController.class).createCommandDetail(commandCreateRequest);
    }

    public static void updateCommandDetail(CommandDetail commandDetail) {
         AppLauncher.getBean(CommandController.class).updateCommandDetail(commandDetail);
    }

    public static void deleteCommand(String commandId) {
         AppLauncher.getBean(CommandController.class).deleteCommand(commandId);
    }

    public static void updateCommandState(UpdateStateRequest updateStateRequest) {
         AppLauncher.getBean(CommandController.class).updateCommandState(updateStateRequest);
    }

    public static SpeechToTextResponse sendChatMessagesAsAudio(File file) {
        return AppLauncher.getBean(SpeechToTextController.class).speechToText((MultipartFile) file);
    }


    public static AssistantSettings getAssistantSettings(){
        return AppLauncher.getBean(SettingsController.class).getAssistantSettings();
    }

    public static void updateAssistantSettings(AssistantSettings assistantSettings){
        AppLauncher.getBean(SettingsController.class).updateAssistantSettings(assistantSettings);
    }

    public static List<String> getChatModels(){
        return AppLauncher.getBean(SettingsController.class).getChatModels();
    }
}