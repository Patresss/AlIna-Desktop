package com.patres.alina.uidesktop.ui.contextmenu;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.ui.listner.Listener;
import com.patres.alina.uidesktop.common.settings.GlobalSettingsLoader;

import java.util.ArrayList;
import java.util.List;

import static com.patres.alina.uidesktop.shortcuts.key.KeyboardKey.getKeyEvent;
import static com.patres.alina.uidesktop.shortcuts.key.KeyboardKey.getKeyEventAsKeyboardKey;


public class ContextMenuKeyListener extends Listener implements NativeKeyListener {

    private final List<KeyboardKey> pressedKeys;
    private final AppGlobalContextMenu contextMenu;

    public static void init(AppGlobalContextMenu contextMenu) {
        final ContextMenuKeyListener contextMenuKeyListener = new ContextMenuKeyListener(contextMenu);
        GlobalScreen.addNativeKeyListener(contextMenuKeyListener);
    }

    private ContextMenuKeyListener(AppGlobalContextMenu contextMenu) {
        this.contextMenu = contextMenu;
        this.pressedKeys = new ArrayList<>();
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
        if (pressedKeys.equals(GlobalSettingsLoader.getSettings().getContextMenuKeys())) {
            contextMenu.displayContextMenu();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent keyEvent) {
        if (List.of(KeyboardKey.CONTROL.getKeyValue(), KeyboardKey.ALT.getKeyValue()).contains(getKeyEvent(keyEvent))) {
            pressedKeys.clear();
        } else {
            getKeyEventAsKeyboardKey(keyEvent).ifPresent(pressedKeys::remove);
        }
    }

}
