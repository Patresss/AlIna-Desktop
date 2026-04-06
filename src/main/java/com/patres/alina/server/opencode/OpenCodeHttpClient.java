package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.common.settings.WorkspaceSettings;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class OpenCodeHttpClient {

    private final OpenCodeConfigurationService configurationService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public OpenCodeHttpClient(final OpenCodeConfigurationService configurationService,
                              final ObjectMapper objectMapper) {
        this.configurationService = configurationService;
        this.objectMapper = objectMapper;
    }

    public JsonNode get(final String path) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri(path))
                .GET()
                .build();
        return sendJson(request, "OpenCode request failed");
    }

    public JsonNode post(final String path, final JsonNode body) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        return sendJson(request, "OpenCode request failed");
    }

    public JsonNode patch(final String path, final JsonNode body) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("content-type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        return sendJson(request, "OpenCode config update failed");
    }

    public void postNoContent(final String path, final JsonNode body) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 204) {
            throw new IllegalStateException("OpenCode request failed: HTTP " + response.statusCode() + " " + response.body());
        }
    }

    public InputStream openEventStream() throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri("/event"))
                .header("accept", "text/event-stream")
                .GET()
                .build();
        final HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Cannot subscribe to OpenCode events: HTTP " + response.statusCode());
        }
        return response.body();
    }

    public boolean isHealthy() {
        try {
            final HttpRequest request = HttpRequest.newBuilder(uri("/global/health")).GET().build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public String baseUrl() {
        final WorkspaceSettings workspace = configurationService.workspaceSettings();
        return "http://" + workspace.openCodeHostname() + ":" + workspace.openCodePort();
    }

    private JsonNode sendJson(final HttpRequest request, final String errorPrefix) throws Exception {
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException(errorPrefix + ": HTTP " + response.statusCode() + " " + response.body());
        }
        if (response.body() == null || response.body().isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(response.body());
    }

    private URI uri(final String path) {
        return URI.create(baseUrl() + path);
    }
}
