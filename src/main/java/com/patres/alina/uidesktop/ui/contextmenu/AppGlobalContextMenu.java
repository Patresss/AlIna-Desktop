
package com.patres.alina.uidesktop.ui.contextmenu;

import atlantafx.base.theme.Styles;
import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.shortcuts.CommandExecutor;
import com.patres.alina.uidesktop.shortcuts.listener.ContextMenuKeyListener;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
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

public class AppGlobalContextMenu extends StackPane implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AppGlobalContextMenu.class);

    @FXML
    private VBox commandsVBox;

    private final CommandExecutor commandExecutor;
    private Stage stage;
    private final StackPane stackPane;

    public static AppGlobalContextMenu init(ApplicationWindow applicationWindow) {
        return new AppGlobalContextMenu(applicationWindow);
    }

    public AppGlobalContextMenu(ApplicationWindow applicationWindow) {
        super();
        this.commandExecutor = new CommandExecutor(applicationWindow);
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
                stage = createStage();
                logger.info("Context menu stage initialized");
            });
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
            close();
            commandExecutor.executeWithSelectedText(command);
        });

        return button;
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

    public void close() {
        if (stage != null) {
            Platform.runLater(() -> {
                if (stage.isShowing()) {
                    stage.hide();
                    logger.info("Context menu closed quietly");
                }
            });
        }
    }

}
