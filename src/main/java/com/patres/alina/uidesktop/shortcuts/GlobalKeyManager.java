package com.patres.alina.uidesktop.shortcuts;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeInputEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.ui.listner.Listener;
import com.patres.alina.uidesktop.ui.util.OsUtils;
import com.tulskiy.keymaster.common.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.patres.alina.uidesktop.shortcuts.key.KeyboardKey.getKeyEventAsKeyboardKey;

/**
 * Tracks pressed keys globally and dispatches registered shortcut actions.
 * On macOS, also registers shortcuts via Carbon API (RegisterEventHotKey)
 * to capture synthetic key events from tools like Logitech Options.
 */
public final class GlobalKeyManager extends Listener implements NativeKeyListener {

    private static final Logger logger = LoggerFactory.getLogger(GlobalKeyManager.class);
    private static final GlobalKeyManager INSTANCE = new GlobalKeyManager();
    private static final long DEBOUNCE_MS = 300;

    private final Set<KeyboardKey> pressedKeys = new HashSet<>();
    private final List<ShortcutAction> shortcutActions = new ArrayList<>();
    private final Set<Set<KeyboardKey>> triggeredCombos = new HashSet<>();
    private final Map<Set<KeyboardKey>, Long> lastTriggerTime = new ConcurrentHashMap<>();

    private Provider carbonProvider;

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
        initCarbonProvider();
    }

    private void initCarbonProvider() {
        if (!OsUtils.isMacOS()) return;
        try {
            carbonProvider = Provider.getCurrentProvider(false);
            logger.info("Carbon hotkey provider initialized for macOS");
        } catch (Throwable e) {
            logger.warn("Carbon hotkey provider unavailable: {}", e.getMessage());
            carbonProvider = null;
        }
    }

    public void registerShortcut(ShortcutAction shortcutAction) {
        if (shortcutAction.isValid()) {
            shortcutActions.add(shortcutAction);
            registerCarbonHotkey(shortcutAction);
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
        reRegisterAllCarbonHotkeys();
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent keyEvent) {
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent keyEvent) {
        syncModifiersFromEvent(keyEvent);
        getKeyEventAsKeyboardKey(keyEvent).ifPresent(key -> {
            pressedKeys.add(key);
            dispatchHandlers();
        });
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent keyEvent) {
        syncModifiersFromEvent(keyEvent);
        getKeyEventAsKeyboardKey(keyEvent).ifPresent(key -> {
            pressedKeys.remove(key);
            triggeredCombos.removeIf(combo -> combo.contains(key));
        });
    }

    private void syncModifiersFromEvent(NativeKeyEvent keyEvent) {
        int modifiers = keyEvent.getModifiers();
        syncModifier(modifiers, NativeInputEvent.SHIFT_L_MASK | NativeInputEvent.SHIFT_R_MASK, KeyboardKey.SHIFT);
        syncModifier(modifiers, NativeInputEvent.CTRL_L_MASK | NativeInputEvent.CTRL_R_MASK, KeyboardKey.CONTROL);
        syncModifier(modifiers, NativeInputEvent.META_L_MASK | NativeInputEvent.META_R_MASK, KeyboardKey.META);
        syncModifier(modifiers, NativeInputEvent.ALT_L_MASK | NativeInputEvent.ALT_R_MASK, KeyboardKey.ALT);
    }

    private void syncModifier(int modifiers, int mask, KeyboardKey key) {
        if ((modifiers & mask) != 0) {
            pressedKeys.add(key);
        } else {
            pressedKeys.remove(key);
        }
    }

    private void dispatchHandlers() {
        for (ShortcutAction shortcut : shortcutActions) {
            if (pressedKeys.containsAll(shortcut.keys()) && triggeredCombos.add(shortcut.keys())) {
                runActionDebounced(shortcut.keys(), shortcut.action());
            }
        }
    }

    private void runActionDebounced(Set<KeyboardKey> keys, Runnable action) {
        long now = System.currentTimeMillis();
        Long prev = lastTriggerTime.put(keys, now);
        if (prev != null && now - prev < DEBOUNCE_MS) {
            return;
        }
        runAction(action);
    }

    private void runAction(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            logger.error("Error executing shortcut action", e);
        }
    }

    // ── Carbon hotkey support (macOS) ──────────────────────────────

    private void registerCarbonHotkey(ShortcutAction shortcutAction) {
        if (carbonProvider == null) return;
        if (!hasModifier(shortcutAction.keys())) return;

        KeyStroke keyStroke = toKeyStroke(shortcutAction.keys());
        if (keyStroke == null) return;

        try {
            carbonProvider.register(keyStroke, hotKey ->
                    runActionDebounced(shortcutAction.keys(), shortcutAction.action()));
            logger.info("Registered Carbon hotkey: {}", keyStroke);
        } catch (Exception e) {
            logger.warn("Failed to register Carbon hotkey {}: {}", keyStroke, e.getMessage());
        }
    }

    private void reRegisterAllCarbonHotkeys() {
        if (carbonProvider == null) return;
        try {
            carbonProvider.reset();
            for (ShortcutAction action : shortcutActions) {
                registerCarbonHotkey(action);
            }
        } catch (Exception e) {
            logger.warn("Failed to re-register Carbon hotkeys: {}", e.getMessage());
        }
    }

    private static boolean hasModifier(Set<KeyboardKey> keys) {
        return keys.stream().anyMatch(k ->
                k == KeyboardKey.SHIFT || k == KeyboardKey.CONTROL ||
                        k == KeyboardKey.ALT || k == KeyboardKey.META || k == KeyboardKey.WINDOWS);
    }

    private static KeyStroke toKeyStroke(Set<KeyboardKey> keys) {
        int modifiers = 0;
        int keyCode = -1;
        for (KeyboardKey key : keys) {
            switch (key) {
                case SHIFT -> modifiers |= InputEvent.SHIFT_DOWN_MASK;
                case CONTROL -> modifiers |= InputEvent.CTRL_DOWN_MASK;
                case ALT -> modifiers |= InputEvent.ALT_DOWN_MASK;
                case META, WINDOWS -> modifiers |= InputEvent.META_DOWN_MASK;
                default -> keyCode = key.getKeyValue();
            }
        }
        if (keyCode == -1) return null;
        return KeyStroke.getKeyStroke(keyCode, modifiers);
    }

}
