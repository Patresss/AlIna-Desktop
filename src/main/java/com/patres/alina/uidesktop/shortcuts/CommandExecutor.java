package com.patres.alina.uidesktop.shortcuts;

import com.patres.alina.common.message.OnMessageCompleteCallback;
import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.util.SystemClipboard;
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Executes commands triggered from different UI entry points and pastes AI responses.
 */
public class CommandExecutor {

    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);

    private final ApplicationWindow applicationWindow;
    private final OnMessageCompleteCallback pasteResponseCallback = this::pasteAiResponse;

    public CommandExecutor(ApplicationWindow applicationWindow) {
        this.applicationWindow = applicationWindow;
    }

    public void executeWithSelectedText(Command command) {
        CompletableFuture.runAsync(() -> {
            String selectedText = SystemClipboard.copySelectedValue();
            if (selectedText.isBlank()) {
                logger.warn("Skipping command '{}' because no text is selected", command.name());
                return;
            }
            logger.info("Executing command '{}' with selected text: '{}'", command.name(), selectedText);

            Platform.runLater(() ->
                    applicationWindow.getChatWindow().sendMessage(selectedText, command.id(), pasteResponseCallback)
            );
        });
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
}
