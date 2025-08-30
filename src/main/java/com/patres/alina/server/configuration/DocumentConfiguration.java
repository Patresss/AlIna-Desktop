package com.patres.alina.server.configuration;


import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocumentConfiguration {

    @Bean
    public FileManager<AssistantSettings> assistantSettingsManager() {
        return new FileManager<>(
                "AssistantSettings",
                AssistantSettings.class,
                AssistantSettings::new);
    }

}
