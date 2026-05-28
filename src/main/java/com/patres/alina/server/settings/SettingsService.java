package com.patres.alina.server.settings;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.event.AssistantSettingsUpdatedEvent;
import com.patres.alina.server.agent.AgentRuntimeSelector;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingsService {

    private final FileManager<AssistantSettings> assistantSettingsManager;
    private final AgentRuntimeSelector agentRuntimeSelector;

    public SettingsService(FileManager<AssistantSettings> assistantSettingsManager,
                           AgentRuntimeSelector agentRuntimeSelector) {
        this.assistantSettingsManager = assistantSettingsManager;
        this.agentRuntimeSelector = agentRuntimeSelector;
    }

    public AssistantSettings getApplicationSettings() {
        return assistantSettingsManager.getSettings();
    }

    public void saveDocument(AssistantSettings settings) {
        assistantSettingsManager.saveDocument(settings);
        DefaultEventBus.getInstance().publish(new AssistantSettingsUpdatedEvent(settings));
    }

    public List<String> getChatModels() {
        return agentRuntimeSelector.active().getAvailableModels();
    }

}
