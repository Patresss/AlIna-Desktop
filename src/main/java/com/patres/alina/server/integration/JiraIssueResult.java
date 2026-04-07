package com.patres.alina.server.integration;

import java.util.List;

public record JiraIssueResult(
        List<JiraIssue> issues,
        int totalCount
) {}
