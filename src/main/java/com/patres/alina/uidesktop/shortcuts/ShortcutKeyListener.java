package com.patres.alina.uidesktop.shortcuts;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.patres.alina.common.event.Event;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.common.event.shortcut.FocusShortcutTriggeredEvent;
import com.patres.alina.uidesktop.common.event.shortcut.SpeechShortcutTriggeredEvent;
import com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent;
import com.patres.alina.uidesktop.settings.ShortcutKeysSettings;
import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;
import com.patres.alina.uidesktop.ui.listner.Listener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;
import static com.patres.alina.uidesktop.shortcuts.key.KeyboardKey.getKeyEvent;
import static com.patres.alina.uidesktop.shortcuts.key.KeyboardKey.getKeyEventAsKeyboardKey;


public class ShortcutKeyListener extends Listener implements NativeKeyListener {

    private final static Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());


    private final Set<KeyboardKey> pressedKeys;

    private ShortcutKeysSettings shortcutKeysSettings;

    private ShortcutKeyListener(ShortcutKeysSettings shortcutKeysSettings) {
        this.shortcutKeysSettings = shortcutKeysSettings;
        this.pressedKeys = new HashSet<>();
    }

    public static void init() {
        try {
            final ShortcutKeyListener shortcutKeyListener = new ShortcutKeyListener(UI_SETTINGS.getSettings().shortcutKeysSettings());
            GlobalScreen.addNativeKeyListener(shortcutKeyListener);
            DefaultEventBus.getInstance().subscribe(
                    UiSettingsUpdateEvent.class,
                    event -> shortcutKeyListener.updateShortcutKeysSettings()
            );
        } catch (Throwable e) {
            logger.warning("ShortcutKeyListener native hook disabled: " + e.getMessage());
        }
    }

    private void updateShortcutKeysSettings() {
        shortcutKeysSettings = UI_SETTINGS.getSettings().shortcutKeysSettings();
    }


    @Override
    public void nativeKeyTyped(NativeKeyEvent keyEvent) {
    }


    @Override
    public void nativeKeyPressed(NativeKeyEvent keyEvent) {
        getKeyEventAsKeyboardKey(keyEvent)
                .ifPresent(pressedKeys::add);
        checkKeys();
    }

    private void checkKeys() {
        checkKeysToTriggerAction(shortcutKeysSettings.speechShortcutKeys(), SpeechShortcutTriggeredEvent::new);
        checkKeysToTriggerAction(shortcutKeysSettings.focusShortcutKeys(), FocusShortcutTriggeredEvent::new);
    }

    private void checkKeysToTriggerAction(final ShortcutKeys shortcutKeys, final Supplier<Event> eventSupplier) {
        if (!shortcutKeys.getAllKeys().isEmpty() && pressedKeys.equals(shortcutKeys.getAllKeys())) {
            System.out.println(pressedKeys);
            DefaultEventBus.getInstance().publish(eventSupplier.get());
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
