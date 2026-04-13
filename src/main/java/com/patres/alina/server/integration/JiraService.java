package com.patres.alina.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.server.integration.http.HttpClientFactory;
import com.patres.alina.server.integration.http.HttpResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for interacting with Jira Cloud API.
 * Fetches issues assigned to the current user that are not closed/done/rejected.
 */
@Service
public class JiraService {

    private static final Logger logger = LoggerFactory.getLogger(JiraService.class);

    private static final String JIRA_BASE_URL = "https://allegrogroup.atlassian.net";
    private static final String SEARCH_API_PATH = "/rest/api/3/search/jql";
    
    // JQL query: assigned to current user, not in terminal states, not Epic, ordered by update time
    private static final String JQL_TEMPLATE = 
            "assignee = currentUser() AND status NOT IN (Closed, Done, Rejected, Cancelled, Resolved) AND type != Epic ORDER BY updated DESC";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;

    public JiraService(final HttpClientFactory httpClientFactory) {
        this.httpClient = httpClientFactory.getClient();
    }

    /**
     * Fetches Jira issues assigned to the current user that are not closed/done/rejected.
     * 
     * @param jiraEmail email address for Jira authentication
     * @param jiraApiToken API token from https://id.atlassian.com/manage-profile/security/api-tokens
     * @param maxResults maximum number of issues to return in the result list
     * @return result containing the issues and total count
     */
    public JiraIssueResult fetchAssignedIssues(final String jiraEmail, final String jiraApiToken, final int maxResults) {
        if (jiraEmail == null || jiraEmail.isBlank() || jiraApiToken == null || jiraApiToken.isBlank()) {
            logger.info("Jira: skipping fetch - email or token is empty");
            return JiraIssueResult.empty();
        }

        final String emailPrefix = jiraEmail.length() > 10 ? jiraEmail.substring(0, 10) + "..." : "***";
        logger.info("Jira: starting fetch with email: {}, maxResults: {}", emailPrefix, maxResults);

        try {
            // Build search URL with JQL query
            final String encodedJql = URLEncoder.encode(JQL_TEMPLATE, StandardCharsets.UTF_8);
            final String searchUrl = JIRA_BASE_URL + SEARCH_API_PATH 
                    + "?jql=" + encodedJql
                    + "&maxResults=" + Math.min(maxResults, 50)  // API limit
                    + "&fields=summary,status,priority,issuetype";

            logger.info("Jira: search URL = {}", searchUrl);

            // Create Basic Auth header: Base64(email:apiToken)
            final String authString = jiraEmail + ":" + jiraApiToken;
            final String basicAuth = "Basic " + java.util.Base64.getEncoder().encodeToString(authString.getBytes(StandardCharsets.UTF_8));

            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(searchUrl))
                    .header("Authorization", basicAuth)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            logger.info("Jira: search response status = {}, body length = {}", 
                    response.statusCode(), response.body().length());

            if (!HttpResponseHandler.isSuccess(response)) {
                HttpResponseHandler.describeError("Jira", response);
                if (response.statusCode() == 401) {
                    logger.warn("Jira API returned 401 Unauthorized. Check your email and API token. " +
                            "Create an API token at: https://id.atlassian.com/manage-profile/security/api-tokens");
                }
                return JiraIssueResult.error();
            }

            return parseResponse(response.body(), maxResults);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Jira API call interrupted", e);
            return JiraIssueResult.error();
        } catch (final Exception e) {
            logger.warn("Failed to fetch Jira issues", e);
            return JiraIssueResult.error();
        }
    }

    private JiraIssueResult parseResponse(final String json, final int maxResults) throws Exception {
        final JsonNode root = OBJECT_MAPPER.readTree(json);
        final int totalCount = root.path("total").asInt(0);
        logger.info("Jira: total issues matching query = {}", totalCount);

        final JsonNode issues = root.path("issues");
        if (issues.isMissingNode() || !issues.isArray()) {
            return JiraIssueResult.empty();
        }

        final List<JiraIssue> issueList = new ArrayList<>();
        for (final JsonNode issue : issues) {
            if (issueList.size() >= maxResults) {
                break;
            }

            final String key = issue.path("key").asText("UNKNOWN");
            final JsonNode fields = issue.path("fields");
            
            final String summary = fields.path("summary").asText("Untitled");
            final String url = JIRA_BASE_URL + "/browse/" + key;
            final String status = fields.path("status").path("name").asText("Unknown");
            final String priority = fields.path("priority").path("name").asText("None");
            final String type = fields.path("issuetype").path("name").asText("Task");

            issueList.add(new JiraIssue(key, summary, url, status, priority, type));
        }

        return new JiraIssueResult(
                Collections.unmodifiableList(issueList),
                totalCount,
                false
        );
    }
}
