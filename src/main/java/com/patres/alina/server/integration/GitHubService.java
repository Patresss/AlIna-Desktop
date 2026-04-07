package com.patres.alina.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class GitHubService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);

    private static final String USER_URL = "https://api.github.com/user";
    private static final String SEARCH_URL_TEMPLATE =
            "https://api.github.com/search/issues?q=type:pr+state:open+review-requested:%s&sort=updated&order=desc&per_page=10";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private GitHubService() {
        // utility class
    }

    public static List<GitHubPullRequest> fetchPendingReviews(final String githubToken) {
        if (githubToken == null || githubToken.isBlank()) {
            return Collections.emptyList();
        }

        final String tokenPrefix = githubToken.length() > 10 ? githubToken.substring(0, 10) + "..." : "***";
        logger.info("GitHub: starting fetch with token: {}", tokenPrefix);

        try {
            final HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();

            final String username = fetchAuthenticatedUsername(client, githubToken);
            if (username == null) {
                logger.warn("Could not resolve GitHub username — check if the token is valid");
                return Collections.emptyList();
            }
            logger.info("GitHub: resolved username = {}", username);

            final String searchUrl = String.format(SEARCH_URL_TEMPLATE, username);
            logger.info("GitHub: search URL = {}", searchUrl);

            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("Authorization", "Bearer " + githubToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("GitHub: search response status = {}, body length = {}", response.statusCode(), response.body().length());

            if (response.statusCode() != 200) {
                logger.warn("GitHub API returned status {}: {}. Token used: {}. " +
                        "If 401, verify: 1) Token is valid and not expired, 2) Token has 'repo' or 'public_repo' scope, " +
                        "3) You've restarted the app after updating the token in settings",
                        response.statusCode(), response.body(), tokenPrefix);
                return Collections.emptyList();
            }

            final List<GitHubPullRequest> result = parseResponse(response.body());
            logger.info("GitHub: parsed {} pull requests", result.size());
            return result;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("GitHub API call interrupted", e);
            return Collections.emptyList();
        } catch (final Exception e) {
            logger.warn("Failed to fetch GitHub pull requests", e);
            return Collections.emptyList();
        }
    }

    private static String fetchAuthenticatedUsername(final HttpClient client, final String githubToken) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_URL))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(TIMEOUT)
                .GET()
                .build();

        final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            logger.warn("GitHub /user API returned status {}: {}", response.statusCode(), response.body());
            return null;
        }

        final JsonNode root = OBJECT_MAPPER.readTree(response.body());
        final JsonNode login = root.get("login");
        if (login == null || !login.isTextual() || login.asText().isBlank()) {
            return null;
        }
        return login.asText();
    }

    private static List<GitHubPullRequest> parseResponse(final String json) throws Exception {
        final JsonNode root = OBJECT_MAPPER.readTree(json);
        final JsonNode totalCount = root.get("total_count");
        logger.info("GitHub: total_count = {}", totalCount);
        final JsonNode items = root.get("items");

        if (items == null || !items.isArray()) {
            return Collections.emptyList();
        }

        final List<GitHubPullRequest> pullRequests = new ArrayList<>();
        for (final JsonNode item : items) {
            final String title = getTextOrDefault(item, "title", "Untitled");
            final String url = getTextOrDefault(item, "html_url", "");
            final String repositoryUrl = getTextOrDefault(item, "repository_url", "");
            final String repository = extractRepositoryName(repositoryUrl);
            final String author = item.has("user") && item.get("user").has("login")
                    ? item.get("user").get("login").asText()
                    : "unknown";
            final boolean draft = item.has("draft") && item.get("draft").asBoolean(false);

            pullRequests.add(new GitHubPullRequest(title, url, repository, author, draft));
        }
        return Collections.unmodifiableList(pullRequests);
    }

    private static String getTextOrDefault(final JsonNode node, final String field, final String defaultValue) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText(defaultValue);
        }
        return defaultValue;
    }

    /**
     * Extracts "owner/repo" from a GitHub API repository URL like
     * "https://api.github.com/repos/owner/repo".
     */
    private static String extractRepositoryName(final String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return "unknown";
        }
        final String marker = "/repos/";
        final int index = repositoryUrl.indexOf(marker);
        if (index >= 0) {
            return repositoryUrl.substring(index + marker.length());
        }
        return repositoryUrl;
    }
}
