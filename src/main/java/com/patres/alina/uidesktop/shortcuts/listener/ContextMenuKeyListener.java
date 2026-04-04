package com.patres.alina.uidesktop.shortcuts.listener;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent;
import com.patres.alina.uidesktop.shortcuts.GlobalKeyManager;
import com.patres.alina.uidesktop.shortcuts.ShortcutAction;
import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;
import com.patres.alina.uidesktop.ui.contextmenu.AppGlobalContextMenu;
import com.patres.alina.uidesktop.ui.listner.Listener;
import com.patres.alina.uidesktop.ui.util.MacTextAccessor;
import com.patres.alina.uidesktop.ui.util.MacTextAccessor.CapturedContext;
import com.patres.alina.uidesktop.ui.util.OsUtils;
import com.patres.alina.uidesktop.ui.util.SystemClipboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;

/**
 * Registers context menu shortcut handling with the shared GlobalKeyManager.
 * Shows the context menu immediately when the shortcut is pressed, while
 * text capture runs in the background via the Accessibility API.
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
            keyManager.registerShortcut(new ShortcutAction(currentShortcut, this::onContextMenuShortcutTriggered));
        }

        keyManager.registerShortcut(new ShortcutAction(KeyboardKey.ESCAPE, contextMenu::close));
    }

    /**
     * Called when the context menu shortcut is pressed.
     * Shows the menu immediately while text capture runs in the background.
     * By the time the user clicks a command (~200ms+), the capture is already done.
     */
    private void onContextMenuShortcutTriggered() {
        CompletableFuture<CapturedContext> capture = CompletableFuture.supplyAsync(this::captureContext);
        contextMenu.displayWithPendingCapture(capture);
    }

    private CapturedContext captureContext() {
        CapturedContext context = OsUtils.isMacOS()
                ? MacTextAccessor.captureContext()
                : new CapturedContext(SystemClipboard.copySelectedValue(), null);
        logger.info("Context captured: hasText={}, sourceApp='{}'", context.hasText(), context.sourceAppName());
        return context;
    }
}
