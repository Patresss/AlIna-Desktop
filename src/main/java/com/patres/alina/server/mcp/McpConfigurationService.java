package com.patres.alina.server.mcp;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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

    private final Path configPath; // primary/shared config
    private final Path localOverridePath; // preferred local override
    private final McpConfigurationValidator validator;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    private Optional<McpServersConfig> currentConfig = Optional.empty();

    @Autowired
    public McpConfigurationService(final McpConfigurationValidator validator,
                                   final ApplicationEventPublisher eventPublisher) {
        this.validator = validator;
        this.eventPublisher = eventPublisher;
        this.configPath = Paths.get("data/mcp/mcp-servers.json").toAbsolutePath();
        this.localOverridePath = Paths.get("data/mcp/mcp-servers.local.json").toAbsolutePath();
        this.objectMapper = createObjectMapper();
        ensureDirectoryExists(configPath.getParent());
        logger.info("McpConfigurationService initialized with configuration path: {} (local override: {})",
                configPath, localOverridePath);
    }

    public synchronized McpServersConfig loadConfiguration() {
        if (currentConfig.isEmpty()) {
            currentConfig = Optional.of(loadFromFileOrCreateEmpty());
        }
        return currentConfig.get();
    }

    public void saveConfiguration(final McpServersConfig config) {
        logger.info("Saving MCP configuration to: {}", configPath);

        final McpConfigurationValidator.ValidationResult result = validator.validate(config);
        if (!result.isValid()) {
            throw new IllegalArgumentException("Cannot save invalid MCP configuration: " + result.getErrors());
        }
        logWarningsIfAny(result);

        try {
            final String jsonContent = objectMapper.writeValueAsString(config);
            Files.writeString(configPath, jsonContent, StandardCharsets.UTF_8);
            currentConfig = Optional.of(config);
            logger.info("MCP configuration saved successfully");
            eventPublisher.publishEvent(new McpConfigurationChangedEvent(config));
        } catch (final IOException e) {
            logger.error("Failed to save MCP configuration to {}", configPath, e);
            throw new RuntimeException("Failed to save MCP configuration", e);
        }
    }

    public void reloadConfiguration() {
        logger.info("Reloading MCP configuration");
        currentConfig = Optional.empty();
        final McpServersConfig newConfig = loadConfiguration();
        eventPublisher.publishEvent(new McpConfigurationChangedEvent(newConfig));
    }

    public Path getConfigurationPath() {
        return configPath;
    }

    private McpServersConfig loadFromFileOrCreateEmpty() {
        final Path effectivePath = selectEffectiveConfigPath();
        logger.info("Loading MCP configuration from: {}", effectivePath);
        try {
            if (Files.exists(effectivePath)) {
                return readAndValidateConfig(effectivePath);
            }
            logger.info("MCP configuration file not found at {}, creating empty configuration at primary path {}",
                    effectivePath, configPath);
            final McpServersConfig empty = McpServersConfig.empty();
            // Persist empty baseline only to the primary shared config
            saveConfiguration(empty);
            return empty;
        } catch (final IOException e) {
            logger.error("Failed to load MCP configuration from {}", effectivePath, e);
            return McpServersConfig.empty();
        }
    }

    private McpServersConfig readAndValidateConfig(final Path path) throws IOException {
        final String jsonContent = Files.readString(path, StandardCharsets.UTF_8);
        final McpServersConfig config = objectMapper.readValue(jsonContent, McpServersConfig.class);
        final McpConfigurationValidator.ValidationResult result = validator.validate(config);
        if (!result.isValid()) {
            logger.error("MCP configuration validation failed for {}: {}", path, result.getErrors());
            logger.warn("Using empty configuration due to validation errors");
            return McpServersConfig.empty();
        }
        logWarningsIfAny(result);
        logger.info("MCP configuration loaded successfully from {} with {} servers", path, config.mcpServers().size());
        return config;
    }

    private Path selectEffectiveConfigPath() {
        try {
            if (Files.exists(localOverridePath)) {
                return localOverridePath;
            }
        } catch (final Exception e) {
            logger.debug("Error checking local override paths", e);
        }
        return configPath;
    }

    private void ensureDirectoryExists(final Path dir) {
        try {
            Files.createDirectories(dir);
            logger.info("MCP configuration directory created: {}", dir);
        } catch (final IOException e) {
            logger.error("Failed to create MCP configuration directory", e);
            throw new RuntimeException("Failed to create MCP configuration directory", e);
        }
    }

    private void logWarningsIfAny(final McpConfigurationValidator.ValidationResult result) {
        if (result.hasWarnings()) {
            logger.warn("MCP configuration warnings: {}", result.getWarnings());
        }
    }

    private ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    public static class McpConfigurationChangedEvent {
        private final McpServersConfig configuration;

        public McpConfigurationChangedEvent(final McpServersConfig configuration) {
            this.configuration = configuration;
        }

        public McpServersConfig getConfiguration() {
            return configuration;
        }
    }
}
