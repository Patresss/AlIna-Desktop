package com.patres.alina.common.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DashboardLayoutSettingsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void defaultsPreserveLegacyCardOrderAndPairing() {
        final var layout = new DashboardLayoutSettings();

        assertThat(layout.twoColumnBreakpoint()).isEqualTo(680);
        assertCard(layout, DashboardCardId.MUSIC, 10, false);
        assertCard(layout, DashboardCardId.TASKS, 20, false);
        assertCard(layout, DashboardCardId.UPCOMING_EVENT, 25, true);
        assertCard(layout, DashboardCardId.CALENDAR, 30, true);
        assertCard(layout, DashboardCardId.GITHUB, 40, true);
        assertCard(layout, DashboardCardId.JIRA, 50, true);
        assertCard(layout, DashboardCardId.OBSIDIAN, 60, true);
    }

    @Test
    void oldWorkspaceJsonWithoutLayoutReceivesDefaults() throws Exception {
        final WorkspaceSettings settings = objectMapper.readValue("{}", WorkspaceSettings.class);

        assertThat(settings.dashboardLayout()).isNotNull();
        assertThat(settings.dashboardLayout().twoColumnBreakpoint()).isEqualTo(680);
        assertThat(settings.upcomingEventCard()).isEqualTo(new UpcomingEventCardSettings());
        assertCard(settings.dashboardLayout(), DashboardCardId.UPCOMING_EVENT, 25, true);
        assertCard(settings.dashboardLayout(), DashboardCardId.CALENDAR, 30, true);
    }

    @Test
    void partialMapUsesCardDefaultsAndIgnoresUnknownKeys() throws Exception {
        final String json = """
                {
                  "twoColumnBreakpoint": 900,
                  "cards": {
                    "jira": {"order": 5},
                    "future-widget": {"canUseHalfWidth": true, "order": 1}
                  }
                }
                """;

        final DashboardLayoutSettings layout = objectMapper.readValue(json, DashboardLayoutSettings.class);

        assertThat(layout.twoColumnBreakpoint()).isEqualTo(900);
        assertCard(layout, DashboardCardId.JIRA, 5, true);
        assertCard(layout, DashboardCardId.GITHUB, 40, true);
        assertThat(layout.cards()).doesNotContainKey("future-widget");
    }

    @Test
    void clampsPersistedValuesToSupportedRanges() {
        final var layout = new DashboardLayoutSettings(
                20_000,
                Map.of(
                        DashboardCardId.MUSIC.key(), new DashboardCardLayoutSettings(true, -5),
                        DashboardCardId.JIRA.key(), new DashboardCardLayoutSettings(false, 20_000)
                )
        );

        assertThat(layout.twoColumnBreakpoint()).isEqualTo(DashboardLayoutSettings.MAX_TWO_COLUMN_BREAKPOINT);
        assertCard(layout, DashboardCardId.MUSIC, DashboardLayoutSettings.MIN_CARD_ORDER, true);
        assertCard(layout, DashboardCardId.JIRA, DashboardLayoutSettings.MAX_CARD_ORDER, false);
    }

    @Test
    void preservesLayoutAcrossWorkspaceWithers() {
        final var customLayout = new DashboardLayoutSettings(
                1_100,
                Map.of(DashboardCardId.TASKS.key(), new DashboardCardLayoutSettings(true, 3))
        );
        final WorkspaceSettings source = workspaceSettingsWithLayout(customLayout);

        assertThat(source.withKeepWindowAlwaysOnTop(false).dashboardLayout()).isEqualTo(customLayout);
        assertThat(source.withDashboardCollapsed(true).dashboardLayout()).isEqualTo(customLayout);
        assertThat(source.withSplitMode(true).dashboardLayout()).isEqualTo(customLayout);
    }

    @Test
    void exposesAnImmutableNormalizedMap() {
        final var layout = new DashboardLayoutSettings();

        assertThatThrownBy(() -> layout.cards().put(
                "custom",
                new DashboardCardLayoutSettings(true, 1)
        )).isInstanceOf(UnsupportedOperationException.class);
    }

    private static void assertCard(DashboardLayoutSettings layout,
                                   DashboardCardId id,
                                   int expectedOrder,
                                   boolean expectedHalfWidth) {
        final DashboardCardLayoutSettings card = layout.card(id);
        assertThat(card.order()).isEqualTo(expectedOrder);
        assertThat(card.canUseHalfWidth()).isEqualTo(expectedHalfWidth);
    }

    private static WorkspaceSettings workspaceSettingsWithLayout(DashboardLayoutSettings dashboardLayout) {
        final WorkspaceSettings defaults = new WorkspaceSettings();
        return new WorkspaceSettings(
                defaults.showDashboard(),
                defaults.dashboardCollapsed(),
                defaults.keepWindowAlwaysOnTop(),
                defaults.tasksFile(),
                defaults.dashboardTaskLimit(),
                defaults.taskGroups(),
                defaults.openCodeHostname(),
                defaults.openCodePort(),
                defaults.openCodeWorkingDirectory(),
                defaults.githubToken(),
                defaults.dashboardTasksRefreshSeconds(),
                defaults.dashboardGithubRefreshSeconds(),
                defaults.dashboardMediaRefreshSeconds(),
                defaults.dashboardGithubPrLimit(),
                defaults.dashboardJiraRefreshSeconds(),
                defaults.dashboardJiraIssueLimit(),
                defaults.jiraEmail(),
                defaults.jiraApiToken(),
                defaults.showDashboardMusic(),
                defaults.showDashboardTasks(),
                defaults.showDashboardGithub(),
                defaults.showDashboardJira(),
                defaults.showDashboardCalendar(),
                defaults.dashboardCalendarRefreshSeconds(),
                defaults.calendarHideAllDayEvents(),
                defaults.calendarShowOnlyCurrentAndFuture(),
                defaults.calendarNotificationsEnabled(),
                defaults.calendarNotificationMinutesBefore(),
                defaults.calendarChangeNotificationsEnabled(),
                defaults.githubChangeNotificationsEnabled(),
                defaults.jiraChangeNotificationsEnabled(),
                defaults.splitMode(),
                defaults.calendarAiPrompt(),
                defaults.tasksAiPrompt(),
                defaults.jiraAiPrompt(),
                defaults.githubAiPrompt(),
                defaults.showDashboardObsidian(),
                defaults.obsidianCliPath(),
                defaults.dashboardObsidianNoteLimit(),
                defaults.dashboardObsidianRefreshSeconds(),
                defaults.obsidianChangeNotificationsEnabled(),
                defaults.obsidianAiPrompt(),
                defaults.obsidianExcludePatterns(),
                defaults.agentBackend(),
                defaults.codexCommand(),
                defaults.codexWorkingDirectory(),
                defaults.upcomingEventCard(),
                dashboardLayout
        );
    }
}
