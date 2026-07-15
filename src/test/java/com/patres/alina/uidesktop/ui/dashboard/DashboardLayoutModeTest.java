package com.patres.alina.uidesktop.ui.dashboard;

import org.junit.jupiter.api.Test;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.VPos;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardLayoutModeTest {

    @Test
    void usesSingleColumnBelowBreakpoint() {
        assertThat(DashboardLayoutMode.forWidth(DashboardLayoutMode.TWO_COLUMN_BREAKPOINT - 1))
                .isEqualTo(DashboardLayoutMode.SINGLE_COLUMN);
    }

    @Test
    void usesBentoGridAtBreakpoint() {
        assertThat(DashboardLayoutMode.forWidth(DashboardLayoutMode.TWO_COLUMN_BREAKPOINT))
                .isEqualTo(DashboardLayoutMode.TWO_COLUMNS);
    }

    @Test
    void usesConfiguredBreakpoint() {
        assertThat(DashboardLayoutMode.forWidth(899, 900))
                .isEqualTo(DashboardLayoutMode.SINGLE_COLUMN);
        assertThat(DashboardLayoutMode.forWidth(900, 900))
                .isEqualTo(DashboardLayoutMode.TWO_COLUMNS);
        assertThat(DashboardLayoutMode.forWidth(901, 900))
                .isEqualTo(DashboardLayoutMode.TWO_COLUMNS);
    }

    @Test
    void recalculatesLayoutAcrossExpandShrinkCycle() {
        assertThat(List.of(
                DashboardLayoutMode.forWidth(1200),
                DashboardLayoutMode.forWidth(620),
                DashboardLayoutMode.forWidth(1200)
        )).containsExactly(
                DashboardLayoutMode.TWO_COLUMNS,
                DashboardLayoutMode.SINGLE_COLUMN,
                DashboardLayoutMode.TWO_COLUMNS
        );
    }

    @Test
    void keepsTwoColumnsExactlyEqualAfterSubtractingGap() {
        final var gridWidth = new SimpleDoubleProperty(1315);
        final var left = DashboardContainer.equalColumn(gridWidth, 10, 2);
        final var right = DashboardContainer.equalColumn(gridWidth, 10, 2);

        assertThat(left.getPrefWidth()).isEqualTo(652.5);
        assertThat(right.getPrefWidth()).isEqualTo(652.5);
        assertThat(left.getMaxWidth()).isEqualTo(652.5);
        assertThat(right.getMaxWidth()).isEqualTo(652.5);

        gridWidth.set(760);

        assertThat(left.getPrefWidth()).isEqualTo(375);
        assertThat(right.getPrefWidth()).isEqualTo(375);
    }

    @Test
    void laysOutCardsInEqualWidthTracksDespiteDifferentContentPreferences() {
        final var grid = new GridPane();
        grid.setHgap(10);
        grid.getColumnConstraints().addAll(
                DashboardContainer.equalColumn(grid.widthProperty(), 10, 2),
                DashboardContainer.equalColumn(grid.widthProperty(), 10, 2)
        );

        final var calendar = new Pane();
        calendar.setMinWidth(0);
        calendar.setPrefWidth(900);
        calendar.setMaxWidth(Double.MAX_VALUE);

        final var github = new Pane();
        github.setMinWidth(0);
        github.setPrefWidth(300);
        github.setMaxWidth(Double.MAX_VALUE);

        grid.add(calendar, 0, 0);
        grid.add(github, 1, 0);
        grid.resize(1315, 100);
        grid.layout();

        // JavaFX snaps tracks to physical pixels, so an odd available width
        // can differ by at most one pixel while remaining visually 50/50.
        assertThat(Math.abs(calendar.getWidth() - github.getWidth())).isLessThanOrEqualTo(1);
        assertThat(calendar.getWidth() + github.getWidth()).isEqualTo(1305);
        assertThat(github.getLayoutX()).isEqualTo(calendar.getWidth() + 10);
    }

    @Test
    void stretchesShortCardToTallCardHeightInSharedRow() {
        final var grid = new GridPane();
        grid.setHgap(10);
        grid.getColumnConstraints().addAll(
                DashboardContainer.equalColumn(grid.widthProperty(), 10, 2),
                DashboardContainer.equalColumn(grid.widthProperty(), 10, 2)
        );

        final var tall = new Pane();
        tall.setPrefHeight(300);
        final var shortCard = new Pane();
        shortCard.setPrefHeight(100);
        configureEqualRowHeight(tall);
        configureEqualRowHeight(shortCard);

        grid.add(tall, 0, 0);
        grid.add(shortCard, 1, 0);
        grid.resize(1_010, 300);
        grid.layout();

        assertThat(tall.getHeight()).isEqualTo(300);
        assertThat(shortCard.getHeight()).isEqualTo(300);
        assertThat(shortCard.getLayoutY()).isZero();
    }

    @Test
    void keepsNaturalHeightForCardInItsOwnRow() {
        final var grid = new GridPane();
        final var card = new Pane();
        card.setPrefHeight(100);
        configureEqualRowHeight(card);

        grid.add(card, 0, 0);
        grid.resize(500, 300);
        grid.layout();

        assertThat(card.getHeight()).isEqualTo(100);
    }

    private static void configureEqualRowHeight(Pane card) {
        card.setMinWidth(0);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMaxHeight(Double.MAX_VALUE);
        GridPane.setHgrow(card, Priority.ALWAYS);
        GridPane.setVgrow(card, Priority.NEVER);
        GridPane.setFillHeight(card, true);
        GridPane.setValignment(card, VPos.TOP);
    }
}
