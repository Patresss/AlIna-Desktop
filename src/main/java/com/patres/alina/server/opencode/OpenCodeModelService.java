package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.patres.alina.common.settings.AssistantSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

@Service
public class OpenCodeModelService {

    private static final Logger logger = LoggerFactory.getLogger(OpenCodeModelService.class);
    private static final int CACHE_TIME_SEC = 60 * 60 * 24; // 1 day
    private static final int PROVIDER_CACHE_TIME_SEC = 60;

    private final OpenCodeConfigurationService configurationService;
    private final OpenCodeHttpClient httpClient;

    private volatile List<String> cachedModels = List.of();
    private volatile Instant cachedModelsAt = Instant.EPOCH;
    private volatile JsonNode cachedProviderCatalog;
    private volatile Instant cachedProviderCatalogAt = Instant.EPOCH;

    public OpenCodeModelService(final OpenCodeConfigurationService configurationService,
                                final OpenCodeHttpClient httpClient) {
        this.configurationService = configurationService;
        this.httpClient = httpClient;
    }

    public void resetCache() {
        cachedModels = List.of();
        cachedModelsAt = Instant.EPOCH;
        cachedProviderCatalog = null;
        cachedProviderCatalogAt = Instant.EPOCH;
    }

    /**
     * Returns the variants advertised by OpenCode for a model. OpenCode calls
     * these values variants rather than reasoning efforts, but they represent
     * the same user-facing choice for this application.
     */
    public List<String> getAvailableEfforts(final String modelIdentifier) {
        final String effectiveModel = modelIdentifier == null || modelIdentifier.isBlank()
                ? resolveEffectiveModelIdentifier()
                : modelIdentifier.trim();
        try {
            final JsonNode response = requestProviderCatalog();
            final JsonNode providers = response.path("all").isArray()
                    ? response.path("all")
                    : response.path("providers");
            if (!providers.isArray()) {
                return List.of();
            }

            final String providerId = providerPart(effectiveModel);
            final String modelId = modelPart(effectiveModel);
            for (final JsonNode provider : providers) {
                final String currentProviderId = provider.path("id")
                        .asText(provider.path("providerID").asText(null));
                if (currentProviderId != null && !currentProviderId.isBlank()
                        && !currentProviderId.equals(providerId)) {
                    continue;
                }
                final JsonNode models = provider.path("models");
                if (!models.isObject()) {
                    continue;
                }
                JsonNode model = models.path(modelId);
                if (model.isMissingNode()) {
                    final Iterator<String> fieldNames = models.fieldNames();
                    while (fieldNames.hasNext()) {
                        final String fieldName = fieldNames.next();
                        if (fieldName.equals(modelId) || fieldName.endsWith("/" + modelId)) {
                            model = models.path(fieldName);
                            break;
                        }
                    }
                }
                final JsonNode variants = model.path("variants");
                if (!variants.isObject()) {
                    return List.of();
                }
                final List<String> efforts = new ArrayList<>();
                variants.fieldNames().forEachRemaining(effort -> {
                    if (!effort.isBlank() && !efforts.contains(effort)) {
                        efforts.add(effort);
                    }
                });
                return List.copyOf(efforts);
            }
        } catch (Exception e) {
            logger.debug("Cannot fetch variants from OpenCode for model {}", effectiveModel, e);
        }
        return List.of();
    }

    private JsonNode requestProviderCatalog() throws Exception {
        if (cachedProviderCatalog != null
                && Instant.now().isBefore(cachedProviderCatalogAt.plusSeconds(PROVIDER_CACHE_TIME_SEC))) {
            return cachedProviderCatalog;
        }
        synchronized (this) {
            if (cachedProviderCatalog != null
                    && Instant.now().isBefore(cachedProviderCatalogAt.plusSeconds(PROVIDER_CACHE_TIME_SEC))) {
                return cachedProviderCatalog;
            }
            cachedProviderCatalog = httpClient.get("/provider");
            cachedProviderCatalogAt = Instant.now();
            return cachedProviderCatalog;
        }
    }

    public List<String> getAvailableModels() {
        try {
            if (Instant.now().isBefore(cachedModelsAt.plusSeconds(CACHE_TIME_SEC)) && !cachedModels.isEmpty()) {
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
