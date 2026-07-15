package com.patres.alina.uidesktop.ui.dashboard;

import com.patres.alina.common.settings.DashboardCardId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Deterministic row planner for ordered full-width and half-width cards. */
final class DashboardGridPlanner {

    private DashboardGridPlanner() {
    }

    static List<Placement> plan(List<Card> cards, DashboardLayoutMode layoutMode) {
        final List<Card> sortedCards = cards.stream()
                .sorted(Comparator.comparingInt(Card::order)
                        .thenComparingInt(card -> card.id().defaultOrder()))
                .toList();

        if (layoutMode == DashboardLayoutMode.SINGLE_COLUMN) {
            final List<Placement> placements = new ArrayList<>(sortedCards.size());
            for (int row = 0; row < sortedCards.size(); row++) {
                placements.add(new Placement(sortedCards.get(row).id(), row, 0, 1));
            }
            return List.copyOf(placements);
        }
        return planTwoColumns(sortedCards);
    }

    private static List<Placement> planTwoColumns(List<Card> sortedCards) {
        final List<Placement> placements = new ArrayList<>(sortedCards.size());
        Card pendingHalfWidth = null;
        int row = 0;

        for (Card card : sortedCards) {
            if (card.canUseHalfWidth()) {
                if (pendingHalfWidth == null) {
                    pendingHalfWidth = card;
                } else {
                    placements.add(new Placement(pendingHalfWidth.id(), row, 0, 1));
                    placements.add(new Placement(card.id(), row, 1, 1));
                    pendingHalfWidth = null;
                    row++;
                }
                continue;
            }

            if (pendingHalfWidth != null) {
                placements.add(new Placement(pendingHalfWidth.id(), row, 0, 2));
                pendingHalfWidth = null;
                row++;
            }
            placements.add(new Placement(card.id(), row, 0, 2));
            row++;
        }

        if (pendingHalfWidth != null) {
            placements.add(new Placement(pendingHalfWidth.id(), row, 0, 2));
        }
        return List.copyOf(placements);
    }

    record Card(DashboardCardId id, boolean canUseHalfWidth, int order) {
    }

    record Placement(DashboardCardId id, int row, int column, int columnSpan) {
    }
}
