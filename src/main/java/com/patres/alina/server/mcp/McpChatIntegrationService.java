package com.patres.alina.server.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class McpChatIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(McpChatIntegrationService.class);
    
    private final McpToolIntegrationService toolIntegrationService;

    @Autowired
    public McpChatIntegrationService(final McpToolIntegrationService toolIntegrationService) {
        this.toolIntegrationService = toolIntegrationService;
    }

    /**
     * Enhances chat messages with MCP tool capabilities
     * Adds system message about available tools if any are present
     */
    public List<AbstractMessage> enhanceMessagesWithMcpTools(final List<AbstractMessage> messages) {
        final List<AbstractMessage> enhancedMessages = new ArrayList<>(messages);
        
        final int availableToolCount = toolIntegrationService.getAvailableToolCount();
        if (availableToolCount > 0) {
            logger.debug("Enhancing chat messages with {} MCP tools", availableToolCount);
            
            // Get tool descriptions
            final List<McpToolIntegrationService.McpToolDescriptor> toolDescriptors = 
                toolIntegrationService.getToolDescriptors();
            
            // Create system message describing available tools
            final SystemMessage toolSystemMessage = buildToolsSystemMessage(toolDescriptors);
            final int insertIndex = findInsertIndexForSystemMessage(enhancedMessages);
            enhancedMessages.add(insertIndex, toolSystemMessage);
            
            logger.debug("Added MCP tools system message to chat context");
        } else {
            logger.debug("No MCP tools available, messages unchanged");
        }
        
        return enhancedMessages;
    }

    private SystemMessage buildToolsSystemMessage(final List<McpToolIntegrationService.McpToolDescriptor> toolDescriptors) {
        final StringBuilder toolDescription = new StringBuilder();
        toolDescription.append("You have access to external tools through MCP (Model Context Protocol). ");
        toolDescription.append("Available tools:\n");

        for (final McpToolIntegrationService.McpToolDescriptor descriptor : toolDescriptors) {
            toolDescription.append("- ").append(descriptor.getName())
                    .append(": ").append(descriptor.getDescription())
                    .append("\n");
        }

        toolDescription.append("\nPolicy: When a tool can improve accuracy (e.g., fetching the current time), call it directly without asking for permission. Execute the most relevant tool, incorporate its result, and answer succinctly.");
        return new SystemMessage(toolDescription.toString());
    }

    private int findInsertIndexForSystemMessage(final List<AbstractMessage> messages) {
        int insertIndex = 0;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) instanceof SystemMessage) {
                insertIndex = i + 1;
            } else {
                break;
            }
        }
        return insertIndex;
    }

    /**
     * Checks if MCP tools are available for use
     */
    public boolean hasMcpTools() {
        return toolIntegrationService.getAvailableToolCount() > 0;
    }

    /**
     * Gets the number of available MCP tools
     */
    public int getAvailableToolCount() {
        return toolIntegrationService.getAvailableToolCount();
    }

}
