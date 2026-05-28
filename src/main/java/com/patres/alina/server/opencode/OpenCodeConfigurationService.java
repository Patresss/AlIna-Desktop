package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.storage.AppPaths;
import com.patres.alina.common.storage.OpenCodePaths;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class OpenCodeConfigurationService {

    private final FileManager<WorkspaceSettings> workspaceSettingsManager;
    private final FileManager<AssistantSettings> assistantSettingsManager;
    private final ObjectMapper objectMapper;

    public OpenCodeConfigurationService(final FileManager<WorkspaceSettings> workspaceSettingsManager,
                                        final FileManager<AssistantSettings> assistantSettingsManager,
                                        final ObjectMapper objectMapper) {
        this.workspaceSettingsManager = workspaceSettingsManager;
        this.assistantSettingsManager = assistantSettingsManager;
        this.objectMapper = objectMapper;
    }

    public WorkspaceSettings workspaceSettings() {
        return workspaceSettingsManager.getSettings();
    }

    public AssistantSettings assistantSettings() {
        return assistantSettingsManager.getSettings();
    }

    public ObjectNode buildGlobalConfig() {
        final AssistantSettings assistant = assistantSettings();

        final ObjectNode root = loadOpenCodeDocument();
        normalizePermissions(root);
        addTrustedDirectories(root);
        root.put("$schema", "https://opencode.ai/config.json");
        root.put("model", resolveModelIdentifier(assistant));
        root.put("default_agent", "general");
        root.put("snapshot", true);
        return root;
    }

    public Map<String, String> buildServerEnvironment() {
        return Map.of();
    }

    public static final String OPENCODE_COMMAND = "opencode";

    public Path resolveWorkingDirectory() {
        return Path.of(workspaceSettings().openCodeWorkingDirectory()).toAbsolutePath().normalize();
    }

    private String resolveModelIdentifier(final AssistantSettings settings) {
        return settings.resolveModelIdentifier();
    }

    private Path resolveConfigPath() {
        return OpenCodePaths.configFile();
    }

    private ObjectNode loadOpenCodeDocument() {
        final Path configPath = resolveConfigPath();
        ensureStarterConfig(configPath);
        try {
            return loadDocument(configPath);
        } catch (IOException e) {
            final ObjectNode document = objectMapper.createObjectNode();
            document.put("$schema", "https://opencode.ai/config.json");
            return document;
        }
    }

    private void ensureStarterConfig(final Path path) {
        if (Files.exists(path)) {
            return;
        }
        try {
            final Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            final ObjectNode document = objectMapper.createObjectNode();
            document.put("$schema", "https://opencode.ai/config.json");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), document);
        } catch (IOException ignored) {
        }
    }

    private ObjectNode loadDocument(final Path path) throws IOException {
        final JsonNode raw = objectMapper.readTree(path.toFile());
        final ObjectNode document = objectMapper.createObjectNode();
        if (raw instanceof ObjectNode objectNode) {
            document.setAll(objectNode);
        }
        if (!document.hasNonNull("$schema")) {
            document.put("$schema", "https://opencode.ai/config.json");
        }
        return document;
    }

    private void normalizePermissions(final ObjectNode root) {
        final JsonNode permissionNode = root.path("permission");
        if (!(permissionNode instanceof ObjectNode permissions)) {
            return;
        }
        final ObjectNode normalized = permissions.deepCopy();
        permissions.fieldNames().forEachRemaining(permissionName -> {
            final JsonNode value = permissions.path(permissionName);
            if (!(value instanceof ObjectNode objectValue)) {
                return;
            }
            if ("bash".equals(permissionName)) {
                normalizeBashPermissions(objectValue, normalized.putObject(permissionName));
                return;
            }
            normalized.put(permissionName, strongestDecision(objectValue));
        });
        root.set("permission", normalized);
    }

    private void normalizeBashPermissions(final ObjectNode source, final ObjectNode target) {
        source.fields().forEachRemaining(entry -> {
            final String pattern = entry.getKey();
            final String decision = entry.getValue().asText("ask");
            target.put(pattern, decision);
            final String compactPattern = compactTrailingWildcard(pattern);
            if (!compactPattern.equals(pattern) && !target.has(compactPattern)) {
                target.put(compactPattern, decision);
            }
        });
    }

    private String compactTrailingWildcard(final String pattern) {
        if (pattern == null) {
            return "";
        }
        final String trimmed = pattern.trim();
        if (trimmed.endsWith(" *")) {
            return trimmed.substring(0, trimmed.length() - 2).trim();
        }
        return trimmed;
    }

    private String strongestDecision(final ObjectNode permissions) {
        final boolean anyAllow = iterable(permissions).stream()
                .anyMatch(value -> "allow".equalsIgnoreCase(value.asText("")));
        return anyAllow ? "allow" : permissions.path("*").asText("ask");
    }

    private java.util.List<JsonNode> iterable(final ObjectNode node) {
        final java.util.ArrayList<JsonNode> values = new java.util.ArrayList<>();
        node.elements().forEachRemaining(values::add);
        return values;
    }

    private void addTrustedDirectories(final ObjectNode root) {
        final ObjectNode permissions;
        if (root.path("permission") instanceof ObjectNode existing) {
            permissions = existing;
        } else {
            permissions = root.putObject("permission");
        }
        final ObjectNode externalDirectories;
        if (permissions.path("external_directory") instanceof ObjectNode existing) {
            externalDirectories = existing;
        } else {
            externalDirectories = permissions.putObject("external_directory");
        }
        externalDirectories.put(AppPaths.baseDataDir().toString() + "/**", "allow");
        externalDirectories.put(resolveWorkingDirectory().toString() + "/**", "allow");
    }

}
