package com.patres.alina.uidesktop.util.ui;

import javafx.scene.control.PasswordField;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import java.util.function.Supplier;

public class ResizableNodeUtils {

    public static final double WIDTH_OD_PARENT = 0.5;

    public static TextField createResizableTextField(final Pane root) {
        var node = new TextField();
        node.prefWidthProperty().bind(root.widthProperty().multiply(WIDTH_OD_PARENT));
        return node;
    }

    public static PasswordField createResizablePasswordField(final Pane root) {
        var node = new PasswordField();
        node.prefWidthProperty().bind(root.widthProperty().multiply(WIDTH_OD_PARENT));
        return node;
    }

    public static TextArea createResizableTextArea(final Pane root) {
        var node = new TextArea();
        node.prefWidthProperty().bind(root.widthProperty().multiply(WIDTH_OD_PARENT));
        return node;
    }

    public static Spinner<Integer> createResizableEditableSpinner(final int min, final int max, final int initialValue, final Pane root) {
        var spinner = new Spinner<Integer>(min, max, initialValue);
        spinner.setEditable(true);
        spinner.prefWidthProperty().bind(root.widthProperty().multiply(WIDTH_OD_PARENT));
        spinner.focusedProperty().addListener((_, _, focused) -> {
            if (!focused) {
                commitSpinnerValue(spinner, min, max);
            }
        });
        return spinner;
    }

    private static void commitSpinnerValue(final Spinner<Integer> spinner, final int min, final int max) {
        try {
            final int value = Integer.parseInt(spinner.getEditor().getText().trim());
            spinner.getValueFactory().setValue(Math.max(min, Math.min(max, value)));
        } catch (NumberFormatException e) {
            spinner.getEditor().setText(String.valueOf(spinner.getValue()));
        }
    }

    public static <T extends Region> T createResizableRegion(final Supplier<T> nodeSupplier, final Pane root) {
        var node = nodeSupplier.get();
        node.prefWidthProperty().bind(root.widthProperty().multiply(WIDTH_OD_PARENT));
        return node;
    }
}
