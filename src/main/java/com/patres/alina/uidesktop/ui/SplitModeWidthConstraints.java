package com.patres.alina.uidesktop.ui;

import javafx.scene.layout.Region;

/**
 * Restores flexible widths after nodes leave the split-mode {@code HBox}.
 *
 * <p>JavaFX keeps the last calculated value when a property is unbound. A
 * region that was bound to half of a wide split container would therefore
 * retain that value as its minimum width and overflow the normal workspace.</p>
 */
final class SplitModeWidthConstraints {

    private SplitModeWidthConstraints() {
    }

    static void reset(Region dashboard, Region chat) {
        reset(dashboard);
        reset(chat);
    }

    private static void reset(Region region) {
        region.prefWidthProperty().unbind();
        region.minWidthProperty().unbind();
        region.setPrefWidth(Region.USE_COMPUTED_SIZE);
        region.setMinWidth(0);
    }
}
