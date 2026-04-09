package com.patres.alina.uidesktop.ui.contextmenu;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.MouseInfo;
import java.awt.Point;

/**
 * Small always-on-top spinner shown while a command is executing.
 * Follows the mouse cursor with a small offset so it acts as a visual badge
 * without intercepting clicks on the underlying application.
 */
public class CommandLoadingIndicator {

    private static final Logger logger = LoggerFactory.getLogger(CommandLoadingIndicator.class);
    private static final double SIZE = 22;

    /** Offset from the cursor so the spinner never covers the click target. */
    private static final double CURSOR_OFFSET_X = -SIZE - 4;
    private static final double CURSOR_OFFSET_Y = -SIZE - 4;

    private Stage stage;
    private AnimationTimer followMouseTimer;

    public void show() {
        Platform.runLater(() -> {
            if (stage == null) {
                stage = createStage();
            }
            updatePosition();
            startFollowingCursor();
            stage.show();
        });
    }

    public void hide() {
        if (stage == null) {
            return;
        }
        Platform.runLater(() -> {
            stopFollowingCursor();
            if (stage.isShowing()) {
                stage.hide();
            }
        });
    }

    private void startFollowingCursor() {
        if (followMouseTimer != null) {
            followMouseTimer.stop();
        }
        followMouseTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updatePosition();
            }
        };
        followMouseTimer.start();
    }

    private void stopFollowingCursor() {
        if (followMouseTimer != null) {
            followMouseTimer.stop();
            followMouseTimer = null;
        }
    }

    private void updatePosition() {
        if (stage == null) {
            return;
        }
        try {
            final Point mousePosition = MouseInfo.getPointerInfo().getLocation();
            stage.setX(mousePosition.getX() + CURSOR_OFFSET_X);
            stage.setY(mousePosition.getY() + CURSOR_OFFSET_Y);
        } catch (Exception e) {
            logger.debug("Cannot resolve mouse position for loading indicator", e);
        }
    }

    private Stage createStage() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(SIZE, SIZE);

        VBox container = new VBox(indicator);
        container.getStyleClass().add("context-menu-loader");
        container.setAlignment(Pos.CENTER);
        container.setMouseTransparent(true);

        Scene scene = new Scene(container);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add("context-menu.css");

        Stage newStage = new Stage();
        newStage.setScene(scene);
        newStage.initStyle(StageStyle.TRANSPARENT);
        newStage.setAlwaysOnTop(true);
        newStage.setResizable(false);
        newStage.setWidth(SIZE);
        newStage.setHeight(SIZE);
        return newStage;
    }
}
