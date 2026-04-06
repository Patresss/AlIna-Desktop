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
        ShortcutKeys pasteShortcut,
        ShortcutKeys displayShortcut,
        ShortcutKeys executeShortcut,
        CommandVisibility visibility
) {

    public Command {
        icon = icon != null && !icon.isBlank() ? icon : "bi-slash";
        model = model != null && !model.isBlank() ? model.trim() : null;
        state = state == null ? State.ENABLED : state;
        pasteShortcut = pasteShortcut == null ? new ShortcutKeys() : pasteShortcut;
        displayShortcut = displayShortcut == null ? new ShortcutKeys() : displayShortcut;
        executeShortcut = executeShortcut == null ? new ShortcutKeys() : executeShortcut;
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
                new ShortcutKeys(),
                new CommandVisibility()
        );
    }

    public Command(String name,
                   String description,
                   String systemPrompt,
                   String icon,
                   ShortcutKeys pasteShortcut,
                   ShortcutKeys displayShortcut,
                   ShortcutKeys executeShortcut,
                   CommandVisibility visibility) {
        this(
                name,
                description,
                systemPrompt,
                icon,
                null,
                pasteShortcut,
                displayShortcut,
                executeShortcut,
                visibility
        );
    }

    public Command(String name,
                   String description,
                   String systemPrompt,
                   String icon,
                   String model,
                   ShortcutKeys pasteShortcut,
                   ShortcutKeys displayShortcut,
                   ShortcutKeys executeShortcut,
                   CommandVisibility visibility) {
        this(
                generateIdFromName(name),
                name,
                description,
                systemPrompt,
                icon,
                model,
                State.ENABLED,
                pasteShortcut,
                displayShortcut,
                executeShortcut,
                visibility
        );
    }

    public Command(String id,
                   String name,
                   String description,
                   String systemPrompt,
                   String icon,
                   State state,
                   ShortcutKeys pasteShortcut,
                   ShortcutKeys displayShortcut,
                   ShortcutKeys executeShortcut,
                   CommandVisibility visibility) {
        this(
                id,
                name,
                description,
                systemPrompt,
                icon,
                null,
                state,
                pasteShortcut,
                displayShortcut,
                executeShortcut,
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
