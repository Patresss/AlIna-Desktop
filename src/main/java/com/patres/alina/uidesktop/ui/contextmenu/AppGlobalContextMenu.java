
package com.patres.alina.uidesktop.ui.contextmenu;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.util.SystemClipboard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public class AppGlobalContextMenu extends StackPane implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AppGlobalContextMenu.class);

    @FXML
    private VBox commandsVBox;

    private ApplicationWindow applicationWindow;
    private Stage stage;
    private final StackPane stackPane;
    private String currentThreadId;
    private final StringBuilder responseBuilder = new StringBuilder();
    private final CountDownLatch initLatch = new CountDownLatch(1);

    public static void init(ApplicationWindow applicationWindow) {
        new AppGlobalContextMenu(applicationWindow);
    }

    public AppGlobalContextMenu(ApplicationWindow applicationWindow) {
        super();
        this.applicationWindow = applicationWindow;
        ContextMenuKeyListener.init(this);
        try {
            var loader = new FXMLLoader(
                    Resources.getResource("fxml/context-menu.fxml").toURL()
            );
            loader.setController(AppGlobalContextMenu.this);
            loader.setRoot(this);
            this.stackPane = loader.load();

            // Initialize stage on JavaFX thread
            Platform.runLater(() -> {
                try {
                    stage = createStage();
                    logger.info("Context menu stage initialized");
                } finally {
                    initLatch.countDown();
                }
            });

            // Subscribe to stream completion events
            subscribeToMessageCompletion();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load FXML file", e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initial load will happen when menu is displayed
    }

    private void loadCommands() {
        commandsVBox.getChildren().clear();

        // Add title
        Label titleLabel = new Label("Wybierz komendę:");
        titleLabel.getStyleClass().add("context-menu-title");
        commandsVBox.getChildren().add(titleLabel);

        // Load enabled commands from backend
        List<Command> commands = BackendApi.getEnabledCommands();

        if (commands.isEmpty()) {
            Label noCommandsLabel = new Label("Brak dostępnych komend");
            noCommandsLabel.getStyleClass().add("context-menu-title");
            commandsVBox.getChildren().add(noCommandsLabel);
        }

        for (Command command : commands) {
            Button commandButton = createCommandButton(command);
            commandsVBox.getChildren().add(commandButton);
        }
    }

    private Button createCommandButton(Command command) {
        Button button = new Button(command.name());
        button.getStyleClass().addAll(Styles.FLAT, "context-menu-button");
        button.setMaxWidth(Double.MAX_VALUE);

        button.setOnAction(event -> {
            // Close menu first WITHOUT activating main window
            closeQuietly();
            // Execute command in background
            CompletableFuture.runAsync(() -> executeCommand(command));
        });

        return button;
    }

    private void executeCommand(Command command) {
        String selectedText = SystemClipboard.copySelectedValue();
        logger.info("Executing command '{}' with selected text", command.name());
        applicationWindow.getChatWindow().sendMessage(selectedText, command.id());
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

    public void displayContextMenu() {
            Platform.runLater(() -> {
                if (stage == null) {
                    logger.error("Stage is null after initialization");
                    return;
                }

                loadCommands(); // Reload commands in case they changed

                try {
                    final Point mousePosition = MouseInfo.getPointerInfo().getLocation();
                    final double x = mousePosition.getX();
                    final double y = mousePosition.getY();
                    stage.setX(x);
                    stage.setY(y);

                    // Show WITHOUT requesting focus to avoid activating main window
                    stage.setAlwaysOnTop(true);
                    stage.show();

                    logger.debug("Context menu displayed at ({}, {})", x, y);
                } catch (HeadlessException e) {
                    logger.error("Failed to get mouse position - headless environment", e);
                } catch (Exception e) {
                    logger.error("Failed to display context menu", e);
                }
            });
    }

    private Stage createStage() {
        Stage newStage = new Stage();
        newStage.setAlwaysOnTop(true);

        Scene scene = new Scene(stackPane);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT); // Make scene background transparent
        scene.getStylesheets().add("context-menu.css");
        newStage.setScene(scene);
        newStage.initStyle(StageStyle.UTILITY); // UTILITY properly releases focus, TRANSPARENT has issues on macOS

        newStage.setResizable(false);

        // Close when losing focus (but don't use focusedProperty as it activates main window)
        scene.setOnMouseExited(e -> {
            // Close if mouse leaves the window bounds
            if (e.getSceneY() < 0 || e.getSceneY() > scene.getHeight() || e.getSceneX() < 0 || e.getSceneX() > scene.getWidth()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(500); // Give time to move mouse back
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    close();
                });
            }
        });


        return newStage;
    }

    private void closeQuietly() {
        if (stage != null) {
            Platform.runLater(() -> {
                if (stage.isShowing()) {
                    stage.hide();
                    logger.info("Context menu closed quietly");
                }
            });
        }
    }

    public void close() {
        closeQuietly();
    }

}
