package com.patres.alina.server.plugin;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.plugin.PluginCreateRequest;
import com.patres.alina.common.plugin.PluginDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@Primary
@ConditionalOnProperty(name = "storage.type", havingValue = "local", matchIfMissing = false)
public class CommandFileService implements PluginServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(CommandFileService.class);

    private final Path commandsDirectory;
    private final MarkdownParser markdownParser;

    public CommandFileService(Path commandsStoragePath, MarkdownParser markdownParser) {
        this.commandsDirectory = commandsStoragePath;
        this.markdownParser = markdownParser;
        logger.info("CommandFileService initialized with directory: {}", commandsDirectory);
    }

    @Override
    public List<CardListItem> getPluginListItems() {
        logger.debug("Loading plugin list from commands directory: {}", commandsDirectory);
        
        if (!Files.exists(commandsDirectory)) {
            logger.warn("Commands directory does not exist: {}", commandsDirectory);
            return List.of();
        }

        try (Stream<Path> paths = Files.list(commandsDirectory)) {
            List<Command> commands = paths
                    .filter(path -> path.toString().endsWith(".md"))
                    .map(this::parseCommandFile)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted(createCommandComparator())
                    .toList();

            List<CardListItem> items = CommandMapper.toCardListItems(commands);
            logger.debug("Loaded {} commands from files", items.size());
            return items;

        } catch (IOException e) {
            logger.error("Failed to list files in commands directory: {}", commandsDirectory, e);
            return List.of();
        }
    }

    @Override
    public Optional<PluginDetail> getPluginDetails(String pluginId) {
        logger.debug("Loading plugin details for id: {}", pluginId);
        
        Path commandFile = getCommandFilePath(pluginId);
        if (!Files.exists(commandFile)) {
            logger.warn("Command file not found for id: {}, path: {}", pluginId, commandFile);
            return Optional.empty();
        }

        return parseCommandFile(commandFile)
                .map(CommandMapper::toPluginDetail);
    }

    @Override
    public String createPluginDetail(final PluginCreateRequest pluginCreateRequest) {
        logger.info("Creating new command: {}", pluginCreateRequest);
        
        Command command = CommandMapper.toCommand(pluginCreateRequest);
        String filename = generateUniqueFilename(command.name());
        
        // Update command with actual filename as ID
        command = new Command(
                filename,
                command.name(),
                command.description(),
                command.systemPrompt(),
                command.icon(),
                command.state()
        );
        
        Path commandFile = commandsDirectory.resolve(filename + ".md");
        
        try {
            String content = markdownParser.generateMarkdownWithFrontmatter(command);
            Files.writeString(commandFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            logger.info("Created command file: {} for command: {}", commandFile, command.name());
            return command.id();
            
        } catch (IOException e) {
            logger.error("Failed to create command file: {}", commandFile, e);
            throw new RuntimeException("Failed to create command file", e);
        }
    }

    @Override
    public String updatePluginDetail(final PluginDetail pluginDetail) {
        logger.info("Updating command: {}", pluginDetail.id());
        
        Command command = CommandMapper.toCommand(pluginDetail);
        Path commandFile = getCommandFilePath(command.id());
        
        if (!Files.exists(commandFile)) {
            throw new IllegalArgumentException("Command file not found for id: " + command.id());
        }
        
        try {
            String content = markdownParser.generateMarkdownWithFrontmatter(command);
            Files.writeString(commandFile, content, StandardOpenOption.TRUNCATE_EXISTING);
            
            logger.info("Updated command file: {}", commandFile);
            return command.id();
            
        } catch (IOException e) {
            logger.error("Failed to update command file: {}", commandFile, e);
            throw new RuntimeException("Failed to update command file", e);
        }
    }

    @Override
    public void deletePlugin(final String pluginId) {
        logger.info("Deleting command: {}", pluginId);
        
        Path commandFile = getCommandFilePath(pluginId);
        
        try {
            boolean deleted = Files.deleteIfExists(commandFile);
            if (deleted) {
                logger.info("Deleted command file: {}", commandFile);
            } else {
                logger.warn("Command file not found for deletion: {}", commandFile);
            }
        } catch (IOException e) {
            logger.error("Failed to delete command file: {}", commandFile, e);
            throw new RuntimeException("Failed to delete command file", e);
        }
    }

    @Override
    public void updatePluginState(final UpdateStateRequest updateStateRequest) {
        logger.info("Updating command state: {} -> {}", updateStateRequest.id(), updateStateRequest.state());
        
        Optional<PluginDetail> existing = getPluginDetails(updateStateRequest.id());
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Command not found: " + updateStateRequest.id());
        }
        
        PluginDetail updated = new PluginDetail(
                existing.get().id(),
                existing.get().name(),
                existing.get().description(),
                existing.get().systemPrompt(),
                existing.get().icon(),
                updateStateRequest.state()
        );
        
        updatePluginDetail(updated);
    }

    // Private helper methods

    private Optional<Command> parseCommandFile(Path filePath) {
        try {
            String content = Files.readString(filePath);
            String id = getFileId(filePath);
            
            MarkdownParser.ParsedCommand parsed = markdownParser.parseMarkdownWithFrontmatter(content, id);
            
            Command command = new Command(
                    parsed.id(),
                    parsed.metadata().name(),
                    parsed.metadata().description(),
                    parsed.content(),
                    parsed.metadata().icon(),
                    parsed.metadata().state()
            );
            
            return Optional.of(command);
            
        } catch (IOException e) {
            logger.warn("Failed to parse command file: {}", filePath, e);
            return Optional.empty();
        }
    }

    private String getFileId(Path filePath) {
        String filename = filePath.getFileName().toString();
        return filename.endsWith(".md") ? filename.substring(0, filename.length() - 3) : filename;
    }

    private Path getCommandFilePath(String pluginId) {
        return commandsDirectory.resolve(pluginId + ".md");
    }

    private String generateUniqueFilename(String name) {
        String baseFilename = generateSafeFilename(name);
        String filename = baseFilename;
        int counter = 1;
        
        while (Files.exists(commandsDirectory.resolve(filename + ".md"))) {
            filename = baseFilename + "-" + counter;
            counter++;
        }
        
        return filename;
    }

    private String generateSafeFilename(String name) {
        if (name == null || name.isBlank()) {
            return "unnamed-command";
        }
        return name.toLowerCase()
                  .replaceAll("[^a-z0-9\\-_]", "-")
                  .replaceAll("-+", "-")
                  .replaceAll("^-|-$", "");
    }

    private Comparator<Command> createCommandComparator() {
        // Sort by state (ENABLED first), then by name alphabetically
        return Comparator.comparing(Command::state, Comparator.reverseOrder())
                         .thenComparing(Command::name, String.CASE_INSENSITIVE_ORDER);
    }
}