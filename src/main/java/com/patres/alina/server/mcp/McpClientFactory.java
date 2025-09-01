package com.patres.alina.server.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class McpClientFactory {

    private static final Logger logger = LoggerFactory.getLogger(McpClientFactory.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    public McpSyncClient createClient(final String serverName, final McpServerConfiguration config) {
        logger.info("Creating MCP client for server: {}", serverName);
        try {
            final ServerParameters params = buildServerParameters(config);
            final StdioClientTransport transport = new StdioClientTransport(params);
            return buildSyncClient(transport);
        } catch (final Exception e) {
            logger.error("Failed to create MCP client for server: {}", serverName, e);
            throw new RuntimeException("Failed to create MCP client for server: " + serverName, e);
        }
    }

    private ServerParameters buildServerParameters(final McpServerConfiguration config) {
        final ServerParameters.Builder builder = ServerParameters.builder(config.command());

        final List<String> args = config.args();
        if (args != null && !args.isEmpty()) {
            builder.args(args);
        }

        final Map<String, String> env = config.env();
        if (env != null && !env.isEmpty()) {
            builder.env(env);
        }

        return builder.build();
    }

    private McpSyncClient buildSyncClient(final StdioClientTransport transport) {
        return McpClient.sync(transport)
                .requestTimeout(DEFAULT_TIMEOUT)
                .build();
    }
}
