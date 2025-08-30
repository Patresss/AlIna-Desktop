package com.patres.alina.server.settings;

import com.patres.alina.common.settings.AssistantSettings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping("/assistant-settings")
    public AssistantSettings getAssistantSettings() {
        return settingsService.getApplicationSettings();
    }

    @PutMapping("/assistant-settings")
    public void updateAssistantSettings(@RequestBody AssistantSettings applicationSettings) {
        settingsService.saveDocument(applicationSettings);
    }

    @GetMapping("/assistant-settings/chat-models")
    public List<String> getChatModels() {
        return settingsService.getChatModels();
    }

}
