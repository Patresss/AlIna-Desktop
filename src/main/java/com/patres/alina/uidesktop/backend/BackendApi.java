package com.patres.alina.uidesktop.backend;

import com.patres.alina.AppLauncher;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.SpeechToTextResponse;
import com.patres.alina.server.command.Command;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.thread.ChatThreadRenameRequest;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.message.ChatMessageController;
import com.patres.alina.server.command.CommandController;
import com.patres.alina.server.settings.SettingsController;
import com.patres.alina.server.speech.SpeechToTextController;
import com.patres.alina.server.thread.ChatThreadController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;


public class BackendApi {

    public static void sendChatMessagesStream(ChatMessageSendModel chatMessageSendModel) {
        AppLauncher.getBean(ChatMessageController.class).sendChatMessagesStream(chatMessageSendModel);
    }

    public static List<ChatMessageResponseModel> getMessagesByThreadId(String chatThreadId) {
        return AppLauncher.getBean(ChatMessageController.class).getMessagesByThreadId(chatThreadId);
    }


    public static List<ChatThread> getChatThreads() {
        return AppLauncher.getBean(ChatThreadController.class).getChatThreads();
    }

    public static ChatThread createChatThread() {
        return AppLauncher.getBean(ChatThreadController.class).createNewChatThread();
    }

    public static void deleteChatThread(String chatThreadId) {
        AppLauncher.getBean(ChatThreadController.class).deleteChatThread(chatThreadId);
    }

    public static void renameChatThread(ChatThreadRenameRequest chatThreadRenameRequest) {
        AppLauncher.getBean(ChatThreadController.class).renameChatThread(chatThreadRenameRequest);
    }


    public static List<Command> getEnabledCommands() {
        return AppLauncher.getBean(CommandController.class).getEnabledCommands();
    }

    public static List<Command> getAllCommands() {
        return AppLauncher.getBean(CommandController.class).getAllCommands();
    }

    public static Command getCommand(String commandId) {
        return AppLauncher.getBean(CommandController.class).getCommand(commandId);
    }

    public static void createCommand(Command command) {
        AppLauncher.getBean(CommandController.class).createCommand(command);
    }

    public static void updateCommand(Command command) {
        AppLauncher.getBean(CommandController.class).updateCommand(command);
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


    public static AssistantSettings getAssistantSettings() {
        return AppLauncher.getBean(SettingsController.class).getAssistantSettings();
    }

    public static void updateAssistantSettings(AssistantSettings assistantSettings) {
        AppLauncher.getBean(SettingsController.class).updateAssistantSettings(assistantSettings);
    }

    public static List<String> getChatModels() {
        return AppLauncher.getBean(SettingsController.class).getChatModels();
    }
}
