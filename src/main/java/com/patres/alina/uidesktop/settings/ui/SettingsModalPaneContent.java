package com.patres.alina.uidesktop.settings.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import org.kordamp.ikonli.feather.Feather;

public abstract class SettingsModalPaneContent extends ApplicationModalPaneContent {

    public SettingsModalPaneContent(Runnable backFunction) {
        super(backFunction);
    }

    @FXML
    public void initialize() {
        super.initialize();
        final Button resetButton = createButton(Feather.REFRESH_CCW, e -> reset());
        final Button saveButton = createButton(Feather.SAVE, e -> save());
        buttonBar.getButtons().addAll(resetButton, saveButton);
    }


    @FXML
    public void back() {
        super.back();
        reset();
    }

    @Override
    public void reload() {
        reset();
    }

    @FXML
    protected abstract void reset();

    @FXML
    protected abstract void save();

}
