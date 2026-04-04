
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
     * Future that resolves to the captured context (selected text + source app).
     * Set before the menu is displayed; resolved by the time the user clicks a command.
     */
    private volatile CompletableFuture<CapturedContext> pendingCapture;

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
     * Shows the context menu immediately while text capture runs in the background.
     * The menu appears instantly; when the user clicks a command, the captured context
     * is retrieved from the future (which should already be complete by then).
     */
    public void displayWithPendingCapture(CompletableFuture<CapturedContext> capture) {
        this.pendingCapture = capture;
        Platform.runLater(() -> {
            if (stage == null) {
                logger.error("Stage is null after initialization");
                return;
            }

            loadCommands();
            showAtMousePosition();
        });
    }

    /**
     * Shows the context menu with already-captured context.
     * Used by HTTP server and other callers that capture context synchronously.
     */
    public void displayWithContext(CapturedContext context) {
        if (!context.hasText()) {
            logger.warn("No text captured, not showing context menu");
            return;
        }
        displayWithPendingCapture(CompletableFuture.completedFuture(context));
    }

    private void showAtMousePosition() {
        try {
            final Point mousePosition = MouseInfo.getPointerInfo().getLocation();
            final double x = mousePosition.getX();
            final double y = mousePosition.getY();
            stage.setX(x);
            stage.setY(y);

            stage.setAlwaysOnTop(true);
            stage.show();

            logger.debug("Context menu displayed at ({}, {})", x, y);
        } catch (HeadlessException e) {
            logger.error("Failed to get mouse position - headless environment", e);
        } catch (Exception e) {
            logger.error("Failed to display context menu", e);
        }
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
                cmd -> onCommandClicked(cmd, commandExecutor::executeWithCapturedText)
        );

        if (hasPaste && !displayCommands.isEmpty()) {
            commandsVBox.getChildren().add(new Separator());
        }

        boolean hasDisplay = addCommandGroup(
                LanguageManager.getLanguageString("context.menu.section.display"),
                displayCommands,
                cmd -> onCommandClicked(cmd, commandExecutor::executeWithCapturedTextAndDisplay)
        );

        if (!hasPaste && !hasDisplay) {
            Label noCommandsLabel = new Label(LanguageManager.getLanguageString("context.menu.empty"));
            noCommandsLabel.getStyleClass().add("context-menu-title");
            commandsVBox.getChildren().add(noCommandsLabel);
        }
    }

    /**
     * Called when a command button is clicked. Waits for the pending capture to complete
     * (should be near-instant since capture runs in parallel with user interaction),
     * then executes the command with the captured context.
     */
    private void onCommandClicked(Command command, java.util.function.BiConsumer<Command, CapturedContext> executor) {
        final CompletableFuture<CapturedContext> capture = this.pendingCapture;
        if (capture == null) {
            logger.warn("No pending capture for command '{}'", command.name());
            return;
        }
        capture.thenAcceptAsync(ctx -> {
            if (!ctx.hasText()) {
                logger.warn("No text captured for command '{}'", command.name());
                return;
            }
            executor.accept(command, ctx);
        });
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
