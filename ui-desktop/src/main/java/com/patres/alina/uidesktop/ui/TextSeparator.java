package com.patres.alina.uidesktop.ui;

import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.io.IOException;


public class TextSeparator extends HBox {

    @FXML
    private Text text;

    private final String textValue;

    public TextSeparator(final String textValue) {
        super();
        try {
            this.textValue = textValue;
            var loader = new FXMLLoader(
                    Resources.getResource("fxml/text-separator.fxml").toURL()
            );
            loader.setController(TextSeparator.this);
            loader.setRoot(this);
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load FXML file", e);
        }
    }

    public TextSeparator(final String textValue, final String style) {
        this(textValue);
        getStyleClass().add(style);
    }

    @FXML
    public void initialize() {
        text.setText(textValue);
    }

    public Text getText() {
        return text;
    }
}
