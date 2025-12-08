package com.patres.alina.uidesktop.ui.contextmenu;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.OnMessageCompleteCallback;
import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.listner.Listener;
import com.patres.alina.uidesktop.ui.util.SystemClipboard;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.patres.alina.uidesktop.shortcuts.key.KeyboardKey.getKeyEvent;
import static com.patres.alina.uidesktop.shortcuts.key.KeyboardKey.getKeyEventAsKeyboardKey;

public class CommandShortcutListener extends Listener implements NativeKeyListener {

    private static final Logger logger = LoggerFactory.getLogger(CommandShortcutListener.class);

    private final ApplicationWindow applicationWindow;
    private final Set<KeyboardKey> pressedKeys;
    private final Map<Set<KeyboardKey>, Command> shortcutToCommandMap;
    private final OnMessageCompleteCallback pasteResponseCallback;

    public static void init(ApplicationWindow applicationWindow) {
        final CommandShortcutListener listener = new CommandShortcutListener(applicationWindow);
        GlobalScreen.addNativeKeyListener(listener);
        logger.info("CommandShortcutListener initialized");
    }

    private CommandShortcutListener(ApplicationWindow applicationWindow) {
        this.applicationWindow = applicationWindow;
        this.pressedKeys = Collections.synchronizedSet(new HashSet<>());
        this.shortcutToCommandMap = new HashMap<>();
        this.pasteResponseCallback = this::pasteAiResponse;
        loadCommandShortcuts();
    }

    private void pasteAiResponse(String aiResponse) {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("AI response received (length: {}), pasting...", aiResponse.length());

                // Copy to clipboard and paste using a single, locked operation
                SystemClipboard.copyAndPaste(aiResponse);

                logger.info("AI response pasted successfully");
            } catch (Exception e) {
                logger.error("Failed to paste AI response", e);
            }
        });
    }

    private void loadCommandShortcuts() {
        shortcutToCommandMap.clear();
        List<Command> commands = BackendApi.getEnabledCommands();
        logger.info("Loading command shortcuts from {} enabled commands", commands.size());

        for (Command command : commands) {
            logger.info("Checking command '{}': hasShortcut={}, shortcut={}",
                command.name(),
                command.globalShortcut() != null,
                command.globalShortcut() != null ? command.globalShortcut().getAllKeys() : "null");

            if (command.globalShortcut() != null && !command.globalShortcut().getAllKeys().isEmpty()) {
                Set<KeyboardKey> allKeys = command.globalShortcut().getAllKeys();
                shortcutToCommandMap.put(allKeys, command);
                logger.info("✓ Registered global shortcut {} for command '{}'", allKeys, command.name());
            } else {
                logger.info("✗ Command '{}' has no global shortcut configured", command.name());
            }
        }

        logger.info("Loaded {} command shortcuts total", shortcutToCommandMap.size());
        logger.info("Registered shortcuts map: {}", shortcutToCommandMap.keySet());
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent keyEvent) {
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent keyEvent) {
        getKeyEventAsKeyboardKey(keyEvent).ifPresent(pressedKeys::add);
        checkKeys();
    }

    private void checkKeys() {
        // Check if current pressed keys match any command shortcut
        logger.debug("Checking pressed keys: {} against {} registered shortcuts", pressedKeys, shortcutToCommandMap.size());

        for (Map.Entry<Set<KeyboardKey>, Command> entry : shortcutToCommandMap.entrySet()) {
            logger.debug("Comparing pressed keys {} with shortcut {} for command '{}'",
                pressedKeys, entry.getKey(), entry.getValue().name());

            if (pressedKeys.equals(entry.getKey())) {
                Command command = entry.getValue();
                logger.info("Global shortcut triggered for command: {}", command.name());
                executeCommandWithSelectedText(command);
                break;
            }
        }
    }

    private void executeCommandWithSelectedText(Command command) {
        CompletableFuture.runAsync(() -> {
            String selectedText = SystemClipboard.copySelectedValue();
            logger.info("Executing command '{}' with selected text: '{}'", command.name(), selectedText);

            Platform.runLater(() -> {
                // Send to existing chat window with paste callback
                applicationWindow.getChatWindow().sendMessage(selectedText, command.id(), pasteResponseCallback);
            });
        });
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent keyEvent) {
        if (List.of(KeyboardKey.CONTROL.getKeyValue(), KeyboardKey.ALT.getKeyValue(), KeyboardKey.META.getKeyValue())
                .contains(getKeyEvent(keyEvent))) {
            pressedKeys.clear();
        } else {
            getKeyEventAsKeyboardKey(keyEvent).ifPresent(pressedKeys::remove);
        }
    }

    public void reloadShortcuts() {
        logger.info("Reloading command shortcuts...");
        loadCommandShortcuts();
    }
}
