package com.patres.alina.uidesktop.ui.contextmenu;

import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.util.SystemClipboard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.MouseInfo;
import java.awt.Point;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * Small always-on-top popup for displaying AI command results.
 */
public class CommandResultPopup extends StackPane implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(CommandResultPopup.class);

    @FXML
    private Label titleLabel;

    @FXML
    private TextArea contentArea;

    @FXML
    private Button copyButton;

    @FXML
    private Button closeButton;

    private Stage stage;
    private StackPane stackPane;

    public CommandResultPopup() {
        super();
        try {
            var loader = new FXMLLoader(Resources.getResource("fxml/command-result-popup.fxml").toURL());
            loader.setController(this);
            loader.setRoot(this);
            this.stackPane = loader.load();
            initStage();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load command result popup FXML", e);
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        copyButton.setOnAction(_ -> copyContent());
        closeButton.setOnAction(_ -> close());
        copyButton.setText(LanguageManager.getLanguageString("command.result.copy"));
        closeButton.setText(LanguageManager.getLanguageString("command.result.close"));
    }

    private void initStage() {
        Platform.runLater(() -> {
            if (stage == null) {
                stage = createStage();
                logger.info("Command result popup initialized");
            }
        });
    }

    private Stage createStage() {
        Stage newStage = new Stage();
        Scene scene = new Scene(stackPane);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        scene.getStylesheets().add("context-menu.css");

        newStage.setScene(scene);
        newStage.initStyle(StageStyle.UTILITY);
        newStage.setAlwaysOnTop(true);
        newStage.setResizable(true);
        newStage.setMinWidth(350);
        newStage.setMinHeight(200);

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
            }
        });
        newStage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (wasFocused && !isFocused) {
                close();
            }
        });

        return newStage;
    }

    public void display(String commandName, String content) {
        Platform.runLater(() -> {
            if (stage == null) {
                stage = createStage();
            }
            titleLabel.setText(LanguageManager.getLanguageString("command.result.title", Optional.ofNullable(commandName).orElse("")));
            contentArea.setText(content);
            contentArea.positionCaret(0);
            try {
                final Point mousePosition = MouseInfo.getPointerInfo().getLocation();
                stage.setX(mousePosition.getX());
                stage.setY(mousePosition.getY());
            } catch (Exception e) {
                logger.warn("Cannot get mouse position, showing popup at default location", e);
            }
            stage.show();
            stage.toFront();
        });
    }

    public void close() {
        if (stage != null) {
            Platform.runLater(() -> {
                if (stage.isShowing()) {
                    stage.hide();
                }
            });
        }
    }

    private void copyContent() {
        final String text = contentArea.getText();
        SystemClipboard.copy(text);
    }
}
