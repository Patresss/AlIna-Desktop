package com.patres.alina.server.integration;

public record GitHubPullRequest(
        String title,
        String url,
        String repository,
        String author,
        boolean draft
) {}
