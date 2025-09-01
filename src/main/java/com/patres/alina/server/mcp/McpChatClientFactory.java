package com.patres.alina.server.mcp;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

@Component
public class McpChatClientFactory {

    private final McpToolIntegrationService mcpToolIntegrationService;

    public McpChatClientFactory(McpToolIntegrationService mcpToolIntegrationService) {
        this.mcpToolIntegrationService = mcpToolIntegrationService;
    }

    public ChatClient createFor(ChatModel chatModel) {
        return ChatClient
                .builder(chatModel)
                .defaultToolCallbacks(mcpToolIntegrationService.getToolCallbacks())
                .build();
    }
}

