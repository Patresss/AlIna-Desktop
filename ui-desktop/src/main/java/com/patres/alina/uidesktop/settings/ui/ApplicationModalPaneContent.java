package com.patres.alina.uidesktop.settings.ui;

import atlantafx.base.theme.Styles;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.ui.AppModalPane;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.util.List;

public abstract class ApplicationModalPaneContent extends BorderPane {

    protected final Runnable backFunction;

    @FXML
    protected StackPane contentPane;

    @FXML
    protected VBox settingsBox;


    @FXML
    protected ButtonBar buttonBar;


    public ApplicationModalPaneContent(Runnable backFunction) {
        super();
        this.backFunction = backFunction;
        try {
            var loader = new FXMLLoader(
                    Resources.getResource("fxml/settings.fxml").toURL()
            );
            loader.setController(ApplicationModalPaneContent.this);
            loader.setRoot(this);
            loader.load();
            setMaxWidth(Double.MAX_VALUE);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load FXML file", e);
        }
    }

    @FXML
    public void initialize() {
        settingsBox.getChildren().addAll(generateContent());
    }

    @FXML
    public void back() {
        backFunction.run();
    }

    public abstract void reload();

    protected abstract List<Node> generateContent();

    protected Button createButton(final Ikon iconCode, final EventHandler<ActionEvent> eventHandler) {
        final Button resetButton = new Button(null, new FontIcon(iconCode));
        resetButton.setOnAction(eventHandler);
        resetButton.getStyleClass()
                .addAll(Styles.BUTTON_CIRCLE, Styles.FLAT);
        return resetButton;
    }


}
