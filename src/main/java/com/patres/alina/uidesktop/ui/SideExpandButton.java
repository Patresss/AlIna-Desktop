package com.patres.alina.uidesktop.ui;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;

/**
 * A floating button placed inside the scene graph on the left edge of the window.
 * Nearly invisible (opacity 0.1) until hovered.
 * Clicking expands the window to the left; clicking again restores it.
 * When {@code autoSplitOnExpand} is enabled in UI settings, expanding also activates
 * split mode (chat left, dashboard right) and collapsing deactivates it.
 */
public class SideExpandButton {

    private static final double BUTTON_WIDTH = 28;
    private static final double BUTTON_HEIGHT = 64;

    private static final String STYLE_BASE =
            "-fx-background-color: -color-accent-emphasis;" +
            "-fx-background-radius: 0 8 8 0;" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 22px;" +
            "-fx-font-weight: bold;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 0;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, -4, 0);";

    private boolean expanded = false;
    private Button button;

    /**
     * Creates the expand button and adds it to the given overlay container.
     * The overlay StackPane should be the scene root wrapping the ApplicationWindow.
     */
    public void attach(Stage mainStage, ApplicationWindow applicationWindow, StackPane overlay) {
        button = new Button("‹");
        button.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setMinSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setMaxSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setStyle(STYLE_BASE);
        button.setFocusTraversable(false);
        button.setPickOnBounds(false);

        // Nearly invisible by default — fully visible on hover
        button.setOpacity(0.1);
        button.setOnMouseEntered(e -> button.setOpacity(1.0));
        button.setOnMouseExited(e -> button.setOpacity(0.1));

        button.setOnAction(e -> {
            var settings = UI_SETTINGS.getSettings();
            var screenBounds = Screen.getPrimary().getVisualBounds();
            int expandBy = settings.resolveExpandWidth();

            if (!expanded) {
                double newX = Math.max(screenBounds.getMinX(), mainStage.getX() - expandBy);
                double actualExpand = mainStage.getX() - newX;
                mainStage.setX(newX);
                mainStage.setWidth(mainStage.getWidth() + actualExpand);
                button.setText("›");
                expanded = true;
                if (settings.isAutoSplitOnExpand()) {
                    Platform.runLater(() -> applicationWindow.setSplitMode(true));
                }
            } else {
                double originalWidth = AssistantAppLauncher.WIDTH;
                double shrinkBy = mainStage.getWidth() - originalWidth;
                if (shrinkBy > 0) {
                    mainStage.setX(mainStage.getX() + shrinkBy);
                    mainStage.setWidth(originalWidth);
                }
                button.setText("‹");
                expanded = false;
                if (settings.isAutoSplitOnExpand()) {
                    Platform.runLater(() -> applicationWindow.setSplitMode(false));
                }
            }
        });

        StackPane.setAlignment(button, Pos.CENTER_LEFT);
        overlay.getChildren().add(button);

        updateVisibility();

        DefaultEventBus.getInstance().subscribe(UiSettingsUpdateEvent.class, event ->
                Platform.runLater(this::updateVisibility)
        );
    }

    private void updateVisibility() {
        boolean show = UI_SETTINGS.getSettings().isShowExpandButton();
        button.setVisible(show);
    }
}
