package com.patres.alina.common.agent;

public record AgentRuntimeStatus(
        AgentBackend backend,
        String displayName,
        String transport,
        String hostname,
        int port,
        String baseUrl,
        String command,
        String workingDirectory,
        boolean workingDirectoryExists,
        boolean processRunning,
        boolean healthy,
        String version,
        String statusMessage
) {
}
