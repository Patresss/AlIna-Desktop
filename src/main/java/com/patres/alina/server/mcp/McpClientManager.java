package com.patres.alina.server.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class McpClientManager {

    private static final Logger logger = LoggerFactory.getLogger(McpClientManager.class);
    
    private final Map<String, McpSyncClient> activeClients = new ConcurrentHashMap<>();
    private final McpClientFactory clientFactory;
    private final McpConfigurationService configurationService;
    
    @Autowired
    public McpClientManager(McpClientFactory clientFactory, 
                           McpConfigurationService configurationService) {
        this.clientFactory = clientFactory;
        this.configurationService = configurationService;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing MCP client manager");
        McpServersConfig config = configurationService.loadConfiguration();
        initializeClients(config);
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down MCP client manager");
        shutdownClients();
    }

    public synchronized void initializeClients(McpServersConfig config) {
        logger.info("Initializing MCP clients for {} servers", config.mcpServers().size());
        
        // Shutdown existing clients first
        shutdownClients();
        
        // Create new clients
        for (var entry : config.mcpServers().entrySet()) {
            String serverName = entry.getKey();
            McpServerConfiguration serverConfig = entry.getValue();
            
            try {
                logger.info("Creating MCP client for server: {}", serverName);
                McpSyncClient client = clientFactory.createClient(serverName, serverConfig);
                
                // Connect the client with timeout handling
                try {
                    client.initialize();
                    activeClients.put(serverName, client);
                    logger.info("Successfully initialized MCP client: {}", serverName);
                } catch (Exception initException) {
                    logger.warn("Failed to initialize MCP client for server: {} - {}", 
                               serverName, initException.getMessage());
                    logger.debug("Full initialization error for server: {}", serverName, initException);
                    
                    // Try to close the client if it was created but failed to initialize
                    try {
                        client.close();
                    } catch (Exception closeException) {
                        logger.debug("Error closing failed client for server: {}", serverName, closeException);
                    }
                }
                
            } catch (Exception e) {
                logger.error("Failed to create MCP client for server: {}", serverName, e);
                // Continue with other clients even if one fails
            }
        }
        
        logger.info("MCP client initialization complete. Active clients: {}", activeClients.size());
    }

    public synchronized void shutdownClients() {
        logger.info("Shutting down {} active MCP clients", activeClients.size());
        
        for (var entry : activeClients.entrySet()) {
            String serverName = entry.getKey();
            McpSyncClient client = entry.getValue();
            
            try {
                logger.debug("Closing MCP client: {}", serverName);
                client.close();
                logger.debug("Successfully closed MCP client: {}", serverName);
                
            } catch (Exception e) {
                logger.error("Error closing MCP client: {}", serverName, e);
            }
        }
        
        activeClients.clear();
        logger.info("All MCP clients shut down");
    }

    public List<McpSyncClient> getActiveClients() {
        return new ArrayList<>(activeClients.values());
    }

    public McpSyncClient getClient(String serverName) {
        return activeClients.get(serverName);
    }

    public boolean isClientActive(String serverName) {
        return activeClients.containsKey(serverName);
    }

    public int getActiveClientCount() {
        return activeClients.size();
    }

    @EventListener
    public void onConfigurationChange(McpConfigurationService.McpConfigurationChangedEvent event) {
        logger.info("MCP configuration changed, reinitializing clients");
        initializeClients(event.getConfiguration());
    }
}