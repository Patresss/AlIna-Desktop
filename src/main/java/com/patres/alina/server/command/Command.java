package com.patres.alina.server.command;

import com.patres.alina.common.card.State;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;

import java.text.Normalizer;

public record Command(
        String id,
        String name,
        String description,
        String systemPrompt,
        String icon,
        String model,
        State state,
        ShortcutKeys copyAndPasteShortcut,
        ShortcutKeys displayShortcut,
        CommandVisibility visibility
) {

    public Command {
        icon = icon != null && !icon.isBlank() ? icon : "bi-slash";
        model = model != null && !model.isBlank() ? model.trim() : null;
        state = state == null ? State.ENABLED : state;
        copyAndPasteShortcut = copyAndPasteShortcut == null ? new ShortcutKeys() : copyAndPasteShortcut;
        displayShortcut = displayShortcut == null ? new ShortcutKeys() : displayShortcut;
        visibility = CommandVisibility.defaults(visibility);
    }

    public Command(String name,
                   String description,
                   String systemPrompt,
                   String icon) {
        this(
                generateIdFromName(name),
                name,
                description,
                systemPrompt,
                icon,
                null,
                State.ENABLED,
                new ShortcutKeys(),
                new ShortcutKeys(),
                new CommandVisibility()
        );
    }

    public Command(String name,
                   String description,
                   String systemPrompt,
                   String icon,
                   ShortcutKeys copyAndPasteShortcut,
                   ShortcutKeys displayShortcut,
                   CommandVisibility visibility) {
        this(
                name,
                description,
                systemPrompt,
                icon,
                null,
                copyAndPasteShortcut,
                displayShortcut,
                visibility
        );
    }

    public Command(String name,
                   String description,
                   String systemPrompt,
                   String icon,
                   String model,
                   ShortcutKeys copyAndPasteShortcut,
                   ShortcutKeys displayShortcut,
                   CommandVisibility visibility) {
        this(
                generateIdFromName(name),
                name,
                description,
                systemPrompt,
                icon,
                model,
                State.ENABLED,
                copyAndPasteShortcut,
                displayShortcut,
                visibility
        );
    }

    public Command(String id,
                   String name,
                   String description,
                   String systemPrompt,
                   String icon,
                   State state,
                   ShortcutKeys copyAndPasteShortcut,
                   ShortcutKeys displayShortcut,
                   CommandVisibility visibility) {
        this(
                id,
                name,
                description,
                systemPrompt,
                icon,
                null,
                state,
                copyAndPasteShortcut,
                displayShortcut,
                visibility
        );
    }

    public static String generateIdFromName(final String name) {
        if (name == null || name.isBlank()) {
            return "unnamed-command";
        }
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                  .replaceAll("\\p{M}+", "")
                  .replace("ł", "l")
                  .replace("Ł", "L")
                  .toLowerCase()
                  .replaceAll("[^a-z0-9\\-_]", "-")
                  .replaceAll("-+", "-")
                  .replaceAll("^-|-$", "");
    }
}
