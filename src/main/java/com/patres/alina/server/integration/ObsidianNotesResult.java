package com.patres.alina.server.integration;

import java.util.Collections;
import java.util.List;

/**
 * Result of fetching recent notes from a directory.
 *
 * @param notes        list of recently edited notes
 * @param errorMessage error description (empty if successful)
 */
public record ObsidianNotesResult(
        List<ObsidianNote> notes,
        String errorMessage
) {

    public static ObsidianNotesResult success(final List<ObsidianNote> notes) {
        return new ObsidianNotesResult(notes, "");
    }

    public static ObsidianNotesResult error(final String message) {
        return new ObsidianNotesResult(Collections.emptyList(), message);
    }
}
