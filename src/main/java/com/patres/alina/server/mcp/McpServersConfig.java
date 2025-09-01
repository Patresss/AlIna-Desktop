package com.patres.alina.server.mcp;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record McpServersConfig(
        @JsonProperty("mcpServers") Map<String, McpServerConfiguration> mcpServers
) {
    @JsonCreator
    public McpServersConfig(@JsonProperty("mcpServers") Map<String, McpServerConfiguration> mcpServers) {
        this.mcpServers = mcpServers != null ? mcpServers : Map.of();
    }

    public static McpServersConfig empty() {
        return new McpServersConfig(Map.of());
    }
}