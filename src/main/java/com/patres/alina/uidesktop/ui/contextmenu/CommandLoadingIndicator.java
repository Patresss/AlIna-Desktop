package com.patres.alina.uidesktop.ui.contextmenu;

import com.patres.alina.uidesktop.ui.language.LanguageManager;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
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

    public void show() {
        Platform.runLater(() -> {
            if (stage == null) {
                stage = createStage();
            }
            try {
                final Point mousePosition = MouseInfo.getPointerInfo().getLocation();
                stage.setX(mousePosition.getX());
                stage.setY(mousePosition.getY());
            } catch (Exception e) {
                logger.debug("Cannot resolve mouse position for loading indicator, showing at default location", e);
            }
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
        });
    }

    private Stage createStage() {
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setPrefSize(32, 32);

        Label label = new Label(LanguageManager.getLanguageString("command.loading.title"));
        label.getStyleClass().add("context-menu-title");

        HBox row = new HBox(10, indicator, label);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(row);
        container.getStyleClass().add("context-menu-container");

        Scene scene = new Scene(container);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add("context-menu.css");

        Stage newStage = new Stage();
        newStage.setScene(scene);
        newStage.initStyle(StageStyle.UTILITY);
        newStage.setAlwaysOnTop(true);
        newStage.setResizable(false);
        return newStage;
    }
}
