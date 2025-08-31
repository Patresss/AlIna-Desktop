# PRP: Migrate Plugin System from MongoDB to File-Based Commands

## Overview

**Objective**: Migrate the existing MongoDB-based plugin system to a file-based command system using Markdown files stored in the `commands/` folder, compatible with both the AlIna application and Claude Code.

**Context**: The application currently stores plugins in MongoDB but needs to move to a file-based approach for better version control, portability, and Claude Code compatibility.

## Current State Analysis

### Existing Plugin Architecture

#### Plugin Entity Structure (`com.patres.alina.server.plugin.Plugin`)
```java
public record Plugin(
    @Id String id,           // UUID generated ID
    String name,             // Display name
    String description,      // User description
    String systemPrompt,     // The actual prompt content
    String icon,             // Icon identifier (e.g., "bi-slash")
    State state              // ENABLED/DISABLED
) {
    // Constructor with defaults
}
```

#### Current MongoDB Components
- **PluginService**: Business logic layer with CRUD operations
- **PluginRepository**: `MongoRepository<Plugin, String>` interface
- **PluginController**: REST endpoints (`/plugins/*`)
- **PluginMapper**: Entity-DTO conversions

#### Data Flow
1. UI calls REST endpoints
2. Controller → Service → Repository → MongoDB
3. Returns `List<CardListItem>` for list view
4. Returns `PluginDetail` for editing

### Existing File Storage Patterns

The application already has a robust file-based storage system:

#### LocalStorageConfiguration Pattern
```java
@Configuration
public class LocalStorageConfiguration {
    @Bean
    @ConditionalOnProperty(name = "storage.type", havingValue = "local")
    public Path localStorageBasePath() {
        // Creates and manages base storage directory
    }
}
```

#### JsonLinesRepository Pattern
- **Base Class**: `JsonLinesRepository<T extends Entity<ID>, ID>`
- **Features**: CRUD operations, append-optimized for new entries
- **Atomic Operations**: Uses temp files and atomic moves
- **Error Handling**: Comprehensive logging and exception handling

### Command Format Analysis

#### Current Example (`commands/translate-to-english.md`)
```markdown
---
name: "Translate to English"
description: "Przetłumacz poniższy tekst na angielski."
icon: mdal-g_translate
state: ENABLED
---

You are a helpful translator. Translate the following Polish text to English.
Use natural, concise phrasing and keep technical terms accurate.
"""
$ARGUMENTS
"""
```

## Target Architecture Design

### File-Based Command Structure

#### Directory Layout
```
commands/
├── translate-to-english.md
├── code-review.md
├── documentation.md
└── [other-commands].md
```

#### Command File Format
```markdown
---
name: "Command Display Name"
description: "Brief description for UI"
icon: icon-identifier
state: ENABLED|DISABLED
---

System prompt content here.
Use $ARGUMENTS placeholder for user input.
```

### New Components Design

#### CommandFileService
```java
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class CommandFileService implements PluginService {
    
    private final Path commandsDirectory;
    private final ObjectMapper yamlMapper; // For frontmatter parsing
    
    // Implement all methods from current PluginService interface
    public List<CardListItem> getPluginListItems()
    public Optional<PluginDetail> getPluginDetails(String pluginId) 
    public String createPluginDetail(PluginCreateRequest request)
    public String updatePluginDetail(PluginDetail detail)
    public void deletePlugin(String pluginId)
    public void updatePluginState(UpdateStateRequest request)
}
```

#### Command Entity
```java
public record Command(
    String id,              // filename without extension
    String name,            // from frontmatter
    String description,     // from frontmatter  
    String systemPrompt,    // markdown content after frontmatter
    String icon,            // from frontmatter, default "bi-slash"
    State state             // from frontmatter, default ENABLED
) implements Entity<String> {
    
    public String getId() { return id; }
}
```

## Implementation Blueprint

### Phase 1: Setup Command Storage Infrastructure

