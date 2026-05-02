package com.patres.alina.uidesktop.ui.toast;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * A lightweight toast notification that appears at the bottom of a StackPane
 * and auto-dismisses after a short duration.
 */
public final class ToastNotification {

    private ToastNotification() {
    }

    /**
     * Shows a success toast notification overlaid on the given container.
     *
     * @param container the StackPane to display the toast in
     * @param message   the message to display
     */
    public static void showSuccess(StackPane container, String message) {
        show(container, message, "toast-success");
    }

    /**
     * Shows an info toast notification overlaid on the given container.
     *
     * @param container the StackPane to display the toast in
     * @param message   the message to display
     */
    public static void showInfo(StackPane container, String message) {
        show(container, message, "toast-info");
    }

    private static void show(StackPane container, String message, String styleClass) {
        Platform.runLater(() -> {
            var icon = new FontIcon(Feather.CHECK_CIRCLE);
            icon.getStyleClass().add("toast-icon");

            var label = new Label(message);
            label.getStyleClass().add("toast-label");

            var toast = new HBox(8, icon, label);
            toast.getStyleClass().addAll("toast-notification", styleClass);
            toast.setAlignment(Pos.CENTER);
            toast.setMaxWidth(Double.MAX_VALUE);
            toast.setMaxHeight(Double.MAX_VALUE);
            toast.setMouseTransparent(true);
            toast.setOpacity(0);

            StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);

            container.getChildren().add(toast);

            var fadeIn = new FadeTransition(Duration.millis(200), toast);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            var pause = new PauseTransition(Duration.seconds(2));

            var fadeOut = new FadeTransition(Duration.millis(400), toast);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            var sequence = new SequentialTransition(fadeIn, pause, fadeOut);
            sequence.setOnFinished(_ -> container.getChildren().remove(toast));
            sequence.play();
        });
    }
}
