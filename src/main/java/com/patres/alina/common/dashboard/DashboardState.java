package com.patres.alina.common.dashboard;

import java.util.List;

public record DashboardState(
        boolean visible,
        boolean collapsed,
        List<DashboardTask> tasks,
        List<String> configuredGroups
) {
}
