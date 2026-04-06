package com.patres.alina.server.settings;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.event.AssistantSettingsUpdatedEvent;
import com.patres.alina.server.opencode.OpenCodeRuntimeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SettingsService {

    private final FileManager<AssistantSettings> assistantSettingsManager;
    private final OpenCodeRuntimeService openCodeRuntimeService;

    public SettingsService(FileManager<AssistantSettings> assistantSettingsManager,
                           OpenCodeRuntimeService openCodeRuntimeService) {
        this.assistantSettingsManager = assistantSettingsManager;
        this.openCodeRuntimeService = openCodeRuntimeService;
    }

    public AssistantSettings getApplicationSettings() {
        return assistantSettingsManager.getSettings();
    }

    public void saveDocument(AssistantSettings settings) {
        assistantSettingsManager.saveDocument(settings);
        DefaultEventBus.getInstance().publish(new AssistantSettingsUpdatedEvent(settings));
    }

    public List<String> getChatModels() {
        final List<String> models = openCodeRuntimeService.getAvailableModels();
        normalizeModelIfNeeded(models);
        return models;
    }

    private void normalizeModelIfNeeded(final List<String> availableModels) {
        final AssistantSettings current = assistantSettingsManager.getSettings();
        final String effective = openCodeRuntimeService.resolveEffectiveModelIdentifier(current, availableModels);
        if (effective == null || effective.isBlank() || effective.equals(current.resolveModelIdentifier())) {
            return;
        }

        final AssistantSettings normalized = new AssistantSettings(
                effective,
                current.systemPrompt(),
                current.numberOfMessagesInContext(),
                current.openAiApiKey(),
                current.anthropicApiKey(),
                current.googleApiKey(),
                current.timeoutSeconds()
        );
        saveDocument(normalized);
    }

}
