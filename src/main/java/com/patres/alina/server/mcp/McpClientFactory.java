package com.patres.alina.server.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class McpClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(McpClientFactory.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    public McpSyncClient createClient(String serverName, McpServerConfiguration config) {
        logger.info("Creating MCP client for server: {}", serverName);
        
        try {
            // Build server parameters
            ServerParameters.Builder paramsBuilder = ServerParameters.builder(config.command());
            
            // Add arguments if provided
            if (config.args() != null && !config.args().isEmpty()) {
                paramsBuilder.args(config.args());
            }
            
            // Add environment variables if provided
            if (config.env() != null && !config.env().isEmpty()) {
                paramsBuilder.env(config.env());
            }
            
            ServerParameters params = paramsBuilder.build();
            
            // Create STDIO transport
            StdioClientTransport transport = new StdioClientTransport(params);
            
            // Create MCP client
            McpSyncClient client = McpClient.sync(transport)
                .requestTimeout(DEFAULT_TIMEOUT)
                .build();
            
            logger.info("Successfully created MCP client for server: {}", serverName);
            return client;
            
        } catch (Exception e) {
            logger.error("Failed to create MCP client for server: {}", serverName, e);
            throw new RuntimeException("Failed to create MCP client for server: " + serverName, e);
        }
    }
}