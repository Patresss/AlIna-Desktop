package com.patres.alina.server.mcp;

import io.modelcontextprotocol.client.McpSyncClient;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

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
    public McpClientManager(final McpClientFactory clientFactory,
                            final McpConfigurationService configurationService) {
        this.clientFactory = clientFactory;
        this.configurationService = configurationService;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing MCP client manager");
        final McpServersConfig config = configurationService.loadConfiguration();
        initializeClients(config);
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down MCP client manager");
        shutdownClients();
    }

    public synchronized void initializeClients(final McpServersConfig config) {
        logger.info("Initializing MCP clients for {} servers", config.mcpServers().size());

        shutdownClients();

        config.mcpServers().forEach(this::initializeClientForServer);

        logger.info("MCP client initialization complete. Active clients: {}", activeClients.size());
    }

    private void initializeClientForServer(final String serverName, final McpServerConfiguration serverConfig) {
        try {
            logger.info("Creating MCP client for server: {}", serverName);
            final McpSyncClient client = clientFactory.createClient(serverName, serverConfig);
            tryInitializeClient(serverName, client);
        } catch (final Exception e) {
            logger.error("Failed to create MCP client for server: {}", serverName, e);
        }
    }

    private void tryInitializeClient(final String serverName, final McpSyncClient client) {
        try {
            client.initialize();
            activeClients.put(serverName, client);
            logger.info("Successfully initialized MCP client: {}", serverName);
        } catch (final Exception initException) {
            logger.warn("Failed to initialize MCP client for server: {} - {}",
                    serverName, initException.getMessage());
            logger.debug("Full initialization error for server: {}", serverName, initException);
            safeClose(serverName, client);
        }
    }

    public synchronized void shutdownClients() {
        logger.info("Shutting down {} active MCP clients", activeClients.size());
        activeClients.forEach(this::safeClose);
        activeClients.clear();
        logger.info("All MCP clients shut down");
    }

    private void safeClose(final String serverName, final McpSyncClient client) {
        try {
            logger.debug("Closing MCP client: {}", serverName);
            client.close();
            logger.debug("Successfully closed MCP client: {}", serverName);
        } catch (final Exception e) {
            logger.error("Error closing MCP client: {}", serverName, e);
        }
    }

    public List<McpSyncClient> getActiveClients() {
        return new ArrayList<>(activeClients.values());
    }

    public McpSyncClient getClient(final String serverName) {
        return activeClients.get(serverName);
    }

    public boolean isClientActive(final String serverName) {
        return activeClients.containsKey(serverName);
    }

    public int getActiveClientCount() {
        return activeClients.size();
    }

    @EventListener
    public void onConfigurationChange(final McpConfigurationService.McpConfigurationChangedEvent event) {
        logger.info("MCP configuration changed, reinitializing clients");
        initializeClients(event.getConfiguration());
    }
}
