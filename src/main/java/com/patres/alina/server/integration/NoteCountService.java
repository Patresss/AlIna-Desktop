package com.patres.alina.server.integration;

import com.patres.alina.common.settings.WorkspaceSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Counts {@code .md} notes in the configured Obsidian vault.
 * <p>
 * The count is intentionally lightweight — it only counts files without
 * reading contents, so even large vaults complete in milliseconds.
 */
public final class NoteCountService {

    private static final Logger logger = LoggerFactory.getLogger(NoteCountService.class);

    private NoteCountService() {
    }

    /**
     * Returns the total number of {@code .md} notes in the Obsidian vault.
     */
    public static long countAllNotes(final WorkspaceSettings settings) {
        return ObsidianCliService.countNotes(
                settings.obsidianCliPath(),
                settings.obsidianVaultName(),
                settings.obsidianExcludePatterns()
        );
    }
}
