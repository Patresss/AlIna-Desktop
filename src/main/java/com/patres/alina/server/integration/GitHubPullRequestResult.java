package com.patres.alina.server.integration;

import java.util.List;

/**
 * Result of GitHub PR search containing the list of PRs and total count.
 */
public record GitHubPullRequestResult(
        List<GitHubPullRequest> pullRequests,
        int totalCount
) {
}
