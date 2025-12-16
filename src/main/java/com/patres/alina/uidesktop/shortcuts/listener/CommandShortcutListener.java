package com.patres.alina.uidesktop.shortcuts.listener;

import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.common.event.CommandUpdateEvent;
import com.patres.alina.uidesktop.shortcuts.CommandExecutor;
import com.patres.alina.uidesktop.shortcuts.GlobalKeyManager;
import com.patres.alina.uidesktop.shortcuts.ShortcutAction;
import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.listner.Listener;
import com.patres.alina.common.event.bus.DefaultEventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Registers command shortcuts with the shared GlobalKeyManager.
 */
public class CommandShortcutListener extends Listener {

    private static final Logger logger = LoggerFactory.getLogger(CommandShortcutListener.class);

    private final CommandExecutor commandExecutor;
    private final GlobalKeyManager keyManager;
    private final List<ShortcutAction> registeredShortcuts = new ArrayList<>();

    public static void init(ApplicationWindow applicationWindow) {
        new CommandShortcutListener(applicationWindow).registerShortcuts();
    }

    private CommandShortcutListener(ApplicationWindow applicationWindow) {
        this.commandExecutor = new CommandExecutor(applicationWindow);
        this.keyManager = GlobalKeyManager.getInstance();
        DefaultEventBus.getInstance().subscribe(CommandUpdateEvent.class, _ -> registerShortcuts());
    }

    private void registerShortcuts() {
        keyManager.unregisterShortcuts(registeredShortcuts);
        registeredShortcuts.clear();

        BackendApi.getEnabledCommands().stream()
                .forEach(command -> {
                    registerShortcut(command, command.copyAndPasteShortcut(), "copy & paste", () -> commandExecutor.executeWithSelectedText(command));
                    registerDisplayShortcut(command);
                });
    }

    private void registerDisplayShortcut(Command command) {
        final ShortcutKeys displayShortcut = command.displayShortcut();
        if (displayShortcut == null || displayShortcut.getAllKeys().isEmpty()) {
            return;
        }

        final Set<KeyboardKey> displayKeys = displayShortcut.getAllKeys();
        final Set<KeyboardKey> pasteKeys = command.copyAndPasteShortcut() != null ? command.copyAndPasteShortcut().getAllKeys() : Set.of();

        if (!pasteKeys.isEmpty() && pasteKeys.equals(displayKeys)) {
            logger.warn("Display shortcut {} for command '{}' matches copy & paste shortcut, skipping duplicate registration", displayKeys, command.name());
            return;
        }

        registerShortcut(command, displayShortcut, "copy & display", () -> commandExecutor.executeWithSelectedTextAndDisplay(command));
    }

    private void registerShortcut(Command command, ShortcutKeys shortcut, String shortcutLabel, Runnable action) {
        if (shortcut == null || shortcut.getAllKeys().isEmpty()) {
            return;
        }
        final Set<KeyboardKey> keys = shortcut.getAllKeys();
        logger.info("Registering {} shortcut {} for command '{}'", shortcutLabel, keys, command.name());
        ShortcutAction shortcutAction = new ShortcutAction(keys, action);
        keyManager.registerShortcut(shortcutAction);
        registeredShortcuts.add(shortcutAction);
    }
}
