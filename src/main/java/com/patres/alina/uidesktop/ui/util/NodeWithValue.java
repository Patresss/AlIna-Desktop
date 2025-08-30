package com.patres.alina.uidesktop.ui.util;

import javafx.scene.Node;

import java.util.function.Consumer;
import java.util.function.Supplier;

public record NodeWithValue(Node node,
                            Supplier<String> valueSupplier,
                            Consumer<String> valueConsumer) {
}
