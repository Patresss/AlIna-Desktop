package com.patres.alina.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.server.integration.http.HttpClientFactory;
import com.patres.alina.server.integration.http.HttpResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GitHubService {

    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);

    private static final String USER_URL = "https://api.github.com/user";
    private static final String SEARCH_URL_TEMPLATE =
            "https://api.github.com/search/issues?q=type:pr+state:open+involves:%s+-author:%s&sort=updated&order=desc&per_page=50";
    private static final String PR_DETAILS_URL_TEMPLATE = "https://api.github.com/repos/%s/pulls/%s";
    private static final String PR_REVIEWS_URL_TEMPLATE = "https://api.github.com/repos/%s/pulls/%s/reviews";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public GitHubService(final HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getClient();
    }

    public GitHubPullRequestResult fetchPendingReviews(final String githubToken, final int maxResults) {
        if (githubToken == null || githubToken.isBlank()) {
            return GitHubPullRequestResult.empty();
        }

        final String tokenPrefix = githubToken.length() > 10 ? githubToken.substring(0, 10) + "..." : "***";
        logger.info("GitHub: starting fetch with token: {}, maxResults: {}", tokenPrefix, maxResults);

            try {
                final String username = fetchAuthenticatedUsername(githubToken);
                if (username == null) {
                    logger.warn("Could not resolve GitHub username — check if the token is valid");
                    return GitHubPullRequestResult.error();
                }
            logger.info("GitHub: resolved username = {}", username);

            final String searchUrl = String.format(SEARCH_URL_TEMPLATE, username, username);
            logger.info("GitHub: search URL = {}", searchUrl);

            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("Authorization", "Bearer " + githubToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("GitHub: search response status = {}, body length = {}", response.statusCode(), response.body().length());

            if (!HttpResponseHandler.isSuccess(response)) {
                HttpResponseHandler.describeError("GitHub", response);
                logger.warn("GitHub API returned status {}: {}. Token used: {}. " +
                        "If 401, verify: 1) Token is valid and not expired, 2) Token has 'repo' or 'public_repo' scope, " +
                        "3) You've restarted the app after updating the token in settings",
                        response.statusCode(), response.body(), tokenPrefix);
                return GitHubPullRequestResult.error();
            }

            final List<GitHubPullRequest> allPRs = parseResponse(response.body());
            logger.info("GitHub: found {} pull requests from search", allPRs.size());
            
            // Filter to only PRs that need review from user
            final GitHubPullRequestResult result = filterPRsNeedingReview(githubToken, username, allPRs, maxResults);
            logger.info("GitHub: filtered to {} pull requests needing review (total: {})", 
                    result.pullRequests().size(), result.totalCount());
            
            return result;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("GitHub API call interrupted", e);
            return GitHubPullRequestResult.error();
        } catch (final Exception e) {
            logger.warn("Failed to fetch GitHub pull requests", e);
            return GitHubPullRequestResult.error();
        }
    }

    private String fetchAuthenticatedUsername(final String githubToken) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(USER_URL))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(TIMEOUT)
                .GET()
                .build();

        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (!HttpResponseHandler.isSuccess(response)) {
            HttpResponseHandler.describeError("GitHub", response);
            return null;
        }

        final JsonNode root = OBJECT_MAPPER.readTree(response.body());
        final JsonNode login = root.get("login");
        if (login == null || !login.isTextual() || login.asText().isBlank()) {
            return null;
        }
        return login.asText();
    }

    /**
     * Filters PRs to only include those that need review from the user:
     * - User is directly requested as reviewer (not via team), OR
     * - User has reviewed but last review is not APPROVED (COMMENTED, CHANGES_REQUESTED, DISMISSED)
     * Returns result with limited number of PRs and total count of all matching PRs.
     */
    private GitHubPullRequestResult filterPRsNeedingReview(
            final String githubToken,
            final String username,
            final List<GitHubPullRequest> allPRs,
            final int maxResults) {
        
        final List<GitHubPullRequest> allMatching = new ArrayList<>();
        final List<GitHubPullRequest> limited = new ArrayList<>();
        
        for (final GitHubPullRequest pr : allPRs) {
            try {
                if (needsReviewFromUser(githubToken, username, pr)) {
                    allMatching.add(pr);
                    if (limited.size() < maxResults) {
                        limited.add(pr);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to check review status for PR: {}", pr.url(), e);
                // Include PR in case of error to be safe
                allMatching.add(pr);
                if (limited.size() < maxResults) {
                    limited.add(pr);
                }
            }
        }
        
        return new GitHubPullRequestResult(
                Collections.unmodifiableList(limited),
                allMatching.size(),
                false
        );
    }

    /**
     * Checks if PR needs review from user:
     * - User is in requested_reviewers (directly, not via team), OR
     * - User has reviewed but last review state is not APPROVED
     */
    private boolean needsReviewFromUser(
            final String githubToken,
            final String username,
            final GitHubPullRequest pr) throws Exception {
        
        // Extract repo and PR number from repository and URL
        final String repo = pr.repository();
        final String prNumber = extractPRNumber(pr.url());
        
        if (repo == null || prNumber == null) {
            logger.warn("Could not extract repo/PR number from: {} / {}", repo, pr.url());
            return true; // Include by default if we can't check
        }
        
        final String detailsUrl = String.format(PR_DETAILS_URL_TEMPLATE, repo, prNumber);
        
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(detailsUrl))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(TIMEOUT)
                .GET()
                .build();
        
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (!HttpResponseHandler.isSuccess(response)) {
            HttpResponseHandler.describeError("GitHub", response);
            return true; // Include by default if we can't check
        }
        
        final JsonNode prDetails = OBJECT_MAPPER.readTree(response.body());
        
        // Check 1: Is user directly requested as reviewer?
        final JsonNode requestedReviewers = prDetails.get("requested_reviewers");
        if (requestedReviewers != null && requestedReviewers.isArray()) {
            for (final JsonNode reviewer : requestedReviewers) {
                final JsonNode login = reviewer.get("login");
                if (login != null && login.asText().equals(username)) {
                    logger.debug("PR {} needs review: user is in requested_reviewers", prNumber);
                    return true;
                }
            }
        }
        
        // Check 2: Has user reviewed but last review is not APPROVED?
        final String reviewsUrl = String.format(PR_REVIEWS_URL_TEMPLATE, repo, prNumber);
        final HttpRequest reviewsRequest = HttpRequest.newBuilder()
                .uri(URI.create(reviewsUrl))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .timeout(TIMEOUT)
                .GET()
                .build();
        
        final HttpResponse<String> reviewsResponse = httpClient.send(reviewsRequest, HttpResponse.BodyHandlers.ofString());
        if (!HttpResponseHandler.isSuccess(reviewsResponse)) {
            HttpResponseHandler.describeError("GitHub", reviewsResponse);
            return false; // If we can't check reviews, assume not needed
        }
        
        final JsonNode reviews = OBJECT_MAPPER.readTree(reviewsResponse.body());
        if (!reviews.isArray() || reviews.size() == 0) {
            return false; // No reviews from anyone
        }
        
        // Find last review by user
        String lastUserReviewState = null;
        for (final JsonNode review : reviews) {
            final JsonNode reviewUser = review.get("user");
            if (reviewUser != null) {
                final JsonNode reviewLogin = reviewUser.get("login");
                if (reviewLogin != null && reviewLogin.asText().equals(username)) {
                    final JsonNode state = review.get("state");
                    if (state != null) {
                        lastUserReviewState = state.asText();
                    }
                }
            }
        }
        
        // If user has reviewed and last state is not APPROVED, PR needs review
        if (lastUserReviewState != null && !lastUserReviewState.equals("APPROVED")) {
            logger.debug("PR {} needs review: last review state = {}", prNumber, lastUserReviewState);
            return true;
        }
        
        return false;
    }

    /**
     * Extracts PR number from GitHub PR URL.
     * Example: "https://github.com/owner/repo/pull/123" -> "123"
     */
    private String extractPRNumber(final String prUrl) {
        if (prUrl == null || prUrl.isBlank()) {
            return null;
        }
        final String[] parts = prUrl.split("/");
        if (parts.length > 0) {
            return parts[parts.length - 1];
        }
        return null;
    }

    private List<GitHubPullRequest> parseResponse(final String json) throws Exception {
        final JsonNode root = OBJECT_MAPPER.readTree(json);
        final JsonNode totalCount = root.get("total_count");
        logger.info("GitHub: total_count = {}", totalCount);
        final JsonNode items = root.get("items");

        if (items == null || !items.isArray()) {
            return Collections.emptyList();
        }

        final List<GitHubPullRequest> pullRequests = new ArrayList<>();
        for (final JsonNode item : items) {
            final int number = item.has("number") ? item.get("number").asInt(0) : 0;
            final String title = getTextOrDefault(item, "title", "Untitled");
            final String url = getTextOrDefault(item, "html_url", "");
            final String repositoryUrl = getTextOrDefault(item, "repository_url", "");
            final String repository = extractRepositoryName(repositoryUrl);
            final String author = item.has("user") && item.get("user").has("login")
                    ? item.get("user").get("login").asText()
                    : "unknown";
            final boolean draft = item.has("draft") && item.get("draft").asBoolean(false);

            pullRequests.add(new GitHubPullRequest(number, title, url, repository, author, draft));
        }
        return Collections.unmodifiableList(pullRequests);
    }

    private String getTextOrDefault(final JsonNode node, final String field, final String defaultValue) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText(defaultValue);
        }
        return defaultValue;
    }

    /**
     * Extracts "owner/repo" from a GitHub API repository URL like
     * "https://api.github.com/repos/owner/repo".
     */
    private String extractRepositoryName(final String repositoryUrl) {
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
