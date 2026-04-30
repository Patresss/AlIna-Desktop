package com.patres.alina.server.integration;

import java.util.Collections;
import java.util.List;

/**
 * Result of fetching recent Obsidian notes via the CLI.
 *
 * @param notes        list of recently edited notes
 * @param errorMessage error description (empty if successful)
 * @param cliMissing   true when the obsidian-cli executable is not found
 */
public record ObsidianNotesResult(
        List<ObsidianNote> notes,
        String errorMessage,
        boolean cliMissing
) {

    public static ObsidianNotesResult success(final List<ObsidianNote> notes) {
        return new ObsidianNotesResult(notes, "", false);
    }

    public static ObsidianNotesResult error(final String message) {
        return new ObsidianNotesResult(Collections.emptyList(), message, false);
    }

    public static ObsidianNotesResult cliNotFound() {
        return new ObsidianNotesResult(Collections.emptyList(),
                "Obsidian CLI not found. Make sure obsidian-cli is installed and available in PATH.",
                true);
    }
}
