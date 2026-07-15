package com.patres.alina.uidesktop.ui;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
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

    private boolean expanded = false;
    private Button button;
    private Stage mainStage;
    private ApplicationWindow applicationWindow;

    /**
     * Creates the expand button and adds it to the given overlay container.
     * The overlay StackPane should be the scene root wrapping the ApplicationWindow.
     */
    public void attach(Stage mainStage, ApplicationWindow applicationWindow, StackPane overlay) {
        this.mainStage = mainStage;
        this.applicationWindow = applicationWindow;

        button = new Button("‹");
        button.setPrefSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setMinSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.setMaxSize(BUTTON_WIDTH, BUTTON_HEIGHT);
        button.getStyleClass().add("side-expand-button");
        button.setAccessibleText(com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString("window.expand"));
        Tooltip.install(button, new Tooltip(
                com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString("window.expand")
        ));
        button.setFocusTraversable(false);
        button.setPickOnBounds(false);

        // Quiet by default, but still discoverable and fully visible on hover.
        button.setOpacity(0.38);
        button.setOnMouseEntered(e -> button.setOpacity(1.0));
        button.setOnMouseExited(e -> button.setOpacity(0.38));

        button.setOnAction(e -> {
            if (!expanded) {
                expand();
            } else {
                shrink();
            }
        });

        StackPane.setAlignment(button, Pos.CENTER_LEFT);
        overlay.getChildren().add(button);

        updateVisibility();

        DefaultEventBus.getInstance().subscribe(UiSettingsUpdateEvent.class, event ->
                Platform.runLater(this::updateVisibility)
        );
    }

    public void expand() {
        if (expanded || mainStage == null) {
            return;
        }
        var settings = UI_SETTINGS.getSettings();
        var screenBounds = Screen.getPrimary().getVisualBounds();
        int expandBy = settings.resolveExpandWidth();

        double newX = Math.max(screenBounds.getMinX(), mainStage.getX() - expandBy);
        double actualExpand = mainStage.getX() - newX;
        mainStage.setX(newX);
        mainStage.setWidth(mainStage.getWidth() + actualExpand);
        if (button != null) {
            button.setText("›");
            updateTooltip("window.shrink");
        }
        expanded = true;
        if (settings.isAutoSplitOnExpand()) {
            Platform.runLater(() -> applicationWindow.setSplitMode(true));
        }
        applicationWindow.refreshDashboardLayoutAfterWindowResize();
    }

    public void shrink() {
        if (!expanded || mainStage == null) {
            return;
        }
        var settings = UI_SETTINGS.getSettings();
        double originalWidth = AssistantAppLauncher.WIDTH;
        double shrinkBy = mainStage.getWidth() - originalWidth;
        if (shrinkBy > 0) {
            mainStage.setX(mainStage.getX() + shrinkBy);
            mainStage.setWidth(originalWidth);
        }
        if (button != null) {
            button.setText("‹");
            updateTooltip("window.expand");
        }
        expanded = false;
        if (settings.isAutoSplitOnExpand()) {
            Platform.runLater(() -> applicationWindow.setSplitMode(false));
        }
        applicationWindow.refreshDashboardLayoutAfterWindowResize();
    }

    private void updateVisibility() {
        boolean show = UI_SETTINGS.getSettings().isShowExpandButton();
        button.setVisible(show);
    }

    private void updateTooltip(String key) {
        final String text = com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString(key);
        button.setAccessibleText(text);
        if (button.getTooltip() != null) {
            button.getTooltip().setText(text);
        }
    }
}
