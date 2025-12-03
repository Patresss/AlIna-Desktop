package com.patres.alina.uidesktop.ui.contextmenu;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.common.event.CommandShortcutExecutedEvent;
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

    private ApplicationWindow applicationWindow;
    private final Set<KeyboardKey> pressedKeys;
    private final Map<Set<KeyboardKey>, Command> shortcutToCommandMap;
    private final StringBuilder responseBuilder = new StringBuilder();
    private String currentThreadId;

    public static void init(ApplicationWindow applicationWindow) {
        final CommandShortcutListener listener = new CommandShortcutListener(applicationWindow);
        GlobalScreen.addNativeKeyListener(listener);
        logger.info("CommandShortcutListener initialized");
    }

    private CommandShortcutListener(ApplicationWindow applicationWindow) {
        this.pressedKeys = new HashSet<>();
        this.shortcutToCommandMap = new HashMap<>();
        this.applicationWindow = applicationWindow;
        loadCommandShortcuts();
        subscribeToMessageCompletion();
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
        getKeyEventAsKeyboardKey(keyEvent).ifPresent(key -> {
            pressedKeys.add(key);
            logger.info("Key pressed: {}, current pressed keys: {}", key, pressedKeys);
        });
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
            try {
                // Wait for user to release shortcut keys (200ms is enough for natural key release)
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            String selectedText = SystemClipboard.copySelectedValue();
            logger.info("Executing command '{}' with selected text: '{}'", command.name(), selectedText);

            Platform.runLater(() -> {
                applicationWindow.getChatWindow().sendMessage(selectedText, command.id());
            });
        });
    }

    private void subscribeToMessageCompletion() {
        DefaultEventBus.getInstance().subscribe(ChatMessageStreamEvent.class, event -> {
            logger.debug("Received ChatMessageStreamEvent: type={}, threadId={}, currentThreadId={}",
                    event.getEventType(), event.getThreadId(), currentThreadId);

            // Only process events for the current thread
            if (currentThreadId == null || !event.getThreadId().equals(currentThreadId)) {
                logger.debug("Ignoring event - not for current thread");
                return;
            }

            if (event.getEventType() == ChatMessageStreamEvent.StreamEventType.TOKEN) {
                // Accumulate tokens
                logger.trace("Received token: {}", event.getToken());
                responseBuilder.append(event.getToken());
            } else if (event.getEventType() == ChatMessageStreamEvent.StreamEventType.COMPLETE) {
                // Stream complete, paste the result
                String finalResponse = responseBuilder.toString();
                logger.info("Command completed, response length: {}, pasting response", finalResponse.length());
                pasteResponse(finalResponse);

                // Reset for next command
                responseBuilder.setLength(0);
                currentThreadId = null;
            } else if (event.getEventType() == ChatMessageStreamEvent.StreamEventType.ERROR) {
                logger.error("Error during command execution: {}", event.getErrorMessage());
                responseBuilder.setLength(0);
                currentThreadId = null;
            }
        });
    }

    private void pasteResponse(String response) {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Starting paste operation...");

                // Copy response to clipboard
                SystemClipboard.copy(response);
                logger.debug("Response copied to clipboard");

                // Wait a bit for clipboard to update
                Thread.sleep(200);

                // Paste it
                SystemClipboard.paste();
                logger.info("Paste command sent");

                // Wait for paste to complete
                Thread.sleep(100);

                logger.info("Response pasted successfully");
            } catch (Exception e) {
                logger.error("Failed to paste response", e);
            }
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
