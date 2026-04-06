package com.patres.alina.common.dashboard;

public record DashboardTaskUpdateRequest(
        String sourceFile,
        int lineNumber,
        boolean completed
) {
}
