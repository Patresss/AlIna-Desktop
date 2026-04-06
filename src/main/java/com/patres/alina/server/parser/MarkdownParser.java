package com.patres.alina.server.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.patres.alina.common.card.State;
import com.patres.alina.server.command.Command;
import com.patres.alina.server.command.CommandVisibility;
import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MarkdownParser {
    
    private static final Logger logger = LoggerFactory.getLogger(MarkdownParser.class);
    private static final String FRONTMATTER_DELIMITER = "---";
    
    private final ObjectMapper yamlMapper;
    
    public MarkdownParser() {
        this.yamlMapper = new ObjectMapper(new YAMLFactory());
    }
    
    public ParsedCommand parseMarkdownWithFrontmatter(String content, String id) {
        if (content == null || content.trim().isEmpty()) {
            logger.warn("Empty content for command id: {}", id);
            return new ParsedCommand(id, getDefaultMetadata(id), "");
        }
        
        content = content.trim();
        
        if (!content.startsWith(FRONTMATTER_DELIMITER)) {
            logger.debug("No frontmatter found for command id: {}, using content as system prompt", id);
            return new ParsedCommand(id, getDefaultMetadata(id), content);
        }
        
        int endIndex = content.indexOf(FRONTMATTER_DELIMITER, 3);
        if (endIndex == -1) {
            logger.warn("Malformed frontmatter for command id: {}, missing closing delimiter", id);
            return new ParsedCommand(id, getDefaultMetadata(id), content);
        }
        
        String frontmatterYaml = content.substring(3, endIndex).trim();
        String markdownContent = content.substring(endIndex + 3).trim();
        
        ParsedFrontmatter parsedFrontmatter = parseYaml(frontmatterYaml, id);
        if (!id.equals(parsedFrontmatter.id())) {
            logger.warn("Frontmatter id '{}' does not match filename '{}', using frontmatter id", parsedFrontmatter.id(), id);
        }
        return new ParsedCommand(parsedFrontmatter.id(), parsedFrontmatter.metadata(), markdownContent);
    }
    
    public String generateMarkdownWithFrontmatter(Command command) {
        StringBuilder sb = new StringBuilder();
        
        // Add frontmatter
        sb.append(FRONTMATTER_DELIMITER).append("\n");
        sb.append("id: ").append(command.id()).append("\n");
        sb.append("name: \"").append(escapeYamlString(command.name())).append("\"\n");
        sb.append("description: \"").append(escapeYamlString(command.description())).append("\"\n");
        sb.append("icon: ").append(command.icon()).append("\n");
        if (command.model() != null && !command.model().isBlank()) {
            sb.append("model: ").append(command.model()).append("\n");
        }
        sb.append("state: ").append(command.state().name()).append("\n");
        sb.append("showInChat: ").append(command.visibility().showInChat()).append("\n");
        sb.append("showInContextMenuPaste: ").append(command.visibility().showInContextMenuPaste()).append("\n");
        sb.append("showInContextMenuDisplay: ").append(command.visibility().showInContextMenuDisplay()).append("\n");
        sb.append("showInContextMenuExecute: ").append(command.visibility().showInContextMenuExecute()).append("\n");

        appendShortcut(sb, "pasteShortcut", command.pasteShortcut());
        appendShortcut(sb, "displayShortcut", command.displayShortcut());
        appendShortcut(sb, "executeShortcut", command.executeShortcut());

        sb.append(FRONTMATTER_DELIMITER).append("\n");
        sb.append("\n");

        // Add content
        if (command.systemPrompt() != null && !command.systemPrompt().trim().isEmpty()) {
            sb.append(command.systemPrompt());
        }

        return sb.toString();
    }
    
    private ParsedFrontmatter parseYaml(String frontmatterYaml, String id) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlMap = yamlMapper.readValue(frontmatterYaml, Map.class);

            String resolvedId = getStringValue(yamlMap, "id", id);
            String name = getStringValue(yamlMap, "name", resolvedId);
        String description = getStringValue(yamlMap, "description", "");
        String icon = getStringValue(yamlMap, "icon", "bi-slash");
        String model = normalizeOptionalString(getStringValue(yamlMap, "model", null));
        State state = parseState(getStringValue(yamlMap, "state", "ENABLED"));
        ShortcutKeys pasteShortcut = parsePasteShortcut(yamlMap);
        ShortcutKeys displayShortcut = parseShortcut(yamlMap, "displayShortcut");
        ShortcutKeys executeShortcut = parseShortcut(yamlMap, "executeShortcut");
        CommandVisibility visibility = parseVisibility(yamlMap);

        return new ParsedFrontmatter(resolvedId, new CommandMetadata(name, description, icon, model, state, pasteShortcut, displayShortcut, executeShortcut, visibility));

    } catch (Exception e) {
        logger.warn("Failed to parse YAML frontmatter for command id: {}, using defaults. Error: {}", id, e.getMessage());
        return new ParsedFrontmatter(id, getDefaultMetadata(id));
    }
}

    @SuppressWarnings("unchecked")
    private ShortcutKeys parseShortcut(Map<String, Object> yamlMap, String rootKey) {
        try {
            Object shortcutObj = yamlMap.get(rootKey);
            if (shortcutObj == null) {
                return new ShortcutKeys();
            }

            if (shortcutObj instanceof Map) {
                Map<String, Object> shortcutMap = (Map<String, Object>) shortcutObj;

                // Parse modeKeys
                List<KeyboardKey> modeKeys = new ArrayList<>();
                Object modeKeysObj = shortcutMap.get("modeKeys");
                if (modeKeysObj instanceof List) {
                    List<Object> modeKeysList = (List<Object>) modeKeysObj;
                    for (Object keyObj : modeKeysList) {
                        if (keyObj == null || "null".equals(keyObj.toString())) {
                            modeKeys.add(null);
                        } else {
                            try {
                                KeyboardKey key = KeyboardKey.valueOf(keyObj.toString().toUpperCase());
                                modeKeys.add(key);
                            } catch (IllegalArgumentException e) {
                                logger.warn("Unknown keyboard key: {}", keyObj);
                                modeKeys.add(null);
                            }
                        }
                    }
                }

                // Parse executeKey
                KeyboardKey executeKey = null;
                Object executeKeyObj = shortcutMap.get("executeKey");
                if (executeKeyObj != null && !"null".equals(executeKeyObj.toString())) {
                    try {
                        executeKey = KeyboardKey.valueOf(executeKeyObj.toString().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        logger.warn("Unknown execute key: {}", executeKeyObj);
                    }
                }

                return new ShortcutKeys(modeKeys, executeKey);
            }

            return new ShortcutKeys();
        } catch (Exception e) {
            logger.warn("Failed to parse {}: {}", rootKey, e.getMessage());
            return new ShortcutKeys();
        }
    }

    private ShortcutKeys parsePasteShortcut(Map<String, Object> yamlMap) {
        ShortcutKeys pasteShortcut = parseShortcut(yamlMap, "pasteShortcut");
        if (pasteShortcut.getAllKeys().isEmpty()) {
            // backward compatibility with old key
            pasteShortcut = parseShortcut(yamlMap, "copyAndPasteShortcut");
        }
        if (pasteShortcut.getAllKeys().isEmpty()) {
            // backward compatibility with even older key
            pasteShortcut = parseShortcut(yamlMap, "globalShortcut");
        }
        return pasteShortcut;
    }
    
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private State parseState(String stateStr) {
        try {
            return State.valueOf(stateStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.debug("Invalid state value: {}, defaulting to ENABLED", stateStr);
            return State.ENABLED;
        }
    }

    private CommandVisibility parseVisibility(Map<String, Object> yamlMap) {
        try {
            boolean showInChat = getBooleanValue(yamlMap, "showInChat", true);
            boolean showInContextMenuPaste = getBooleanValue(yamlMap, "showInContextMenuPaste", true);
            boolean showInContextMenuDisplay = getBooleanValue(yamlMap, "showInContextMenuDisplay", true);
            boolean showInContextMenuExecute = getBooleanValue(yamlMap, "showInContextMenuExecute", false);
            return new CommandVisibility(showInChat, showInContextMenuPaste, showInContextMenuDisplay, showInContextMenuExecute);
        } catch (Exception e) {
            logger.warn("Failed to parse visibility flags: {}", e.getMessage());
            return new CommandVisibility();
        }
    }

    private boolean getBooleanValue(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(value.toString());
    }
    
    private CommandMetadata getDefaultMetadata(String id) {
        return new CommandMetadata(
                id != null ? id.replace("-", " ") : "Unnamed Command",
                "",
                "bi-slash",
                null,
                State.ENABLED,
                new ShortcutKeys(),
                new ShortcutKeys(),
                new ShortcutKeys(),
                new CommandVisibility()
        );
    }
    
    private String escapeYamlString(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"").replace("\n", "\\n");
    }

    private String normalizeOptionalString(final String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void appendShortcut(StringBuilder sb, String key, ShortcutKeys shortcut) {
        if (shortcut == null || shortcut.getAllKeys().isEmpty()) {
            return;
        }
        sb.append(key).append(":\n");
        sb.append("  modeKeys:\n");
        for (KeyboardKey modeKey : shortcut.getModeKeys()) {
            if (modeKey != null) {
                sb.append("    - ").append(modeKey.name()).append("\n");
            } else {
                sb.append("    - null\n");
            }
        }
        if (shortcut.getExecuteKey() != null) {
            sb.append("  executeKey: ").append(shortcut.getExecuteKey().name()).append("\n");
        }
    }
    
    public record ParsedCommand(
            String id,
            CommandMetadata metadata,
            String content
    ) {}

    private record ParsedFrontmatter(
            String id,
            CommandMetadata metadata
    ) {}
    
    public record CommandMetadata(
            String name,
            String description,
            String icon,
            String model,
            State state,
            ShortcutKeys pasteShortcut,
            ShortcutKeys displayShortcut,
            ShortcutKeys executeShortcut,
            CommandVisibility visibility
    ) {}
}
