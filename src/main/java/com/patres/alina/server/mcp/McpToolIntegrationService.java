package com.patres.alina.server.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.modelcontextprotocol.client.McpSyncClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class McpToolIntegrationService implements ToolCallbackProvider {

    private static final Logger logger = LoggerFactory.getLogger(McpToolIntegrationService.class);
    
    private final McpClientManager clientManager;
    private final List<ToolCallback> toolCallbacks = new CopyOnWriteArrayList<>();
    private final List<McpToolDescriptor> toolDescriptors = new CopyOnWriteArrayList<>();

    @Autowired
    public McpToolIntegrationService(final McpClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @PostConstruct
    public void initialize() {
        logger.info("Initializing MCP tool integration service");
        refreshToolCallbacks();
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        logger.debug("Providing {} MCP tool callbacks", toolCallbacks.size());
        return toolCallbacks.toArray(new ToolCallback[0]);
    }

    @EventListener
    public void onConfigurationChange(final McpConfigurationService.McpConfigurationChangedEvent event) {
        logger.info("MCP configuration changed, refreshing tool callbacks");
        refreshToolCallbacks();
    }

    private synchronized void refreshToolCallbacks() {
        logger.info("Refreshing MCP tool callbacks");
        
        // Clear existing callbacks and descriptors
        toolCallbacks.clear();
        toolDescriptors.clear();
        
        // Get all active clients
        final List<McpSyncClient> activeClients = clientManager.getActiveClients();
        logger.info("Discovered {} active MCP clients", activeClients.size());
        activeClients.forEach(this::addToolsFromClient);
        
        logger.info("MCP tool refresh complete. Total tools available: {}", toolCallbacks.size());
        logAvailableToolNames();
    }

    private void addToolsFromClient(final McpSyncClient client) {
        try {
            final SyncMcpToolCallbackProvider provider = new SyncMcpToolCallbackProvider(client);
            final ToolCallback[] clientCallbacks = provider.getToolCallbacks();
            if (clientCallbacks == null || clientCallbacks.length == 0) {
                return;
            }
            for (final ToolCallback callback : clientCallbacks) {
                toolCallbacks.add(callback);
                toolDescriptors.add(new McpToolDescriptor(callback.toString(), "MCP Tool", client.toString()));
                logger.debug("Added MCP tool: {} from client: {}", callback, client);
            }
            logger.info("Added {} tools from MCP client: {}", clientCallbacks.length, client);
        } catch (final Exception e) {
            logger.error("Failed to get tools from MCP client: {}", client, e);
        }
    }

    private void logAvailableToolNames() {
        if (!logger.isInfoEnabled() || toolCallbacks.isEmpty()) {
            return;
        }
        final List<String> toolNames = toolCallbacks.stream().map(ToolCallback::toString).toList();
        logger.info("Available MCP tools: {}", toolNames);
    }

    public List<McpToolDescriptor> getToolDescriptors() {
        return new ArrayList<>(toolDescriptors);
    }

    public int getAvailableToolCount() {
        return toolCallbacks.size();
    }

    /**
     * Descriptor for MCP tool information
     */
    public static class McpToolDescriptor {
        private final String name;
        private final String description;
        private final String clientId;

        public McpToolDescriptor(String name, String description, String clientId) {
            this.name = name;
            this.description = description;
            this.clientId = clientId;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getClientId() {
            return clientId;
        }

        @Override
        public String toString() {
            return "McpToolDescriptor{" +
                   "name='" + name + '\'' +
                   ", description='" + description + '\'' +
                   ", clientId='" + clientId + '\'' +
                   '}';
        }
    }
}
