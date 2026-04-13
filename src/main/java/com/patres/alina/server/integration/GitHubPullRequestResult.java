package com.patres.alina.server.integration;

import java.util.Collections;
import java.util.List;

/**
 * Result of GitHub PR search containing the list of PRs and total count.
 */
public record GitHubPullRequestResult(
        List<GitHubPullRequest> pullRequests,
        int totalCount,
        boolean fetchError
) {
    /** Convenience factory for a successful (non-error) empty result. */
    public static GitHubPullRequestResult empty() {
        return new GitHubPullRequestResult(Collections.emptyList(), 0, false);
    }

    /** Factory for results that failed due to a connection / API error. */
    public static GitHubPullRequestResult error() {
        return new GitHubPullRequestResult(Collections.emptyList(), 0, true);
    }
}
