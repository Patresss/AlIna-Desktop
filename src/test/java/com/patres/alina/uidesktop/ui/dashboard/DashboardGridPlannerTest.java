package com.patres.alina.uidesktop.ui.dashboard;

import com.patres.alina.common.settings.DashboardCardId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardGridPlannerTest {

    @Test
    void reproducesDefaultWideLayout() {
        final List<DashboardGridPlanner.Placement> placements = DashboardGridPlanner.plan(
                List.of(
                        card(DashboardCardId.MUSIC, false, 10),
                        card(DashboardCardId.TASKS, false, 20),
                        card(DashboardCardId.CALENDAR, true, 30),
                        card(DashboardCardId.GITHUB, true, 40),
                        card(DashboardCardId.JIRA, true, 50),
                        card(DashboardCardId.OBSIDIAN, true, 60)
                ),
                DashboardLayoutMode.TWO_COLUMNS
        );

        assertThat(placements).containsExactly(
                placement(DashboardCardId.MUSIC, 0, 0, 2),
                placement(DashboardCardId.TASKS, 1, 0, 2),
                placement(DashboardCardId.CALENDAR, 2, 0, 1),
                placement(DashboardCardId.GITHUB, 2, 1, 1),
                placement(DashboardCardId.JIRA, 3, 0, 1),
                placement(DashboardCardId.OBSIDIAN, 3, 1, 1)
        );
    }

    @Test
    void sortsByConfiguredOrderAndDefaultOrderForTies() {
        final List<DashboardGridPlanner.Placement> placements = DashboardGridPlanner.plan(
                List.of(
                        card(DashboardCardId.GITHUB, false, 5),
                        card(DashboardCardId.MUSIC, false, 5),
                        card(DashboardCardId.JIRA, false, 1)
                ),
                DashboardLayoutMode.TWO_COLUMNS
        );

        assertThat(placements)
                .extracting(DashboardGridPlanner.Placement::id)
                .containsExactly(DashboardCardId.JIRA, DashboardCardId.MUSIC, DashboardCardId.GITHUB);
    }

    @Test
    void fullWidthCardFlushesPendingHalfCardWithoutReordering() {
        final List<DashboardGridPlanner.Placement> placements = DashboardGridPlanner.plan(
                List.of(
                        card(DashboardCardId.CALENDAR, true, 30),
                        card(DashboardCardId.TASKS, false, 35),
                        card(DashboardCardId.GITHUB, true, 40)
                ),
                DashboardLayoutMode.TWO_COLUMNS
        );

        assertThat(placements).containsExactly(
                placement(DashboardCardId.CALENDAR, 0, 0, 2),
                placement(DashboardCardId.TASKS, 1, 0, 2),
                placement(DashboardCardId.GITHUB, 2, 0, 2)
        );
    }

    @Test
    void finalUnpairedHalfCardSpansBothColumns() {
        final List<DashboardGridPlanner.Placement> placements = DashboardGridPlanner.plan(
                List.of(card(DashboardCardId.JIRA, true, 10)),
                DashboardLayoutMode.TWO_COLUMNS
        );

        assertThat(placements).containsExactly(placement(DashboardCardId.JIRA, 0, 0, 2));
    }

    @Test
    void oneColumnIgnoresHalfWidthEligibilityAndKeepsSortedOrder() {
        final List<DashboardGridPlanner.Placement> placements = DashboardGridPlanner.plan(
                List.of(
                        card(DashboardCardId.GITHUB, true, 20),
                        card(DashboardCardId.CALENDAR, true, 10)
                ),
                DashboardLayoutMode.SINGLE_COLUMN
        );

        assertThat(placements).containsExactly(
                placement(DashboardCardId.CALENDAR, 0, 0, 1),
                placement(DashboardCardId.GITHUB, 1, 0, 1)
        );
    }

    @Test
    void omittedHiddenCardDoesNotReserveAPlace() {
        final List<DashboardGridPlanner.Placement> placements = DashboardGridPlanner.plan(
                List.of(
                        card(DashboardCardId.CALENDAR, true, 30),
                        card(DashboardCardId.JIRA, true, 50)
                ),
                DashboardLayoutMode.TWO_COLUMNS
        );

        assertThat(placements).containsExactly(
                placement(DashboardCardId.CALENDAR, 0, 0, 1),
                placement(DashboardCardId.JIRA, 0, 1, 1)
        );
    }

    private static DashboardGridPlanner.Card card(DashboardCardId id,
                                                   boolean canUseHalfWidth,
                                                   int order) {
        return new DashboardGridPlanner.Card(id, canUseHalfWidth, order);
    }

    private static DashboardGridPlanner.Placement placement(DashboardCardId id,
                                                             int row,
                                                             int column,
                                                             int span) {
        return new DashboardGridPlanner.Placement(id, row, column, span);
    }
}
