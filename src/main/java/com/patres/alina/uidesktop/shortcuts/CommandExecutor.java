package com.patres.alina.uidesktop.shortcuts;

import com.patres.alina.common.message.OnMessageCompleteCallback;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.contextmenu.CommandLoadingIndicator;
import com.patres.alina.uidesktop.ui.contextmenu.CommandResultPopup;
import com.patres.alina.uidesktop.ui.util.SystemClipboard;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Executes commands triggered from different UI entry points and handles AI responses.
 */
public class CommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);
    private static final CommandResultPopup RESULT_POPUP = new CommandResultPopup();
    private static final CommandLoadingIndicator LOADING_INDICATOR = new CommandLoadingIndicator();

    private final ApplicationWindow applicationWindow;
    private final OnMessageCompleteCallback pasteResponseCallback = this::pasteAiResponse;

    public CommandExecutor(ApplicationWindow applicationWindow) {
        this.applicationWindow = applicationWindow;
    }

    public void executeWithSelectedText(Command command) {
        execute(command, pasteResponseCallback);
    }

    public void executeWithSelectedTextAndDisplay(Command command) {
        execute(command, aiResponse -> displayAiResponse(command, aiResponse));
    }

    private void execute(Command command, OnMessageCompleteCallback onComplete) {
        CompletableFuture.runAsync(() -> {
            String selectedText = SystemClipboard.copySelectedValue();
            if (selectedText.isBlank()) {
                logger.warn("Skipping command '{}' because no text is selected", command.name());
                return;
            }
            logger.info("Executing command '{}' with selected text: '{}'", command.name(), selectedText);

            Platform.runLater(() -> {
                LOADING_INDICATOR.show();
                String threadId = applicationWindow.getChatThread().map(t -> t.id()).orElse(null);
                subscribeToStream(threadId);
                applicationWindow.getChatWindow().sendMessage(selectedText, command.id(), wrapOnComplete(onComplete, threadId));
            });
        });
    }

    private OnMessageCompleteCallback wrapOnComplete(OnMessageCompleteCallback downstream, String threadId) {
        return aiResponse -> {
            LOADING_INDICATOR.hide();
            unsubscribeFromStream(threadId);
            if (downstream != null) {
                downstream.onComplete(aiResponse);
            }
        };
    }

    private void pasteAiResponse(String aiResponse) {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("AI response received (length: {}), pasting...", aiResponse.length());

                SystemClipboard.copyAndPaste(aiResponse);

                logger.info("AI response pasted successfully");
            } catch (Exception e) {
                logger.error("Failed to paste AI response", e);
            }
        });
    }

    private void displayAiResponse(Command command, String aiResponse) {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("AI response received (length: {}), displaying popup...", aiResponse.length());
                RESULT_POPUP.display(command.name(), aiResponse);
            } catch (Exception e) {
                logger.error("Failed to display AI response", e);
            }
        });
    }

    private void subscribeToStream(String threadId) {
        if (threadId == null) {
            return;
        }
        Consumer<ChatMessageStreamEvent> listener = new Consumer<>() {
            @Override
            public void accept(ChatMessageStreamEvent event) {
                if (!threadId.equals(event.getThreadId())) {
                    return;
                }
                if (event.getEventType() == ChatMessageStreamEvent.StreamEventType.COMPLETE
                        || event.getEventType() == ChatMessageStreamEvent.StreamEventType.CANCELLED
                        || event.getEventType() == ChatMessageStreamEvent.StreamEventType.ERROR) {
                    LOADING_INDICATOR.hide();
                    DefaultEventBus.getInstance().unsubscribe(ChatMessageStreamEvent.class, this);
                }
            }
        };
        DefaultEventBus.getInstance().subscribe(ChatMessageStreamEvent.class, listener);
    }

    private void unsubscribeFromStream(String threadId) {
        // Safety: stream listener will self-unsubscribe; nothing more needed here.
    }
}
