package com.patres.alina.uidesktop.shortcuts.listener;

import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.shortcuts.CommandExecutor;
import com.patres.alina.uidesktop.shortcuts.GlobalKeyManager;
import com.patres.alina.uidesktop.shortcuts.ShortcutAction;
import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.listner.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Registers command shortcuts with the shared GlobalKeyManager.
 */
public class CommandShortcutListener extends Listener {

    private static final Logger logger = LoggerFactory.getLogger(CommandShortcutListener.class);

    private final CommandExecutor commandExecutor;
    private final GlobalKeyManager keyManager;

    public static void init(ApplicationWindow applicationWindow) {
        new CommandShortcutListener(applicationWindow).registerShortcuts();
    }

    private CommandShortcutListener(ApplicationWindow applicationWindow) {
        this.commandExecutor = new CommandExecutor(applicationWindow);
        this.keyManager = GlobalKeyManager.getInstance();
    }

    private void registerShortcuts() {
        BackendApi.getEnabledCommands().stream()
                .filter(command -> command.globalShortcut() != null && !command.globalShortcut().getAllKeys().isEmpty())
                .forEach(command -> {
                    final Set<KeyboardKey> keys = command.globalShortcut().getAllKeys();
                    logger.info("Registering global shortcut {} for command '{}'", keys, command.name());
                    keyManager.registerShortcut(new ShortcutAction(keys, () -> commandExecutor.executeWithSelectedText(command)));
                });
    }
}
