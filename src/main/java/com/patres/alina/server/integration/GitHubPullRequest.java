package com.patres.alina.server.integration;

public record GitHubPullRequest(
        int number,
        String title,
        String url,
        String repository,
        String author,
        boolean draft
) {}
