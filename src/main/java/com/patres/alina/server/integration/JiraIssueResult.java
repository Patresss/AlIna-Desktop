package com.patres.alina.server.integration;

import java.util.Collections;
import java.util.List;

public record JiraIssueResult(
        List<JiraIssue> issues,
        int totalCount,
        boolean fetchError
) {
    /** Convenience factory for a successful (non-error) empty result. */
    public static JiraIssueResult empty() {
        return new JiraIssueResult(Collections.emptyList(), 0, false);
    }

    /** Factory for results that failed due to a connection / API error. */
    public static JiraIssueResult error() {
        return new JiraIssueResult(Collections.emptyList(), 0, true);
    }
}
