package com.patres.alina.server.mcp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class McpConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(McpConfigurationService.class);
    
    private final Path configPath;
    private final McpConfigurationValidator validator;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    
    private Optional<McpServersConfig> currentConfig = Optional.empty();

    @Autowired
    public McpConfigurationService(McpConfigurationValidator validator, 
                                   ApplicationEventPublisher eventPublisher) {
        this.validator = validator;
        this.eventPublisher = eventPublisher;
        this.configPath = Paths.get("data/mcp/mcp-servers.json").toAbsolutePath();
        this.objectMapper = createObjectMapper();
        
        // Ensure directory exists
        try {
            Files.createDirectories(configPath.getParent());
            logger.info("MCP configuration directory created: {}", configPath.getParent());
        } catch (IOException e) {
            logger.error("Failed to create MCP configuration directory", e);
            throw new RuntimeException("Failed to create MCP configuration directory", e);
        }
        
        logger.info("McpConfigurationService initialized with config path: {}", configPath);
    }

    public synchronized McpServersConfig loadConfiguration() {
        if (currentConfig.isEmpty()) {
            currentConfig = Optional.of(loadConfigurationFromFile());
        }
        return currentConfig.get();
    }

    private McpServersConfig loadConfigurationFromFile() {
        logger.info("Loading MCP configuration from: {}", configPath);
        
        try {
            if (Files.exists(configPath)) {
                String jsonContent = Files.readString(configPath, StandardCharsets.UTF_8);
                McpServersConfig config = objectMapper.readValue(jsonContent, McpServersConfig.class);
                
                // Validate configuration
                McpConfigurationValidator.ValidationResult result = validator.validate(config);
                if (!result.isValid()) {
                    logger.error("MCP configuration validation failed: {}", result.getErrors());
                    logger.warn("Using empty configuration due to validation errors");
                    return McpServersConfig.empty();
                }
                
                if (result.hasWarnings()) {
                    logger.warn("MCP configuration warnings: {}", result.getWarnings());
                }
                
                logger.info("MCP configuration loaded successfully with {} servers", 
                           config.mcpServers().size());
                return config;
                
            } else {
                logger.info("MCP configuration file not found at {}, creating empty configuration", configPath);
                McpServersConfig emptyConfig = McpServersConfig.empty();
                saveConfiguration(emptyConfig);
                return emptyConfig;
            }
            
        } catch (IOException e) {
            logger.error("Failed to load MCP configuration from {}", configPath, e);
            return McpServersConfig.empty();
        }
    }

    public void saveConfiguration(McpServersConfig config) {
        logger.info("Saving MCP configuration to: {}", configPath);
        
        // Validate before saving
        McpConfigurationValidator.ValidationResult result = validator.validate(config);
        if (!result.isValid()) {
            throw new IllegalArgumentException("Cannot save invalid MCP configuration: " + result.getErrors());
        }
        
        if (result.hasWarnings()) {
            logger.warn("MCP configuration warnings: {}", result.getWarnings());
        }
        
        try {
            String jsonContent = objectMapper.writeValueAsString(config);
            Files.writeString(configPath, jsonContent, StandardCharsets.UTF_8);
            
            currentConfig = Optional.of(config);
            logger.info("MCP configuration saved successfully");
            
            // Publish configuration change event
            eventPublisher.publishEvent(new McpConfigurationChangedEvent(config));
            
        } catch (IOException e) {
            logger.error("Failed to save MCP configuration to {}", configPath, e);
            throw new RuntimeException("Failed to save MCP configuration", e);
        }
    }

    public void reloadConfiguration() {
        logger.info("Reloading MCP configuration");
        currentConfig = Optional.empty();
        McpServersConfig newConfig = loadConfiguration();
        
        // Publish configuration change event
        eventPublisher.publishEvent(new McpConfigurationChangedEvent(newConfig));
    }

    public Path getConfigurationPath() {
        return configPath;
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    // Configuration change event
    public static class McpConfigurationChangedEvent {
        private final McpServersConfig configuration;

        public McpConfigurationChangedEvent(McpServersConfig configuration) {
            this.configuration = configuration;
        }

        public McpServersConfig getConfiguration() {
            return configuration;
        }
    }
}