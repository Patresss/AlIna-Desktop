package com.patres.alina.uidesktop.shortcuts;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.ui.listner.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.patres.alina.uidesktop.shortcuts.key.KeyboardKey.getKeyEventAsKeyboardKey;

/**
 * Tracks pressed keys globally and dispatches registered shortcut actions.
 */
public final class GlobalKeyManager extends Listener implements NativeKeyListener {

    private static final Logger logger = LoggerFactory.getLogger(GlobalKeyManager.class);
    private static final GlobalKeyManager INSTANCE = new GlobalKeyManager();

    private final Set<KeyboardKey> pressedKeys = new HashSet<>();
    private final List<ShortcutAction> shortcutActions = new ArrayList<>();
    private final Set<Set<KeyboardKey>> triggeredCombos = new HashSet<>();

    public static GlobalKeyManager getInstance() {
        return INSTANCE;
    }

    private GlobalKeyManager() {
        if (!Listener.isNativeHookAvailable()) {
            logger.warn("Native hook not available - global shortcuts will be disabled");
            return;
        }
        try {
            GlobalScreen.addNativeKeyListener(this);
            logger.info("GlobalKeyManager registered as native key listener");
        } catch (Exception e) {
            logger.error("Unable to register GlobalKeyManager as native key listener", e);
        }
    }

    public void registerShortcut(ShortcutAction shortcutAction) {
        if (shortcutAction.isValid()) {
            shortcutActions.add(shortcutAction);
        }
    }

    public void unregisterShortcuts(Collection<ShortcutAction> shortcuts) {
        if (shortcuts == null || shortcuts.isEmpty()) {
            return;
        }
        shortcutActions.removeAll(shortcuts);
        shortcuts.stream()
                .map(ShortcutAction::keys)
                .forEach(triggeredCombos::remove);
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent keyEvent) {
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent keyEvent) {
        getKeyEventAsKeyboardKey(keyEvent).ifPresent(key -> {
            pressedKeys.add(key);
            dispatchHandlers();
        });
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent keyEvent) {
        getKeyEventAsKeyboardKey(keyEvent).ifPresent(key -> {
            pressedKeys.remove(key);
            triggeredCombos.removeIf(combo -> combo.contains(key));
        });
    }

    private void dispatchHandlers() {
        for (ShortcutAction shortcut : shortcutActions) {
            if (pressedKeys.containsAll(shortcut.keys()) && triggeredCombos.add(shortcut.keys())) {
                runAction(shortcut.action());
            }
        }
    }

    private void runAction(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            logger.error("Error executing shortcut action", e);
        }
    }

}
