package com.patres.alina.server.settings;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.event.AssistantSettingsUpdatedEvent;
import com.patres.alina.server.openai.OpenAiApi;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingsService {

    private final FileManager<AssistantSettings> assistantSettingsManager;
    private final OpenAiApi openAiApi;


    public SettingsService(FileManager<AssistantSettings> assistantSettingsManager, OpenAiApi openAiApi) {
        this.assistantSettingsManager = assistantSettingsManager;
        this.openAiApi = openAiApi;
    }

    public AssistantSettings getApplicationSettings() {
        return assistantSettingsManager.getSettings();
    }

    public void saveDocument(AssistantSettings settings) {
        assistantSettingsManager.saveDocument(settings);
        DefaultEventBus.getInstance().publish(new AssistantSettingsUpdatedEvent(settings));
    }

    public List<String> getChatModels() {
        return openAiApi.getChatModels();
    }

}
