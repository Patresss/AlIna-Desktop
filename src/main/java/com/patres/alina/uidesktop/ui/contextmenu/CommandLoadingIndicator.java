package com.patres.alina.uidesktop.ui.contextmenu;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.animation.AnimationTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.MouseInfo;
import java.awt.Point;

/**
 * Small always-on-top spinner shown while a command is executing.
 */
public class CommandLoadingIndicator {

    private static final Logger logger = LoggerFactory.getLogger(CommandLoadingIndicator.class);

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
            stage.toFront();
        });
    }

    public void hide() {
        if (stage == null) {
            return;
        }
        Platform.runLater(() -> {
            if (stage.isShowing()) {
                stage.hide();
            }
            stopFollowingCursor();
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
            stage.setX(mousePosition.getX());
            stage.setY(mousePosition.getY());
        } catch (Exception e) {
            logger.debug("Cannot resolve mouse position for loading indicator", e);
        }
    }

    private Stage createStage() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(22, 22);

        VBox container = new VBox(indicator);
        container.getStyleClass().add("context-menu-loader");
        container.setAlignment(Pos.CENTER);

        Scene scene = new Scene(container);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add("context-menu.css");

        Stage newStage = new Stage();
        newStage.setScene(scene);
        newStage.initStyle(StageStyle.TRANSPARENT);
        newStage.setAlwaysOnTop(true);
        newStage.setResizable(false);
        return newStage;
    }
}
