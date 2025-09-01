package com.patres.alina.server.mcp;

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
public class McpConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(McpConfiguration.class);

    /**
     * Ensures MCP storage directory exists
     */
    @Bean
    public Path mcpStoragePath() {
        Path mcpDir = Paths.get("data/mcp").toAbsolutePath();
        
        try {
            Files.createDirectories(mcpDir);
            logger.info("Created MCP storage directory: {}", mcpDir);
            return mcpDir;
        } catch (IOException e) {
            logger.error("Failed to create MCP storage directory: {}", mcpDir, e);
            throw new RuntimeException("Failed to create MCP storage directory", e);
        }
    }

}
