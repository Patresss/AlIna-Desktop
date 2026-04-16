package com.patres.alina.uidesktop.scheduler;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.scheduler.ExecutionMode;
import com.patres.alina.server.scheduler.ScheduledTask;
import com.patres.alina.server.scheduler.SchedulerTaskTriggeredEvent;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Handles execution of scheduled tasks on the UI side.
 * Subscribes to {@link SchedulerTaskTriggeredEvent} and dispatches
 * the prompt to the correct tab based on the task's {@link ExecutionMode}.
 */
public class SchedulerTaskExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerTaskExecutor.class);

    private final ApplicationWindow applicationWindow;

    public SchedulerTaskExecutor(ApplicationWindow applicationWindow) {
        this.applicationWindow = applicationWindow;
        subscribeToEvents();
    }

    private void subscribeToEvents() {
        DefaultEventBus.getInstance().subscribe(
                SchedulerTaskTriggeredEvent.class,
                this::handleTaskTriggered
        );
    }

    private void handleTaskTriggered(SchedulerTaskTriggeredEvent event) {
        ScheduledTask task = event.getTask();
        logger.info("Executing scheduled task '{}' with mode {}", task.name(), task.executionMode());

        ExecutionMode mode = task.executionMode() != null ? task.executionMode() : ExecutionMode.NEW_TAB;

        switch (mode) {
            case CURRENT_TAB -> executeInCurrentTab(task);
            case NEW_TAB -> executeInNewTab(task);
            case BACKGROUND -> executeInBackground(task);
        }
    }

    /**
     * Sends the prompt in the currently active tab.
     */
    private void executeInCurrentTab(ScheduledTask task) {
        Platform.runLater(() -> {
            var chatWindow = applicationWindow.getChatWindow();
            if (chatWindow != null) {
                chatWindow.sendMessageWithModel(task.prompt(), null, null, task.model());
            } else {
                logger.warn("No active chat window for CURRENT_TAB mode, falling back to NEW_TAB");
                executeInNewTab(task);
            }
        });
    }

    /**
     * Creates a new tab, activates it, and sends the prompt there.
     * Uses a short delay to let the WebView finish loading before sending the message.
     */
    private void executeInNewTab(ScheduledTask task) {
        Thread.startVirtualThread(() -> {
            ChatThread newThread = BackendApi.createChatThread();
            Platform.runLater(() -> {
                applicationWindow.loadChatThread(newThread, List.of());
                // Delay sending to let WebView initialize (avoid scrollToBottom race)
                PauseTransition delay = new PauseTransition(Duration.millis(500));
                delay.setOnFinished(e -> {
                    var chatWindow = applicationWindow.getChatWindow();
                    if (chatWindow != null) {
                        chatWindow.sendMessageWithModel(task.prompt(), null, null, task.model());
                    }
                });
                delay.play();
            });
        });
    }

    /**
     * Creates a new session and sends the prompt without creating a visible tab.
     * The response goes to the OpenCode session but does not appear in the UI.
     */
    private void executeInBackground(ScheduledTask task) {
        Thread.startVirtualThread(() -> {
            try {
                ChatThread bgThread = BackendApi.createChatThread();
                ChatMessageSendModel sendModel = new ChatMessageSendModel(
                        task.prompt(),
                        bgThread.id(),
                        null,
                        ChatMessageStyleType.NONE,
                        null,
                        task.model()
                );
                BackendApi.sendChatMessagesStream(sendModel);
                logger.info("Background task '{}' completed in thread {}", task.name(), bgThread.id());
            } catch (Exception e) {
                logger.error("Background task '{}' failed", task.name(), e);
            }
        });
    }
}
