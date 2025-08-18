
package com.patres.alina.uidesktop.ui.contextmenu;

import atlantafx.base.theme.Styles;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.ui.util.SystemClipboard;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import static com.patres.alina.uidesktop.RetryLogic.runWithRetry;
import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;

public class AppGlobalContextMenu extends StackPane implements Initializable {

    private MenuButton selectedMenuButton = null;

    @FXML
    private TextField promptValueTextField;

    @FXML
    private VBox vBox;

    private final Stage stage;
    private final StackPane stackPane;

    public static void init() {
        new AppGlobalContextMenu();
    }


    public AppGlobalContextMenu() {
        super();
        ContextMenuKeyListener.init(this);
        try {
            var loader = new FXMLLoader(
                    Resources.getResource("fxml/systemPrompt-menu.fxml").toURL()
            );
            loader.setController(AppGlobalContextMenu.this);
            loader.setRoot(this);
            this.stackPane = loader.load();
            this.stage = createStage();
        } catch (IOException e) {
            throw new RuntimeException("Unable to load FXML file", e);
        }
    }

    public void setPromptValue(final String promptValue) {
        promptValueTextField.setText(promptValue);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        vBox.getChildren().addAll(
                createMenuButton("Zapytaj ChatGPT"),
                createMenuButton("Przetłumacz na angielski"),
                createMenuButton("Przetłumacz na polski"),
                createMenuButton("Sparafrazuj")
        );

        addHidingMenuButtonsStrategy();

    }

    private void addHidingMenuButtonsStrategy() {
        promptValueTextField.addEventHandler(MOUSE_ENTERED, e -> updateMenuButton(null));
        vBox.getChildren().stream()
                .filter(node -> node instanceof MenuButton)
                .map(node -> (MenuButton) node)
                .forEach(menuButton -> {
                    menuButton.addEventHandler(MOUSE_ENTERED, e -> updateMenuButton(menuButton));
                });
    }

    private void updateMenuButton(final MenuButton newSelectedMenuButton) {
        if (selectedMenuButton != null) {
            selectedMenuButton.hide();
        }
        selectedMenuButton = newSelectedMenuButton;
        if (selectedMenuButton != null) {
            selectedMenuButton.show();
        }
    }

    @NotNull
    private MenuButton createMenuButton(final String text) {
        var rightMenuBtn = new MenuButton(text);
        rightMenuBtn.getItems().addAll(createItems());
        rightMenuBtn.setPopupSide(Side.RIGHT);
        rightMenuBtn.getStyleClass().add(Styles.FLAT);
        return rightMenuBtn;
    }

    private List<MenuItem> createItems() {
        return List.of(
                new MenuItem("Skopiuj do schowka"),
                new MenuItem("Zastąp (wklej)"),
                new MenuItem("Wklej poniżej"),
                new MenuItem("Wyświetl")
        );
    }


    public void displayContextMenu() {
        calculatePromptValue();
        Platform.runLater(() -> {
            final Point mousePosition = MouseInfo.getPointerInfo().getLocation();
            final double x = mousePosition.getX();
            final double y = mousePosition.getY();
            stage.setX(x);
            stage.setY(y);
            stage.show();
        });
    }

    private void calculatePromptValue() {
        final String selectedText = SystemClipboard.copySelectedValue();
        Platform.runLater(() -> runWithRetry(() -> promptValueTextField.setText(selectedText), 3));
    }

    private Stage createStage() {
        Stage stage = new Stage();
        stage.setAlwaysOnTop(true);
        Scene scene = new Scene(stackPane);
        scene.getStylesheets().add("context-menu.css");
        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);
        return stage;
    }

    public void close() {
        Platform.runLater(stage::close);
    }

}