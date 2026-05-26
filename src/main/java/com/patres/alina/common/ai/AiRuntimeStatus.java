package com.patres.alina.common.ai;

public record AiRuntimeStatus(
        AiProvider provider,
        String displayName,
        String command,
        String hostname,
        int port,
        String baseUrl,
        String workingDirectory,
        boolean workingDirectoryExists,
        boolean processRunning,
        boolean healthy,
        String version,
        String statusMessage
) {
}
