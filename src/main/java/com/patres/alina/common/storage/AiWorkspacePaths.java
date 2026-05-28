package com.patres.alina.common.storage;

import java.nio.file.Path;

public final class AiWorkspacePaths {

    public static final String AGENTS_DIRECTORY = ".agents";
    public static final String OPENCODE_DIRECTORY = ".opencode";

    private AiWorkspacePaths() {
    }

    public static Path agentsDirectory(final Path workspaceDirectory) {
        return workspaceDirectory.toAbsolutePath()
                .normalize()
                .resolve(AGENTS_DIRECTORY)
                .normalize();
    }

    public static Path openCodeDirectory(final Path workspaceDirectory) {
        return workspaceDirectory.toAbsolutePath()
                .normalize()
                .resolve(OPENCODE_DIRECTORY)
                .normalize();
    }
}