#### 1.1 Create Commands Directory Configuration
```java
@Bean
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public Path commandsStoragePath(Path localStorageBasePath) {
    Path commandsDir = localStorageBasePath.resolve("../commands").normalize();
    Files.createDirectories(commandsDir);
    return commandsDir;
}
```

#### 1.2 Create Command File Parser
- Use Jackson YAML mapper for frontmatter parsing
- Split file content: frontmatter vs. markdown content
- Handle missing/malformed frontmatter gracefully
- Default values: icon="bi-slash", state=ENABLED

#### 1.3 Create Command Entity and Mapper
- Implement `Command` record with `Entity<String>` interface
- Create `CommandMapper` similar to existing `PluginMapper`
- Map filename to ID (without .md extension)

### Phase 2: Implement File-Based Service

#### 2.1 CommandFileService Implementation
```java
@Primary
@ConditionalOnProperty(name = "storage.type", havingValue = "local")
public class CommandFileService implements PluginService {
    
    public List<CardListItem> getPluginListItems() {
        return Files.list(commandsDirectory)
            .filter(path -> path.toString().endsWith(".md"))
            .map(this::parseCommandFile)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .sorted(comparator) // State DESC, name ASC 
            .map(CommandMapper::toCardListItem)
            .collect(toList());
    }
    
    private Optional<Command> parseCommandFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            return parseMarkdownWithFrontmatter(content, getFileId(filePath));
        } catch (IOException e) {
            logger.warn("Failed to parse command file: {}", filePath, e);
            return Optional.empty();
        }
    }
}
```

#### 2.2 File Operations
- **Read**: Parse .md files with frontmatter
- **Create**: Generate filename from name, write with frontmatter
- **Update**: Modify frontmatter while preserving content
- **Delete**: Remove .md file
- **State Update**: Modify only frontmatter state field

#### 2.3 Error Handling and Validation
- Invalid frontmatter → use defaults, log warning
- Missing files → return empty Optional
- File I/O errors → throw service exceptions
- Invalid state values → default to ENABLED

### Phase 3: Maintain API Compatibility

#### 3.1 Service Interface Compatibility
- Maintain all existing `PluginService` method signatures
- Return same DTOs (`CardListItem`, `PluginDetail`)
- Preserve REST API contract completely
- No changes required in Controller or UI

#### 3.2 ID Strategy
- Use filename (without .md) as plugin ID
- Generate safe filenames from plugin names
- Handle special characters and duplicates
- Maintain bidirectional ID ↔ filename mapping

### Phase 4: Configuration and Deployment

#### 4.1 Migration Strategy
- No migration of existing MongoDB plugins (as requested)
- Both systems can coexist during transition
- Configuration flag determines which storage to use

## Critical Implementation Details

### Frontmatter Parsing
```java
public class MarkdownParser {
    
    private static final String FRONTMATTER_DELIMITER = "---";
    
    public ParsedCommand parseMarkdownWithFrontmatter(String content, String id) {
        if (!content.startsWith(FRONTMATTER_DELIMITER)) {
            return new ParsedCommand(id, getDefaults(), content);
        }
        
        int endIndex = content.indexOf(FRONTMATTER_DELIMITER, 3);
        if (endIndex == -1) {
            return new ParsedCommand(id, getDefaults(), content);
        }
        
        String frontmatterYaml = content.substring(3, endIndex).trim();
        String markdownContent = content.substring(endIndex + 3).trim();
        
        CommandMetadata metadata = parseYaml(frontmatterYaml);
        return new ParsedCommand(id, metadata, markdownContent);
    }
}
```

### Safe Filename Generation
```java
public class FileUtils {
    public static String generateSafeFilename(String name) {
        return name.toLowerCase()
                  .replaceAll("[^a-z0-9\\-_]", "-")
                  .replaceAll("-+", "-")
                  .replaceAll("^-|-$", "");
    }
}
```

