package com.patres.alina.server.command;

import com.patres.alina.common.card.State;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.server.parser.MarkdownParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.UUID;

@Service
public class CommandFileService {

    private static final Logger logger = LoggerFactory.getLogger(CommandFileService.class);

    private final Path commandsDirectory;
    private final MarkdownParser markdownParser;
    private final FileManager<WorkspaceSettings> workspaceSettingsManager;

    public CommandFileService(final Path commandsStoragePath, final MarkdownParser markdownParser) {
        this(commandsStoragePath, markdownParser, null);
    }

    @Autowired
    public CommandFileService(final Path commandsStoragePath,
                              final MarkdownParser markdownParser,
                              final FileManager<WorkspaceSettings> workspaceSettingsManager) {
        this.commandsDirectory = commandsStoragePath;
        this.markdownParser = markdownParser;
        this.workspaceSettingsManager = workspaceSettingsManager;
        logger.info("CommandFileService initialized with directory: {}", commandsDirectory);
    }

    public List<Command> getCommands() {
        return loadCommands(ListMode.ENABLED_ONLY);
    }

    public List<Command> getAllCommands() {
        return loadCommands(ListMode.ALL);
    }

    private enum ListMode { ENABLED_ONLY, ALL }

    private List<Command> loadCommands(final ListMode mode) {
        logger.debug("Loading command list from OpenCode command directories: {} (mode={})", resolveCommandDirectories(), mode);

        final Map<String, Command> commandsById = new LinkedHashMap<>();
        for (final Path directory : resolveCommandDirectories()) {
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (Stream<Path> paths = Files.list(directory)) {
                paths
                        .filter(path -> path.toString().endsWith(".md"))
                        .map(this::parseCommandFile)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(command -> mode == ListMode.ALL || command.state() == State.ENABLED)
                        .forEach(command -> commandsById.putIfAbsent(command.id(), command));
            } catch (IOException e) {
                logger.warn("Failed to list command files in directory: {}", directory, e);
            }
        }

        final List<Command> commands = commandsById.values().stream()
                .sorted(createCommandComparator())
                .toList();
        logger.debug("Loaded {} commands from OpenCode command files", commands.size());
        return commands;
    }

    public Optional<Command> findById(final String commandId) {
        logger.debug("Loading command by id: {}", commandId);

        final Path commandFile = resolveCommandFilePath(commandId);
        if (commandFile == null || !Files.exists(commandFile)) {
            logger.warn("Command file not found for id: {}, path: {}", commandId, commandFile);
            return Optional.empty();
        }

        return parseCommandFile(commandFile);
    }

    public String create(final Command request) {
        logger.info("Creating new command: {}", request);
        final String generatedId = UUID.randomUUID().toString();
        final Command command = new Command(
                generatedId,
                request.name(),
                request.description(),
                request.systemPrompt(),
                request.icon(),
                request.model(),
                State.ENABLED,
                request.pasteShortcut(),
                request.displayShortcut(),
                request.executeShortcut(),
                CommandVisibility.defaults(request.visibility())
        );

        final Path commandDirectory = resolveWriteDirectory();
        final Path commandFile = commandDirectory.resolve(generateUniqueFilename(commandDirectory, command.name()) + ".md");

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

        final Path existingCommandFile = resolveCommandFilePath(command.id());

        if (existingCommandFile == null || !Files.exists(existingCommandFile)) {
            throw new IllegalArgumentException("Command file not found for id: " + command.id());
        }

        try {
            final String content = markdownParser.generateMarkdownWithFrontmatter(command);
            final Path targetCommandFile = resolveTargetCommandFile(existingCommandFile, command);
            Files.writeString(targetCommandFile, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            if (!existingCommandFile.equals(targetCommandFile)) {
                Files.deleteIfExists(existingCommandFile);
            }

            logger.info("Updated command file: {}", targetCommandFile);
            return command.id();

        } catch (IOException e) {
            logger.error("Failed to update command file for id: {}", command.id(), e);
            throw new RuntimeException("Failed to update command file", e);
        }
    }

    public void deleteCommand(final String commandId) {
        logger.info("Deleting command: {}", commandId);

        final Path commandFile = resolveCommandFilePath(commandId);
        if (commandFile == null) {
            logger.warn("Command file not found for deletion: {}", commandId);
            return;
        }

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
                updateStateRequest.state(),
                existing.get().pasteShortcut(),
                existing.get().displayShortcut(),
                existing.get().executeShortcut(),
                existing.get().visibility()
        );

        update(updated);
    }

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
                    parsed.metadata().model(),
                    parsed.metadata().state(),
                    parsed.metadata().pasteShortcut(),
                    parsed.metadata().displayShortcut(),
                    parsed.metadata().executeShortcut(),
                    parsed.metadata().visibility()
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

