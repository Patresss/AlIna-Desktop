package com.patres.alina.uidesktop.ui.dashboard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardHeightPolicyTest {

    @Test
    void usesPreferredHeightWhenDashboardFitsAboveChat() {
        assertThat(DashboardHeightPolicy.resolve(900, 540, false)).isEqualTo(540);
    }

    @Test
    void reservesUsableChatWorkspaceWhenDashboardIsTooTall() {
        assertThat(DashboardHeightPolicy.resolve(700, 800, false))
                .isEqualTo(700 - DashboardHeightPolicy.MINIMUM_CHAT_WORKSPACE);
    }

    @Test
    void usesCompactHeightWhenCollapsed() {
        assertThat(DashboardHeightPolicy.resolve(900, 700, true))
                .isEqualTo(DashboardHeightPolicy.COLLAPSED_HEIGHT);
    }
}
