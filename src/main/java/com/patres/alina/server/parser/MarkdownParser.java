package com.patres.alina.server.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.patres.alina.common.card.State;
import com.patres.alina.server.command.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
            
            return new CommandMetadata(name, description, icon, state);
            
        } catch (Exception e) {
            logger.warn("Failed to parse YAML frontmatter for command id: {}, using defaults. Error: {}", id, e.getMessage());
            return getDefaultMetadata(id);
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
                State.ENABLED
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
            State state
    ) {}
}