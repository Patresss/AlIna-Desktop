package com.patres.alina.server.dashboard;

import com.patres.alina.common.dashboard.DashboardState;
import com.patres.alina.common.dashboard.DashboardTaskUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(final DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    public DashboardState getState() {
        return dashboardService.getState();
    }

    public void updateTask(final DashboardTaskUpdateRequest request) {
        dashboardService.updateTask(request);
    }
}
