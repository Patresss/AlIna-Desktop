package com.patres.alina.server.command;

import com.patres.alina.common.card.State;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;

public record Command(
        String id,
        String name,
        String description,
        String systemPrompt,
        String icon,
        State state,
        ShortcutKeys globalShortcut,
        ShortcutKeys displayShortcut,
        CommandVisibility visibility
) {

    public Command {
        icon = icon != null && !icon.isBlank() ? icon : "bi-slash";
        state = state == null ? State.ENABLED : state;
        globalShortcut = globalShortcut == null ? new ShortcutKeys() : globalShortcut;
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
                   ShortcutKeys globalShortcut,
                   ShortcutKeys displayShortcut,
                   CommandVisibility visibility) {
        this(
                generateIdFromName(name),
                name,
                description,
                systemPrompt,
                icon,
                State.ENABLED,
                globalShortcut,
                displayShortcut,
                visibility
        );
    }

    public static String generateIdFromName(final String name) {
        if (name == null || name.isBlank()) {
            return "unnamed-command";
        }
        return name.toLowerCase()
                  .replaceAll("[^a-z0-9\\-_]", "-")
                  .replaceAll("-+", "-")
                  .replaceAll("^-|-$", "");
    }
}
