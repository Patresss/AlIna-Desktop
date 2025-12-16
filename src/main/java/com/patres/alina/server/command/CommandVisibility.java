package com.patres.alina.server.command;

/**
 * Controls where a command is exposed across the UI entry points.
 */
public record CommandVisibility(
        boolean showInChat,
        boolean showInContextMenuPaste,
        boolean showInContextMenuDisplay
) {

    public CommandVisibility() {
        this(true, true, true);
    }

    public static CommandVisibility defaults(CommandVisibility visibility) {
        return visibility == null ? new CommandVisibility() : visibility;
    }
}
