package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.patres.alina.common.settings.AssistantSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.TreeSet;

@Service
public class OpenCodeModelService {

    private static final Logger logger = LoggerFactory.getLogger(OpenCodeModelService.class);

    private final OpenCodeConfigurationService configurationService;
    private final OpenCodeHttpClient httpClient;

    private volatile List<String> cachedModels = List.of();
    private volatile Instant cachedModelsAt = Instant.EPOCH;

    public OpenCodeModelService(final OpenCodeConfigurationService configurationService,
                                final OpenCodeHttpClient httpClient) {
        this.configurationService = configurationService;
        this.httpClient = httpClient;
    }

    public void resetCache() {
        cachedModels = List.of();
        cachedModelsAt = Instant.EPOCH;
    }

    public List<String> getAvailableModels() {
        try {
            if (Instant.now().isBefore(cachedModelsAt.plusSeconds(30)) && !cachedModels.isEmpty()) {
                return cachedModels;
            }

            final TreeSet<String> models = new TreeSet<>(fetchAvailableModelsFromCli());
            if (models.isEmpty()) {
                final JsonNode config = httpClient.get("/global/config");
                final String current = config.path("model").asText(null);
                if (current != null && !current.isBlank()) {
                    models.add(current);
                }
            }
            if (models.isEmpty()) {
                models.add(configurationService.assistantSettings().resolveModelIdentifier());
            }
            cachedModels = List.copyOf(models);
            cachedModelsAt = Instant.now();
            return cachedModels;
        } catch (Exception e) {
            logger.warn("Cannot fetch available models from OpenCode", e);
            return List.of(configurationService.assistantSettings().resolveModelIdentifier());
        }
    }

    public String resolveEffectiveModelIdentifier() {
        return configurationService.assistantSettings().resolveModelIdentifier();
    }

    public String providerPart(final String modelIdentifier) {
        if (modelIdentifier == null || !modelIdentifier.contains("/")) {
            return configurationService.assistantSettings().resolveProviderId();
        }
        return modelIdentifier.substring(0, modelIdentifier.indexOf('/'));
    }

    public String modelPart(final String modelIdentifier) {
        if (modelIdentifier == null || !modelIdentifier.contains("/")) {
            return configurationService.assistantSettings().resolveModelId();
        }
        return modelIdentifier.substring(modelIdentifier.indexOf('/') + 1);
    }

    private List<String> fetchAvailableModelsFromCli() throws IOException, InterruptedException {
        final ProcessBuilder processBuilder = new ProcessBuilder(
                OpenCodeConfigurationService.OPENCODE_COMMAND, "models"
        );
        processBuilder.directory(configurationService.resolveWorkingDirectory().toFile());
        processBuilder.environment().putAll(configurationService.buildServerEnvironment());
        processBuilder.redirectErrorStream(true);

        final Process process = processBuilder.start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return List.of();
        }
        if (process.exitValue() != 0) {
            return List.of();
        }

        return output.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> line.contains("/"))
                .toList();
    }

}
