package com.patres.alina.server.settings;

import com.patres.alina.common.settings.AssistantSettings;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public AssistantSettings getAssistantSettings() {
        return settingsService.getApplicationSettings();
    }

    public void updateAssistantSettings(AssistantSettings applicationSettings) {
        settingsService.saveDocument(applicationSettings);
    }

    public List<String> getChatModels() {
        return settingsService.getChatModels();
    }

}
