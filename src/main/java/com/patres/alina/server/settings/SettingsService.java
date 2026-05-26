package com.patres.alina.server.settings;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.event.AssistantSettingsUpdatedEvent;
import com.patres.alina.server.ai.AiRuntimeRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingsService {

    private final FileManager<AssistantSettings> assistantSettingsManager;
    private final AiRuntimeRegistry aiRuntimeRegistry;

    public SettingsService(FileManager<AssistantSettings> assistantSettingsManager,
                           AiRuntimeRegistry aiRuntimeRegistry) {
        this.assistantSettingsManager = assistantSettingsManager;
        this.aiRuntimeRegistry = aiRuntimeRegistry;
    }

    public AssistantSettings getApplicationSettings() {
        return assistantSettingsManager.getSettings();
    }

    public void saveDocument(AssistantSettings settings) {
        assistantSettingsManager.saveDocument(settings);
        DefaultEventBus.getInstance().publish(new AssistantSettingsUpdatedEvent(settings));
    }

    public List<String> getChatModels() {
        return aiRuntimeRegistry.currentRuntime().getAvailableModels();
    }

}
