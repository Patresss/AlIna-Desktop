package com.patres.alina.uidesktop.ui;

import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;

public class ApplicationHeaderButtonBox extends HBox {

    @FXML
    private ToggleButton pinToggleButton;

    private ApplicationWindow applicationWindow;;

    public ApplicationHeaderButtonBox(ApplicationWindow applicationWindow) {
        super();
        this.applicationWindow = applicationWindow;
        try {
            var loader = new FXMLLoader(
                    Resources.getResource("fxml/header-bar-button-box.fxml").toURL()
            );
            loader.setController(ApplicationHeaderButtonBox.this);
            loader.setRoot(this);
            loader.setResources(LanguageManager.getBundle());
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load FXML file", e);
        }
    }

    @FXML
    public void initialize() {
        pinToggleButton.selectedProperty()
                .addListener((obs, newValue, oldValue) -> getStage().setAlwaysOnTop(!newValue));
    }


    @FXML
    public void openThreadHistories() {
        applicationWindow.openThreadHistories();
    }

    @FXML
    public void createAndOpenNewChatThread() {
        applicationWindow.createAndOpenNewChatThread();
    }

    @FXML
    public void openUiSettings() {
        applicationWindow.openUiSettings();
    }

    @FXML
    public void openServerSettings() {
        applicationWindow.openServerSettings();
    }

    @FXML
    public void openAssistantSettings() {
        applicationWindow.openAssistantSettings();
    }

    @FXML
    public void openCommands() {
        applicationWindow.openCommands();
    }

    @FXML
    public void openIntegrations() {
        applicationWindow.openIntegrations();
    }

    @FXML
    public void openLogs() {
        applicationWindow.openLogs();
    }

    public Stage getStage() {
        return (Stage) getScene().getWindow();
    }

}
