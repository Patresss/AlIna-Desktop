package com.patres.alina.server.command;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.State;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.server.parser.MarkdownParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class CommandFileService {

    private static final Logger logger = LoggerFactory.getLogger(CommandFileService.class);

    private final Path commandsDirectory;
    private final MarkdownParser markdownParser;

    public CommandFileService(final Path commandsStoragePath, final MarkdownParser markdownParser) {
        this.commandsDirectory = commandsStoragePath;
        this.markdownParser = markdownParser;
        logger.info("CommandFileService initialized with directory: {}", commandsDirectory);
    }

    public List<CardListItem> getListCardItems() {
        logger.debug("Loading command list from commands directory: {}", commandsDirectory);

        if (!Files.exists(commandsDirectory)) {
            logger.warn("Commands directory does not exist: {}", commandsDirectory);
            return List.of();
        }

        try (Stream<Path> paths = Files.list(commandsDirectory)) {
            final List<Command> commands = paths
                    .filter(path -> path.toString().endsWith(".md"))
                    .map(this::parseCommandFile)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .sorted(createCommandComparator())
                    .toList();

            final List<CardListItem> items = commands.stream()
                    .map(CommandFileService::toCardListItem)
                    .toList();
            logger.debug("Loaded {} commands from files", items.size());
            return items;

        } catch (IOException e) {
            logger.error("Failed to list files in commands directory: {}", commandsDirectory, e);
            return List.of();
        }
    }

    public Optional<Command> findById(final String commandId) {
        logger.debug("Loading command by id: {}", commandId);

        final Path commandFile = getCommandFilePath(commandId);
        if (!Files.exists(commandFile)) {
            logger.warn("Command file not found for id: {}, path: {}", commandId, commandFile);
            return Optional.empty();
        }

        return parseCommandFile(commandFile);
    }

    public String create(final Command request) {
        logger.info("Creating new command: {}", request);
        final String filename = generateUniqueFilename(request.name());
        final Command command = new Command(
                filename,
                request.name(),
                request.description(),
                request.systemPrompt(),
                request.icon(),
                State.ENABLED
        );

        final Path commandFile = commandsDirectory.resolve(filename + ".md");

        try {
            final String content = markdownParser.generateMarkdownWithFrontmatter(command);
            Files.writeString(commandFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            logger.info("Created command file: {} for command: {}", commandFile, command.name());
            return command.id();

        } catch (IOException e) {
            logger.error("Failed to create command file: {}", commandFile, e);
            throw new RuntimeException("Failed to create command file", e);
        }
    }

    public String update(final Command command) {
        logger.info("Updating command: {}", command.id());

        final Path commandFile = getCommandFilePath(command.id());

        if (!Files.exists(commandFile)) {
            throw new IllegalArgumentException("Command file not found for id: " + command.id());
        }

        try {
            final String content = markdownParser.generateMarkdownWithFrontmatter(command);
            Files.writeString(commandFile, content, StandardOpenOption.TRUNCATE_EXISTING);

            logger.info("Updated command file: {}", commandFile);
            return command.id();

        } catch (IOException e) {
            logger.error("Failed to update command file: {}", commandFile, e);
            throw new RuntimeException("Failed to update command file", e);
        }
    }

    public void deleteCommand(final String commandId) {
        logger.info("Deleting command: {}", commandId);

        final Path commandFile = getCommandFilePath(commandId);

        try {
            final boolean deleted = Files.deleteIfExists(commandFile);
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

    public void updateCommandState(final UpdateStateRequest updateStateRequest) {
        logger.info("Updating command state: {} -> {}", updateStateRequest.id(), updateStateRequest.state());

        final Optional<Command> existing = findById(updateStateRequest.id());
        if (existing.isEmpty()) {
            throw new IllegalArgumentException("Command not found: " + updateStateRequest.id());
        }

        final Command updated = new Command(
                existing.get().id(),
                existing.get().name(),
                existing.get().description(),
                existing.get().systemPrompt(),
                existing.get().icon(),
                updateStateRequest.state()
        );

        update(updated);
    }

    // Private helper methods

    private Optional<Command> parseCommandFile(final Path filePath) {
        try {
            final String content = Files.readString(filePath);
            final String id = getFileId(filePath);

            final MarkdownParser.ParsedCommand parsed = markdownParser.parseMarkdownWithFrontmatter(content, id);

            final Command command = new Command(
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

    private String getFileId(final Path filePath) {
        final String filename = filePath.getFileName().toString();
        return filename.endsWith(".md") ? filename.substring(0, filename.length() - 3) : filename;
    }

    private Path getCommandFilePath(final String commandId) {
        return commandsDirectory.resolve(commandId + ".md");
    }

    private String generateUniqueFilename(final String name) {
        final String baseFilename = Command.generateIdFromName(name);
        String filename = baseFilename;
        int counter = 1;

        while (Files.exists(commandsDirectory.resolve(filename + ".md"))) {
            filename = baseFilename + "-" + counter;
            counter++;
        }

        return filename;
    }

    private Comparator<Command> createCommandComparator() {
        // Sort by state (ENABLED first), then by name alphabetically
        return Comparator.comparing(Command::state)
                .thenComparing(Command::name, String.CASE_INSENSITIVE_ORDER);
    }

    private static CardListItem toCardListItem(final Command command) {
        return new CardListItem(
                command.id(),
                command.name(),
                command.description(),
                command.icon(),
                command.state()
        );
    }


}
