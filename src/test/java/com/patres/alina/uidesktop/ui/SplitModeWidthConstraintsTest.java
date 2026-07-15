package com.patres.alina.uidesktop.ui;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SplitModeWidthConstraintsTest {

    @Test
    void restoresFlexibleWidthsAfterLeavingSplitMode() {
        final var splitWidth = new SimpleDoubleProperty(1600);
        final var dashboard = new Pane();
        final var chat = new Pane();

        dashboard.prefWidthProperty().bind(splitWidth.divide(2));
        dashboard.minWidthProperty().bind(splitWidth.divide(2));
        chat.prefWidthProperty().bind(splitWidth.divide(2));
        chat.minWidthProperty().bind(splitWidth.divide(2));

        assertThat(dashboard.getMinWidth()).isEqualTo(800);
        assertThat(chat.getMinWidth()).isEqualTo(800);

        SplitModeWidthConstraints.reset(dashboard, chat);

        assertThat(dashboard.prefWidthProperty().isBound()).isFalse();
        assertThat(dashboard.minWidthProperty().isBound()).isFalse();
        assertThat(dashboard.getPrefWidth()).isEqualTo(Region.USE_COMPUTED_SIZE);
        assertThat(dashboard.getMinWidth()).isZero();
        assertThat(chat.prefWidthProperty().isBound()).isFalse();
        assertThat(chat.minWidthProperty().isBound()).isFalse();
        assertThat(chat.getPrefWidth()).isEqualTo(Region.USE_COMPUTED_SIZE);
        assertThat(chat.getMinWidth()).isZero();
    }
}
