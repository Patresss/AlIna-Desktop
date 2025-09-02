package com.patres.alina.server.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.server.message.ChatMessageStorageRepository;
import com.patres.alina.server.message.ConversationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class LocalStorageConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalStorageConfiguration.class);
    
    @Bean
    public Path localStorageBasePath() {
        String pathProperty = System.getProperty("storage.local.base.path", "data");
        Path dataDir = Paths.get(pathProperty).toAbsolutePath();
        
        try {
            Files.createDirectories(dataDir);
            logger.info("Created local storage base directory: {}", dataDir);
            return dataDir;
        } catch (IOException e) {
            logger.error("Failed to create local storage directory: {}", dataDir, e);
            throw new RuntimeException("Failed to create local storage directory", e);
        }
    }
    
    @Bean
    public Path conversationsStoragePath(Path localStorageBasePath) {
        Path conversationsDir = localStorageBasePath.resolve("conversations");
        
        try {
            Files.createDirectories(conversationsDir);
            logger.info("Created conversations storage directory: {}", conversationsDir);
            return conversationsDir;
        } catch (IOException e) {
            logger.error("Failed to create conversations storage directory: {}", conversationsDir, e);
            throw new RuntimeException("Failed to create conversations storage directory", e);
        }
    }
    
    @Bean
    public Path commandsStoragePath(Path localStorageBasePath) {
        Path commandsDir = localStorageBasePath.resolve("commands").normalize();
        
        try {
            Files.createDirectories(commandsDir);
            logger.info("Created commands storage directory: {}", commandsDir);
            return commandsDir;
        } catch (IOException e) {
            logger.error("Failed to create commands storage directory: {}", commandsDir, e);
            throw new RuntimeException("Failed to create commands storage directory", e);
        }
    }
    
    @Bean
    @Primary
    public ChatMessageStorageRepository conversationRepository(
            Path conversationsStoragePath, 
            ObjectMapper objectMapper) {
        
        logger.info("Configuring JSONL conversation storage at: {}", conversationsStoragePath);
        return new ConversationRepository(conversationsStoragePath, objectMapper);
    }

}
