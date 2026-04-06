package com.patres.alina.server.configuration;


import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.event.WorkspaceSettingsUpdatedEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentConfiguration {

    @Bean
    public FileManager<AssistantSettings> assistantSettingsManager() {
        return new FileManager<>(
                "assistant",
                AssistantSettings.class,
                AssistantSettings::new);
    }

    @Bean
    public FileManager<WorkspaceSettings> workspaceSettingsManager() {
        return new FileManager<>(
                "context",
                WorkspaceSettings.class,
                WorkspaceSettings::new,
                WorkspaceSettingsUpdatedEvent::new
        );
    }

}
