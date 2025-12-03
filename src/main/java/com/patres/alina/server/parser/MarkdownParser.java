package com.patres.alina.server.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.patres.alina.common.card.State;
import com.patres.alina.server.command.Command;
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
        
        CommandMetadata metadata = parseYaml(frontmatterYaml, id);
        return new ParsedCommand(id, metadata, markdownContent);
    }
    
    public String generateMarkdownWithFrontmatter(Command command) {
        StringBuilder sb = new StringBuilder();
        
        // Add frontmatter
        sb.append(FRONTMATTER_DELIMITER).append("\n");
        sb.append("name: \"").append(escapeYamlString(command.name())).append("\"\n");
        sb.append("description: \"").append(escapeYamlString(command.description())).append("\"\n");
        sb.append("icon: ").append(command.icon()).append("\n");
        sb.append("state: ").append(command.state().name()).append("\n");

        // Add globalShortcut if present (using ShortcutKeys structure)
        if (command.globalShortcut() != null && !command.globalShortcut().getAllKeys().isEmpty()) {
            sb.append("globalShortcut:\n");
            sb.append("  modeKeys:\n");
            for (KeyboardKey key : command.globalShortcut().getModeKeys()) {
                if (key != null) {
                    sb.append("    - ").append(key.name()).append("\n");
                } else {
                    sb.append("    - null\n");
                }
            }
            if (command.globalShortcut().getExecuteKey() != null) {
                sb.append("  executeKey: ").append(command.globalShortcut().getExecuteKey().name()).append("\n");
            }
        }

        sb.append(FRONTMATTER_DELIMITER).append("\n");
        sb.append("\n");

        // Add content
        if (command.systemPrompt() != null && !command.systemPrompt().trim().isEmpty()) {
            sb.append(command.systemPrompt());
        }

        return sb.toString();
    }
    
    private CommandMetadata parseYaml(String frontmatterYaml, String id) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> yamlMap = yamlMapper.readValue(frontmatterYaml, Map.class);

            String name = getStringValue(yamlMap, "name", id);
            String description = getStringValue(yamlMap, "description", "");
            String icon = getStringValue(yamlMap, "icon", "bi-slash");
            State state = parseState(getStringValue(yamlMap, "state", "ENABLED"));
            ShortcutKeys globalShortcut = parseGlobalShortcut(yamlMap);

            return new CommandMetadata(name, description, icon, state, globalShortcut);

        } catch (Exception e) {
            logger.warn("Failed to parse YAML frontmatter for command id: {}, using defaults. Error: {}", id, e.getMessage());
            return getDefaultMetadata(id);
        }
    }

    @SuppressWarnings("unchecked")
    private ShortcutKeys parseGlobalShortcut(Map<String, Object> yamlMap) {
        try {
            Object shortcutObj = yamlMap.get("globalShortcut");
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
            logger.warn("Failed to parse globalShortcut: {}", e.getMessage());
            return new ShortcutKeys();
        }
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
    
    private CommandMetadata getDefaultMetadata(String id) {
        return new CommandMetadata(
                id != null ? id.replace("-", " ") : "Unnamed Command",
                "",
                "bi-slash",
                State.ENABLED,
                new ShortcutKeys()
        );
    }
    
    private String escapeYamlString(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"").replace("\n", "\\n");
    }
    
    public record ParsedCommand(
            String id,
            CommandMetadata metadata,
            String content
    ) {}
    
    public record CommandMetadata(
            String name,
            String description,
            String icon,
            State state,
            ShortcutKeys globalShortcut
    ) {}
}
