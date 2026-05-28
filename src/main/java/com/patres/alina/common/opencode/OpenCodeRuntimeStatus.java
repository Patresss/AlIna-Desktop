package com.patres.alina.common.opencode;

public record OpenCodeRuntimeStatus(
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
