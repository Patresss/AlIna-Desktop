package com.patres.alina.server.integration;

/**
 * Represents a single recently edited Obsidian note.
 *
 * @param path     the vault-relative file path (e.g. "Notes/My Note.md")
 * @param name     the display name derived from the path (e.g. "My Note")
 * @param folder   the folder portion of the path (e.g. "Notes") or empty if root
 */
public record ObsidianNote(
        String path,
        String name,
        String folder
) {
}
