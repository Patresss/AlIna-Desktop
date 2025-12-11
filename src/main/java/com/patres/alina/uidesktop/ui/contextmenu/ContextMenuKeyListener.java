package com.patres.alina.uidesktop.ui.contextmenu;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent;
import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;
import com.patres.alina.uidesktop.ui.listner.Listener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;
import static com.patres.alina.uidesktop.shortcuts.key.KeyboardKey.getKeyEvent;
import static com.patres.alina.uidesktop.shortcuts.key.KeyboardKey.getKeyEventAsKeyboardKey;


public class ContextMenuKeyListener extends Listener implements NativeKeyListener {

    private final Set<KeyboardKey> pressedKeys;
    private final AppGlobalContextMenu contextMenu;
    private ShortcutKeys contextMenuShortcutKeys;

    public static void init(AppGlobalContextMenu contextMenu) {
        final ContextMenuKeyListener contextMenuKeyListener = new ContextMenuKeyListener(contextMenu);
        GlobalScreen.addNativeKeyListener(contextMenuKeyListener);
    }

    private ContextMenuKeyListener(AppGlobalContextMenu contextMenu) {
        this.contextMenu = contextMenu;
        this.pressedKeys = new HashSet<>();
        this.contextMenuShortcutKeys = UI_SETTINGS.getSettings().shortcutKeysSettings().contextMenuShortcutKeys();

        // Subscribe to settings updates
        DefaultEventBus.getInstance().subscribe(
                UiSettingsUpdateEvent.class,
                _ -> updateShortcutKeys()
        );
    }

    private void updateShortcutKeys() {
        contextMenuShortcutKeys = UI_SETTINGS.getSettings().shortcutKeysSettings().contextMenuShortcutKeys();
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent keyEvent) {
    }


    @Override
    public void nativeKeyPressed(NativeKeyEvent keyEvent) {
        if (KeyboardKey.ESCAPE.getKeyValue().equals(getKeyEvent(keyEvent))) {
            contextMenu.close();
        } else {
            getKeyEventAsKeyboardKey(keyEvent).ifPresent(pressedKeys::add);
            checkKeys();
        }
    }


    private void checkKeys() {
        if (!contextMenuShortcutKeys.getAllKeys().isEmpty() && pressedKeys.equals(contextMenuShortcutKeys.getAllKeys())) {
            contextMenu.displayContextMenu();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent keyEvent) {
        if (List.of(KeyboardKey.CONTROL.getKeyValue(), KeyboardKey.ALT.getKeyValue(), KeyboardKey.META.getKeyValue()).contains(getKeyEvent(keyEvent))) {
            pressedKeys.clear();
        } else {
            getKeyEventAsKeyboardKey(keyEvent).ifPresent(pressedKeys::remove);
        }
    }

}
