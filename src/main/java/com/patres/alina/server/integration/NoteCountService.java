package com.patres.alina.server.integration;

import com.patres.alina.common.settings.WorkspaceSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Counts {@code .md} notes in the configured working directory.
 * <p>
 * The count is intentionally lightweight — it only counts files without
 * reading contents, so even large directories complete in milliseconds.
 */
public final class NoteCountService {

    private static final Logger logger = LoggerFactory.getLogger(NoteCountService.class);

    private NoteCountService() {
    }

    /**
     * Returns the total number of {@code .md} notes in the working directory.
     */
    public static long countAllNotes(final WorkspaceSettings settings) {
        final Path notesDirectory = Path.of(settings.openCodeWorkingDirectory()).toAbsolutePath().normalize();
        return ObsidianCliService.countNotes(
                notesDirectory,
                settings.obsidianExcludePatterns()
        );
    }
}