### Default Values Handling
```java
private CommandMetadata getDefaultMetadata(String name) {
    return CommandMetadata.builder()
        .name(name)
        .description("")
        .icon("bi-slash")  // Default as specified
        .state(State.ENABLED)
        .build();
}
```

## Validation Gates

### Build and Compilation
```bash
# Ensure code compiles
JAVA_HOME="/Users/patrykpiechaczek/Library/Java/JavaVirtualMachines/temurin-24.0.2/Contents/Home" ./gradlew build
```

### Unit Tests
```bash
# Run all tests including new command file service tests
JAVA_HOME="/Users/patrykpiechaczek/Library/Java/JavaVirtualMachines/temurin-24.0.2/Contents/Home" ./gradlew test
```

### Integration Tests
```bash
# Test application startup with local storage
JAVA_HOME="/Users/patrykpiechaczek/Library/Java/JavaVirtualMachines/temurin-24.0.2/Contents/Home" ./gradlew run
```

### Command Validation
- Create test command file with all metadata fields
- Verify UI shows commands correctly
- Test command execution with $ARGUMENTS replacement
- Validate state changes persist to files
- Confirm Claude Code compatibility

## Implementation Sequence

1. **Setup Infrastructure** (30 min)
   - Create `CommandFileService` class skeleton
   - Add `commandsStoragePath` bean configuration
   - Create `Command` record and `CommandMapper`

2. **Implement File Parsing** (45 min)
   - Create `MarkdownParser` for frontmatter handling
   - Implement safe filename generation utilities
   - Add comprehensive error handling

3. **Service Implementation** (60 min)
   - Implement all `PluginService` methods in `CommandFileService`
   - Add proper logging and exception handling
   - Ensure alphabetical sorting with state priority

4. **Configuration Integration** (15 min)
   - Wire up conditional beans with `@ConditionalOnProperty`
   - Test configuration switching between storage types

5. **Testing and Validation** (45 min)
   - Create unit tests for file operations
   - Test error scenarios (malformed files, missing metadata)
   - Validate UI integration works correctly

## Code References

- **Current Plugin System**: `src/main/java/com/patres/alina/server/plugin/`
- **Local Storage Pattern**: `src/main/java/com/patres/alina/server/configuration/LocalStorageConfiguration.java`
- **File Repository Pattern**: `src/main/java/com/patres/alina/server/storage/JsonLinesRepository.java`
- **Example Command**: `/Users/patrykpiechaczek/Programming/Repositories/AlInaDesktop/commands/translate-to-english.md`
- **Configuration**: `/Users/patrykpiechaczek/Programming/Repositories/AlInaDesktop/config/application.yml`

## External Resources

- **Claude Code Commands Documentation**: https://docs.anthropic.com/en/docs/claude-code/common-workflows
- **Markdown Frontmatter Spec**: https://jekyllrb.com/docs/front-matter/
- **Jackson YAML Parsing**: https://github.com/FasterXML/jackson-dataformats-text

## Success Criteria

- [ ] Plugin list shows commands from .md files
- [ ] Commands can be created/edited/deleted via UI
- [ ] State changes persist to file frontmatter
- [ ] Commands work with $ARGUMENTS placeholder
- [ ] Files are compatible with Claude Code
- [ ] All tests pass
- [ ] No regression in existing functionality

## Confidence Score: 9/10

This PRP provides comprehensive context for one-pass implementation success:

- **Detailed Architecture**: Clear understanding of current and target systems
- **Existing Patterns**: Leverages proven local storage patterns from codebase
- **Complete Code Examples**: Specific implementation details with error handling
- **Validation Strategy**: Executable tests and validation gates
- **External Context**: Claude Code compatibility requirements and documentation
- **Risk Mitigation**: Comprehensive error handling and fallback strategies

The implementation follows established patterns in the codebase and maintains full API compatibility, minimizing risk of breaking changes.