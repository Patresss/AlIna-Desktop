package com.patres.alina.server.integration;

public record JiraIssue(
        String key,
        String summary,
        String url,
        String status,
        String priority,
        String type
) {}
