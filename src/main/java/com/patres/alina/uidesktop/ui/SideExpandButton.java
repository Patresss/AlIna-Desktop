package com.patres.alina.uidesktop.ui;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;

import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;

/**
 * A floating button rendered entirely OUTSIDE the left edge of the main window
 * via a focus-safe {@link Popup}. Nearly invisible (opacity 0.1) until hovered.
 * Clicking expands the window to the left; clicking again restores it.
 */
public class SideExpandButton {

    private static final double BUTTON_WIDTH = 28;
    private static final double BUTTON_HEIGHT = 64;

    private static final String STYLE_BASE =
            "-fx-background-color: -color-accent-emphasis;" +
            "-fx-background-radius: 8 0 0 8;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 22px;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, -4, 0);";

    private boolean expanded = false;

    public void attach(Stage mainStage) {
        var button = new Button("‹");
        button.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setMinSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setMaxSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setStyle(STYLE_BASE);

        // Nearly invisible by default — fully visible on hover
        button.setOpacity(0.1);
        button.setOnMouseEntered(e -> button.setOpacity(1.0));
        button.setOnMouseExited(e -> button.setOpacity(0.1));

        button.setOnAction(e -> {
            var screenBounds = Screen.getPrimary().getVisualBounds();
            int expandBy = UI_SETTINGS.getSettings().resolveExpandWidth();
            if (!expanded) {
                double newX = Math.max(screenBounds.getMinX(), mainStage.getX() - expandBy);
                double actualExpand = mainStage.getX() - newX;
                mainStage.setX(newX);
                mainStage.setWidth(mainStage.getWidth() + actualExpand);
                button.setText("›");
                expanded = true;
            } else {
                double originalWidth = AssistantAppLauncher.WIDTH;
                double shrinkBy = mainStage.getWidth() - originalWidth;
                if (shrinkBy > 0) {
                    mainStage.setX(mainStage.getX() + shrinkBy);
                    mainStage.setWidth(originalWidth);
                }
                button.setText("‹");
                expanded = false;
            }
        });

        var root = new StackPane(button);
        // Explicit size so the Popup takes exactly BUTTON_WIDTH x BUTTON_HEIGHT
        root.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        root.setMaxSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        root.setStyle("-fx-background-color: transparent;");

        // Popup never steals focus from the owner stage
        var popup = new Popup();
        popup.setAutoFix(false);
        popup.setHideOnEscape(false);
        popup.getContent().add(root);

        // Position popup so its RIGHT edge aligns exactly with the main stage's LEFT edge
        Runnable syncPosition = () -> {
            popup.setX(mainStage.getX() - BUTTON_WIDTH);
            popup.setY(mainStage.getY() + (mainStage.getHeight() - BUTTON_HEIGHT) / 2.0);
        };

        mainStage.xProperty().addListener((obs, o, n) -> syncPosition.run());
        mainStage.yProperty().addListener((obs, o, n) -> syncPosition.run());
        mainStage.heightProperty().addListener((obs, o, n) -> syncPosition.run());
        mainStage.widthProperty().addListener((obs, o, n) -> syncPosition.run());

        mainStage.showingProperty().addListener((obs, wasShowing, isShowing) -> {
            if (isShowing) {
                syncPosition.run();
                boolean show = UI_SETTINGS.getSettings().isShowExpandButton();
                if (show) popup.show(mainStage);
            } else {
                popup.hide();
            }
        });

        // React to settings changes (show/hide toggle)
        DefaultEventBus.getInstance().subscribe(UiSettingsUpdateEvent.class, event ->
                Platform.runLater(() -> {
                    boolean show = UI_SETTINGS.getSettings().isShowExpandButton();
                    if (show && mainStage.isShowing() && !popup.isShowing()) {
                        syncPosition.run();
                        popup.show(mainStage);
                    } else if (!show && popup.isShowing()) {
                        popup.hide();
                    }
                })
        );
    }
}
