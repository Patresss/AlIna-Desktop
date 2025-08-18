package com.patres.alina.uidesktop.util.ui;

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

    public static TextArea createResizableTextArea(final Pane root) {
        var node = new TextArea();
        node.prefWidthProperty().bind(root.widthProperty().multiply(WIDTH_OD_PARENT));
        return node;
    }

    public static <T extends Region> T createResizableRegion(final Supplier<T> nodeSupplier, final Pane root) {
        var node = nodeSupplier.get();
        node.prefWidthProperty().bind(root.widthProperty().multiply(WIDTH_OD_PARENT));
        return node;
    }
}