    private Path resolveCommandFilePath(final String commandId) {
        for (final Path directory : resolveCommandDirectories()) {
            if (!Files.isDirectory(directory)) {
                continue;
            }
            try (Stream<Path> paths = Files.list(directory)) {
                final Optional<Path> match = paths
                        .filter(path -> path.toString().endsWith(".md"))
                        .filter(path -> fileMatchesCommandId(path, commandId))
                        .findFirst();
                if (match.isPresent()) {
                    return match.get();
                }
            } catch (IOException e) {
                logger.warn("Failed to resolve command file path for id: {}", commandId, e);
            }
        }
        return null;
    }

    private boolean fileMatchesCommandId(final Path filePath, final String commandId) {
        final Optional<Command> command = parseCommandFile(filePath);
        return command.map(value -> value.id().equals(commandId)).orElse(false);
    }

    private String generateUniqueFilename(final Path directory, final String name) {
        final String baseFilename = Command.generateIdFromName(name);
        String filename = baseFilename;
        int counter = 1;

        while (Files.exists(directory.resolve(filename + ".md"))) {
            filename = baseFilename + "-" + counter;
            counter++;
        }

        return filename;
    }

    private Path resolveTargetCommandFile(final Path currentFile, final Command command) {
        final String targetFilename = generateUniqueFilename(command.name(), currentFile);
        return currentFile.getParent().resolve(targetFilename + ".md");
    }

    private String generateUniqueFilename(final String name, final Path currentFile) {
        final String baseFilename = sanitizeFilename(name);
        String filename = baseFilename;
        int counter = 1;

        while (true) {
            final Path candidate = currentFile.getParent().resolve(filename + ".md");
            if (!Files.exists(candidate) || candidate.equals(currentFile)) {
                return filename;
            }
            filename = baseFilename + "-" + counter;
            counter++;
        }
    }

    private String sanitizeFilename(final String name) {
        final String generated = Command.generateIdFromName(name);
        return generated == null || generated.isBlank() ? "unnamed-command" : generated;
    }

    private Comparator<Command> createCommandComparator() {
        return Comparator.comparing(Command::name, String.CASE_INSENSITIVE_ORDER);
    }

    private List<Path> resolveCommandDirectories() {
        final java.util.ArrayList<Path> directories = new java.util.ArrayList<>();
        localOpenCodeCommandsDirectory().ifPresent(directories::add);
        if (directories.stream().noneMatch(commandsDirectory::equals)) {
            directories.add(commandsDirectory);
        }
        return List.copyOf(directories);
    }

    private Path resolveWriteDirectory() {
        final Optional<Path> localDirectory = localOpenCodeCommandsDirectory().filter(Files::isDirectory);
        final Path directory = localDirectory.orElse(commandsDirectory);
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create command directory: " + directory, e);
        }
        return directory;
    }

    private Optional<Path> localOpenCodeCommandsDirectory() {
        if (workspaceSettingsManager == null) {
            return Optional.empty();
        }
        final WorkspaceSettings settings = workspaceSettingsManager.getSettings();
        if (settings.openCodeWorkingDirectory() == null || settings.openCodeWorkingDirectory().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(Path.of(settings.openCodeWorkingDirectory()).toAbsolutePath().normalize()
                .resolve(".opencode")
                .resolve("commands")
                .normalize());
    }

}
