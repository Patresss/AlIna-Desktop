
package com.patres.alina.uidesktop.ui.contextmenu;

import atlantafx.base.theme.Styles;
import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.shortcuts.CommandExecutor;
import com.patres.alina.uidesktop.shortcuts.listener.ContextMenuKeyListener;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.util.MacTextAccessor.CapturedContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
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
import java.util.function.Consumer;

public class AppGlobalContextMenu extends StackPane implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(AppGlobalContextMenu.class);

    @FXML
    private VBox commandsVBox;

    private final CommandExecutor commandExecutor;
    private Stage stage;
    private final StackPane stackPane;

    /**
     * Holds the context captured before the menu was displayed.
     * This includes the selected text and the source app name,
     * captured while the source app was still frontmost.
     */
    private volatile CapturedContext capturedContext;

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
            initStage();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load FXML file", e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initial load will happen when menu is displayed
    }

    /**
     * Displays the context menu with pre-captured context.
     * The context (selected text + source app) should have been captured
     * BEFORE this method is called, while the source app was still frontmost.
     */
    public void displayWithContext(CapturedContext context) {
        this.capturedContext = context;
        Platform.runLater(() -> {
            if (stage == null) {
                logger.error("Stage is null after initialization");
                return;
            }

            if (!context.hasText()) {
                logger.warn("No text captured, not showing context menu");
                return;
            }

            loadCommands();

            try {
                final Point mousePosition = MouseInfo.getPointerInfo().getLocation();
                final double x = mousePosition.getX();
                final double y = mousePosition.getY();
                stage.setX(x);
                stage.setY(y);

                stage.setAlwaysOnTop(true);
                stage.show();

                logger.debug("Context menu displayed at ({}, {}) with {} chars of captured text",
                        x, y, context.selectedText().length());
            } catch (HeadlessException e) {
                logger.error("Failed to get mouse position - headless environment", e);
            } catch (Exception e) {
                logger.error("Failed to display context menu", e);
            }
        });
    }

    private void loadCommands() {
        commandsVBox.getChildren().clear();

        List<Command> commands = BackendApi.getEnabledCommands();
        List<Command> pasteCommands = commands.stream()
                .filter(cmd -> cmd.visibility().showInContextMenuPaste())
                .toList();
        List<Command> displayCommands = commands.stream()
                .filter(cmd -> cmd.visibility().showInContextMenuDisplay())
                .toList();

        boolean hasPaste = addCommandGroup(
                LanguageManager.getLanguageString("context.menu.section.paste"),
                pasteCommands,
                cmd -> commandExecutor.executeWithCapturedText(cmd, capturedContext)
        );

        if (hasPaste && !displayCommands.isEmpty()) {
            commandsVBox.getChildren().add(new Separator());
        }

        boolean hasDisplay = addCommandGroup(
                LanguageManager.getLanguageString("context.menu.section.display"),
                displayCommands,
                cmd -> commandExecutor.executeWithCapturedTextAndDisplay(cmd, capturedContext)
        );

        if (!hasPaste && !hasDisplay) {
            Label noCommandsLabel = new Label(LanguageManager.getLanguageString("context.menu.empty"));
            noCommandsLabel.getStyleClass().add("context-menu-title");
            commandsVBox.getChildren().add(noCommandsLabel);
        }
    }

    private boolean addCommandGroup(String groupName, List<Command> commands, Consumer<Command> action) {
        if (commands.isEmpty()) {
            return false;
        }
        Label groupLabel = new Label(groupName);
        groupLabel.getStyleClass().add("context-menu-title");
        commandsVBox.getChildren().add(groupLabel);

        for (Command command : commands) {
            Button commandButton = createCommandButton(command, action);
            commandsVBox.getChildren().add(commandButton);
        }
        return true;
    }

    private Button createCommandButton(Command command, Consumer<Command> action) {
        Button button = new Button(command.name());
        button.getStyleClass().addAll(Styles.FLAT, "context-menu-button");
        button.setMaxWidth(Double.MAX_VALUE);

        button.setOnAction(_ -> {
            close();
            action.accept(command);
        });

        return button;
    }

    private void initStage() {
        Platform.runLater(() -> {
            stage = createStage();
            logger.info("Context menu stage initialized");
        });
    }

    private Stage createStage() {
        Stage newStage = new Stage();
        newStage.setAlwaysOnTop(true);

        Scene scene = new Scene(stackPane);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add("context-menu.css");
        newStage.setScene(scene);
        newStage.initStyle(StageStyle.UTILITY);

        newStage.setResizable(false);

        scene.setOnMouseExited(e -> {
            if (e.getSceneY() < 0 || e.getSceneY() > scene.getHeight() || e.getSceneX() < 0 || e.getSceneX() > scene.getWidth()) {
                CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(500);
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
