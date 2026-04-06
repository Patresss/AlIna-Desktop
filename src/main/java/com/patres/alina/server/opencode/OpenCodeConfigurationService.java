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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class OpenCodeConfigurationService {

    private static final Pattern TYPED_RULE_PATTERN = Pattern.compile("^([A-Za-z0-9_]+)\\((.*)\\)$");
    private static final Set<String> PATTERN_PERMISSION_KEYS = Set.of("bash", "skill", "external_directory");

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
        final WorkspaceSettings workspace = workspaceSettings();
        final AssistantSettings assistant = assistantSettings();

        final ObjectNode root = loadOpenCodeDocument();
        root.put("$schema", "https://opencode.ai/config.json");
        root.put("model", resolveModelIdentifier(assistant));
        root.put("default_agent", "general");
        root.set("permission", buildPermissionConfig(workspace));
        root.put("snapshot", true);
        return root;
    }

    public Map<String, String> buildServerEnvironment() {
        final AssistantSettings assistant = assistantSettings();
        final Map<String, String> env = new java.util.HashMap<>();
        putIfPresent(env, "OPENAI_API_KEY", assistant.openAiApiKey(), AssistantSettings.DEFAULT_OPENAI_API_KEY);
        putIfPresent(env, "ANTHROPIC_API_KEY", assistant.anthropicApiKey(), null);
        putIfPresent(env, "GOOGLE_GENERATIVE_AI_API_KEY", assistant.googleApiKey(), null);
        return env;
    }

    public Path resolveWorkingDirectory() {
        return Path.of(workspaceSettings().openCodeWorkingDirectory()).toAbsolutePath().normalize();
    }

    public ObjectNode loadOpenCodePermissionConfig() {
        final Path configPath = resolvePermissionsPath();
        ensureStarterConfig(configPath);
        migrateLegacyPermissionsIfNeeded(configPath);
        try {
            return loadPermissionNode(configPath);
        } catch (IOException e) {
            return starterPermissionNode();
        }
    }

    private JsonNode buildPermissionConfig(final WorkspaceSettings workspace) {
        final ObjectNode permission = loadOpenCodePermissionConfig().deepCopy();
        final JsonNode externalNode = permission.get("external_directory");
        final ObjectNode external = externalNode instanceof ObjectNode objectNode
                ? objectNode
                : permission.putObject("external_directory");
        for (final Path path : collectExternalDirectories(workspace)) {
            external.put(path.toString() + "/**", "allow");
        }
        return permission;
    }

    private String resolveModelIdentifier(final AssistantSettings settings) {
        return settings.resolveModelIdentifier();
    }

    private Set<Path> collectExternalDirectories(final WorkspaceSettings workspace) {
        final Set<Path> roots = new LinkedHashSet<>();
        roots.add(AppPaths.baseDataDir().toAbsolutePath().normalize());
        roots.add(AppPaths.resolve("profile/default").toAbsolutePath().normalize());
        roots.add(OpenCodePaths.commandsDir());
        roots.add(OpenCodePaths.skillsDir());
        if (workspace.openCodeWorkingDirectory() != null && !workspace.openCodeWorkingDirectory().isBlank()) {
            roots.add(Path.of(workspace.openCodeWorkingDirectory()).toAbsolutePath().normalize());
        }
        return roots;
    }

    private void putIfPresent(final Map<String, String> env,
                              final String key,
                              final String value,
                              final String ignoredValue) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (ignoredValue != null && ignoredValue.equals(value)) {
            return;
        }
        env.put(key, value);
    }

    private Path resolvePermissionsPath() {
        return OpenCodePaths.configFile();
    }

    private ObjectNode loadOpenCodeDocument() {
        final Path configPath = resolvePermissionsPath();
        ensureStarterConfig(configPath);
        try {
            return loadDocument(configPath);
        } catch (IOException e) {
            final ObjectNode document = objectMapper.createObjectNode();
            document.put("$schema", "https://opencode.ai/config.json");
            document.set("permission", starterPermissionNode());
            return document;
        }
    }

    private void migrateLegacyPermissionsIfNeeded(final Path targetPath) {
        final Path legacyPath = Path.of(
                System.getProperty("user.home", "."),
                ".config",
                "AlIna",
                "config",
                "permissions.json"
        ).toAbsolutePath().normalize();
        if (targetPath.equals(legacyPath) || !Files.isRegularFile(legacyPath)) {
            return;
        }

        try {
            final ObjectNode targetDocument = loadDocument(targetPath);
            final JsonNode legacyRaw = objectMapper.readTree(legacyPath.toFile());
            final ObjectNode legacyPermission = normalizePermissionNode(legacyRaw);
            final ObjectNode targetPermission = ensurePermissionNode(targetDocument);
            if (mergeMissingPermissionRules(targetPermission, legacyPermission)) {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(targetPath.toFile(), targetDocument);
            }
        } catch (IOException ignored) {
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
            document.set("permission", starterPermissionNode());
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
        document.set("permission", normalizePermissionNode(raw));
        return document;
    }

    private ObjectNode loadPermissionNode(final Path path) throws IOException {
        return ensurePermissionNode(loadDocument(path));
    }

    private ObjectNode ensurePermissionNode(final ObjectNode document) {
        final JsonNode existing = document.get("permission");
        if (existing instanceof ObjectNode objectNode) {
            return objectNode;
        }
        final ObjectNode permission = starterPermissionNode();
        document.set("permission", permission);
        return permission;
    }

    private ObjectNode normalizePermissionNode(final JsonNode root) {
        final ObjectNode normalized;
        if (root != null && root.path("permission").isObject()) {
            normalized = ((ObjectNode) root.path("permission")).deepCopy();
        } else if (root != null && root.path("permissions").isObject()) {
            normalized = legacyPermissionsToOpenCode(root.path("permissions"));
        } else {
            normalized = starterPermissionNode();
        }
        canonicalizePermissionNode(normalized);
        return normalized;
    }

    private ObjectNode legacyPermissionsToOpenCode(final JsonNode legacyPermissions) {
        final ObjectNode permission = starterPermissionNode();
        final JsonNode allow = legacyPermissions.path("allow");
        if (allow.isArray()) {
            allow.forEach(rule -> applyLegacyRule(permission, rule.asText(null), "allow"));
        }
        final JsonNode deny = legacyPermissions.path("deny");
        if (deny.isArray()) {
            deny.forEach(rule -> applyLegacyRule(permission, rule.asText(null), "deny"));
        }
        final JsonNode ask = legacyPermissions.path("ask");
        if (ask.isArray()) {
            ask.forEach(rule -> applyLegacyRule(permission, rule.asText(null), "ask"));
        }
        return permission;
    }

    private void applyLegacyRule(final ObjectNode permission,
                                 final String legacyRule,
                                 final String decision) {
        if (legacyRule == null || legacyRule.isBlank()) {
            return;
        }
        final var matcher = TYPED_RULE_PATTERN.matcher(legacyRule.trim());
        if (!matcher.matches()) {
            if ("*".equals(legacyRule.trim())) {
                permission.put("*", decision);
            }
            return;
        }

        final String type = matcher.group(1).trim().toLowerCase(java.util.Locale.ROOT);
        final String value = matcher.group(2).trim();
        switch (type) {
            case "bash" -> upsertAllowRule(permission, "bash", List.of(legacyToBashPattern(value)), decision);
            case "tool", "mcp" -> permission.put(value, decision);
            default -> permission.put(value, decision);
        }
    }

    private void upsertAllowRule(final ObjectNode permission,
                                 final String permissionKey,
                                 final List<String> patterns) {
        upsertAllowRule(permission, permissionKey, patterns, "allow");
    }

    private void upsertAllowRule(final ObjectNode permission,
                                 final String permissionKey,
                                 final List<String> patterns,
                                 final String decision) {
        if (!supportsPatternRules(permissionKey)) {
            permission.put(permissionKey, decision);
            return;
        }
        if (patterns == null || patterns.isEmpty()) {
            permission.put(permissionKey, decision);
            return;
        }
        final JsonNode existing = permission.get(permissionKey);
        if (existing != null && existing.isTextual() && decision.equalsIgnoreCase(existing.asText())) {
            return;
        }
        final ObjectNode objectNode;
        if (existing instanceof ObjectNode existingObject) {
            objectNode = existingObject;
        } else {
            objectNode = objectMapper.createObjectNode();
            if (existing != null && existing.isTextual()) {
                objectNode.put("*", existing.asText());
            }
            permission.set(permissionKey, objectNode);
        }
        for (final String pattern : patterns) {
            if (pattern != null && !pattern.isBlank() && !isCoveredByExistingRule(objectNode, pattern, decision)) {
                objectNode.put(pattern, decision);
            }
        }
        if (shouldPruneRedundantRules(permissionKey)) {
            pruneRedundantPatternRules(objectNode);
        }
        if (shouldCollapsePermissionObject(permissionKey, objectNode)) {
            permission.put(permissionKey, objectNode.elements().next().asText());
        }
    }

    private void canonicalizePermissionNode(final ObjectNode permission) {
        final java.util.ArrayList<String> fieldNames = new java.util.ArrayList<>();
        permission.fieldNames().forEachRemaining(fieldNames::add);
        for (final String fieldName : fieldNames) {
            final JsonNode value = permission.get(fieldName);
            if (!(value instanceof ObjectNode objectNode)) {
                continue;
            }
            if (!supportsPatternRules(fieldName)) {
                permission.put(fieldName, resolveUnsupportedObjectDecision(objectNode));
                continue;
            }
            if ("bash".equalsIgnoreCase(fieldName)) {
                normalizeBashPatterns(objectNode);
            }
            if (shouldPruneRedundantRules(fieldName)) {
                pruneRedundantPatternRules(objectNode);
            }
            if (shouldCollapsePermissionObject(fieldName, objectNode)) {
                permission.put(fieldName, objectNode.elements().next().asText());
            }
        }
    }

    private void normalizeBashPatterns(final ObjectNode objectNode) {
        final LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        objectNode.fields().forEachRemaining(field -> {
            if (!field.getValue().isTextual()) {
                return;
            }
            final String decision = field.getValue().asText();
            for (final String pattern : expandBashPattern(field.getKey())) {
                normalized.putIfAbsent(pattern, decision);
            }
        });
        objectNode.removeAll();
        normalized.forEach(objectNode::put);
    }

    private List<String> expandBashPattern(final String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return List.of();
        }
        if ("*".equals(pattern)) {
            return List.of(pattern);
        }
        if (pattern.indexOf('?') >= 0) {
            return List.of(pattern);
        }
        final int firstStar = pattern.indexOf('*');
        if (firstStar >= 0 && firstStar != pattern.length() - 1) {
            return List.of(pattern);
        }
        if (!pattern.endsWith("*")) {
            return List.of(pattern);
        }

        final String command = pattern.substring(0, pattern.length() - 1).trim();
        if (command.isBlank()) {
            return List.of(pattern);
        }
        return List.of(command, command + " *");
    }

    private boolean isCoveredByExistingRule(final ObjectNode objectNode,
                                            final String candidatePattern,
                                            final String decision) {
        final var fields = objectNode.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> field = fields.next();
            final JsonNode value = field.getValue();
            if (!value.isTextual() || !decision.equalsIgnoreCase(value.asText())) {
                continue;
            }
            if (globMatches(field.getKey(), candidatePattern)) {
                return true;
            }
        }
        return false;
    }

    private void pruneRedundantPatternRules(final ObjectNode objectNode) {
        final LinkedHashMap<String, String> rules = new LinkedHashMap<>();
        objectNode.fields().forEachRemaining(field -> {
            if (field.getValue().isTextual()) {
                rules.put(field.getKey(), field.getValue().asText());
            }
        });
        if (rules.size() < 2) {
            return;
        }

        final java.util.ArrayList<String> removable = new java.util.ArrayList<>();
        for (final Map.Entry<String, String> candidate : rules.entrySet()) {
            for (final Map.Entry<String, String> existing : rules.entrySet()) {
                if (candidate == existing) {
                    continue;
                }
                if (!existing.getValue().equalsIgnoreCase(candidate.getValue())) {
                    continue;
                }
                if (globMatches(existing.getKey(), candidate.getKey())) {
                    removable.add(candidate.getKey());
                    break;
                }
            }
        }

        removable.forEach(objectNode::remove);
    }

    private boolean globMatches(final String pattern, final String text) {
        if (pattern == null || pattern.isBlank() || text == null || text.isBlank()) {
            return false;
        }
        final StringBuilder regex = new StringBuilder("^");
        for (int i = 0; i < pattern.length(); i++) {
            final char ch = pattern.charAt(i);
            if (ch == '*') {
                regex.append(".*");
            } else {
                regex.append(Pattern.quote(String.valueOf(ch)));
            }
        }
        regex.append('$');
        return Pattern.compile(regex.toString()).matcher(text).matches();
    }

    private boolean mergeMissingPermissionRules(final ObjectNode targetPermission, final ObjectNode legacyPermission) {
        boolean changed = false;
        final java.util.Iterator<Map.Entry<String, JsonNode>> fields = legacyPermission.fields();
        while (fields.hasNext()) {
            final Map.Entry<String, JsonNode> field = fields.next();
            final String key = field.getKey();
            final JsonNode legacyValue = field.getValue();
            final JsonNode targetValue = targetPermission.get(key);

            if (targetValue == null || targetValue.isNull()) {
                targetPermission.set(key, legacyValue.deepCopy());
                changed = true;
                continue;
            }

            if (targetValue instanceof ObjectNode targetObject && legacyValue instanceof ObjectNode legacyObject) {
                final java.util.Iterator<Map.Entry<String, JsonNode>> nestedFields = legacyObject.fields();
                while (nestedFields.hasNext()) {
                    final Map.Entry<String, JsonNode> nestedField = nestedFields.next();
                    if (targetObject.has(nestedField.getKey())) {
                        continue;
                    }
                    targetObject.set(nestedField.getKey(), nestedField.getValue().deepCopy());
                    changed = true;
                }
            }
        }
        if (changed) {
            canonicalizePermissionNode(targetPermission);
        }
        return changed;
    }

    private boolean shouldCollapsePermissionObject(final String permissionKey, final ObjectNode objectNode) {
        if (!supportsPatternRules(permissionKey)) {
            return false;
        }
        if (!objectNode.has("*")) {
            return false;
        }
        String decision = null;
        final java.util.Iterator<JsonNode> values = objectNode.elements();
        while (values.hasNext()) {
            final JsonNode value = values.next();
            if (!value.isTextual()) {
                return false;
            }
            final String text = value.asText();
            if (decision == null) {
                decision = text;
                continue;
            }
            if (!decision.equalsIgnoreCase(text)) {
                return false;
            }
        }
        return decision != null && !decision.isBlank();
    }

    private boolean supportsPatternRules(final String permissionKey) {
        if (permissionKey == null || permissionKey.isBlank()) {
            return false;
        }
        return PATTERN_PERMISSION_KEYS.contains(permissionKey.toLowerCase(java.util.Locale.ROOT));
    }

    private boolean shouldPruneRedundantRules(final String permissionKey) {
        return permissionKey == null || !"bash".equalsIgnoreCase(permissionKey);
    }

    private String resolveUnsupportedObjectDecision(final ObjectNode objectNode) {
        boolean hasAllow = false;
        boolean hasDeny = false;
        final java.util.Iterator<JsonNode> values = objectNode.elements();
        while (values.hasNext()) {
            final JsonNode value = values.next();
            if (!value.isTextual()) {
                continue;
            }
            final String text = value.asText("").trim().toLowerCase(java.util.Locale.ROOT);
            if ("allow".equals(text)) {
                hasAllow = true;
            } else if ("deny".equals(text)) {
                hasDeny = true;
            }
        }
        if (hasAllow) {
            return "allow";
        }
        if (hasDeny) {
            return "deny";
        }
        return "ask";
    }

    private ObjectNode starterPermissionNode() {
        final ObjectNode permission = objectMapper.createObjectNode();
        permission.put("*", "ask");
        permission.put("read", "allow");
        permission.put("grep", "allow");
        permission.put("glob", "allow");
        permission.put("list", "allow");
        permission.put("todoread", "allow");
        permission.put("todowrite", "allow");
        permission.put("edit", "ask");
        permission.put("webfetch", "ask");
        permission.put("websearch", "ask");
        permission.put("codesearch", "ask");
        permission.putObject("skill").put("*", "allow");

        final ObjectNode bash = permission.putObject("bash");
        allowBashCommand(bash, "pwd");
        allowBashCommand(bash, "ls");
        allowBashCommand(bash, "find");
        allowBashCommand(bash, "rg");
        allowBashCommand(bash, "grep");
        allowBashCommand(bash, "cat");
        allowBashCommand(bash, "head");
        allowBashCommand(bash, "tail");
        allowBashCommand(bash, "sed");
        allowBashCommand(bash, "wc");
        allowBashCommand(bash, "tree");
        allowBashCommand(bash, "fd");
        allowBashCommand(bash, "stat");
        allowBashCommand(bash, "file");
        allowBashCommand(bash, "git status");
        allowBashCommand(bash, "git show");
        allowBashCommand(bash, "git diff");
        allowBashCommand(bash, "git log");
        allowBashCommand(bash, "git branch");
        allowBashCommand(bash, "git rev-parse");
        permission.putObject("external_directory");
        return permission;
    }

    private void allowBashCommand(final ObjectNode bash, final String command) {
        bash.put(command, "allow");
        bash.put(command + " *", "allow");
    }

    private String legacyToBashPattern(final String legacyRuleValue) {
        if (legacyRuleValue == null || legacyRuleValue.isBlank()) {
            return "*";
        }
        if ("*".equals(legacyRuleValue.trim())) {
            return "*";
        }
        if (legacyRuleValue.endsWith(":*")) {
            return legacyRuleValue.substring(0, legacyRuleValue.length() - 2).trim() + "*";
        }
        return legacyRuleValue;
    }
}
