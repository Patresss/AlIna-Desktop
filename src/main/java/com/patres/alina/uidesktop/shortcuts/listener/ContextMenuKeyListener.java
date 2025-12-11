package com.patres.alina.uidesktop.shortcuts.listener;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent;
import com.patres.alina.uidesktop.shortcuts.GlobalKeyManager;
import com.patres.alina.uidesktop.shortcuts.ShortcutAction;
import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;
import com.patres.alina.uidesktop.ui.contextmenu.AppGlobalContextMenu;
import com.patres.alina.uidesktop.ui.listner.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;

/**
 * Registers context menu shortcut handling with the shared GlobalKeyManager.
 */
public class ContextMenuKeyListener extends Listener {

    private static final Logger logger = LoggerFactory.getLogger(ContextMenuKeyListener.class);

    private final AppGlobalContextMenu contextMenu;
    private final GlobalKeyManager keyManager;

    private ContextMenuKeyListener(AppGlobalContextMenu contextMenu) {
        this.contextMenu = contextMenu;
        this.keyManager = GlobalKeyManager.getInstance();

        registerShortcuts();
        DefaultEventBus.getInstance().subscribe(
                UiSettingsUpdateEvent.class,
                _ -> registerShortcuts()
        );
    }

    public static void init(AppGlobalContextMenu contextMenu) {
        new ContextMenuKeyListener(contextMenu);
    }

    private void registerShortcuts() {
        ShortcutKeys contextMenuShortcutKeys = UI_SETTINGS.getSettings().shortcutKeysSettings().contextMenuShortcutKeys();
        logger.info("Registering context menu shortcut: {}", contextMenuShortcutKeys.getAllKeys());


        Set<KeyboardKey> currentShortcut = contextMenuShortcutKeys.getAllKeys();
        if (!currentShortcut.isEmpty()) {
            keyManager.registerShortcut(new ShortcutAction(currentShortcut, contextMenu::displayContextMenu));
        }

        keyManager.registerShortcut(new ShortcutAction(KeyboardKey.ESCAPE, contextMenu::close));
    }
}
