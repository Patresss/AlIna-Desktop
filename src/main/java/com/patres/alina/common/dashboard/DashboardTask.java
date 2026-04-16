package com.patres.alina.common.dashboard;

public record DashboardTask(
        String title,
        String sourceFile,
        int lineNumber,
        String group
) {
}
