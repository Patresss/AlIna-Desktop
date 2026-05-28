package com.patres.alina.server.command;

import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.storage.AiWorkspacePaths;
import com.patres.alina.server.parser.MarkdownParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CommandFileServiceTest {

    private static final String COMMANDS_DIRECTORY = "commands";

    @TempDir
    Path tempDir;

    private final MarkdownParser markdownParser = new MarkdownParser();

    @Test
    void shouldLoadAgentsCommandsBeforeLegacyOpenCodeCommands() throws Exception {
        final Path workdir = tempDir.resolve("workspace");
        final Path agentsCommands = agentsCommandsDirectory(workdir);
        final Path openCodeCommands = openCodeCommandsDirectory(workdir);
        writeCommand(agentsCommands.resolve("agents-command.md"), "agents-command", "Agents Command");
        writeCommand(openCodeCommands.resolve("opencode-command.md"), "opencode-command", "OpenCode Command");

        final CommandFileService service = newService(workdir);

        final List<Command> commands = service.getAllCommands();

        assertEquals(1, commands.size());
        assertEquals("agents-command", commands.getFirst().id());
    }

    @Test
    void shouldLoadLegacyOpenCodeCommandsWhenAgentsDirectoryIsMissing() throws Exception {
        final Path workdir = tempDir.resolve("workspace");
        final Path openCodeCommands = openCodeCommandsDirectory(workdir);
        writeCommand(openCodeCommands.resolve("opencode-command.md"), "opencode-command", "OpenCode Command");

        final CommandFileService service = newService(workdir);

        final List<Command> commands = service.getAllCommands();

        assertEquals(1, commands.size());
        assertEquals("opencode-command", commands.getFirst().id());
    }

    @Test
    void shouldNotLoadGlobalOpenCodeCommandsWhenWorkspaceHasNoCommandDirectory() throws Exception {
        final Path workdir = tempDir.resolve("workspace");
        final Path globalCommands = tempDir.resolve("global-commands");
        writeCommand(globalCommands.resolve("global-command.md"), "global-command", "Global Command");

        final CommandFileService service = newService(workdir, globalCommands);

        assertTrue(service.getAllCommands().isEmpty());
    }

    @Test
    void shouldWriteNewCommandToExistingAgentsDirectory() throws Exception {
        final Path workdir = tempDir.resolve("workspace");
        final Path agentsCommands = agentsCommandsDirectory(workdir);
        final Path openCodeCommands = openCodeCommandsDirectory(workdir);
        Files.createDirectories(agentsCommands);
        Files.createDirectories(openCodeCommands);

        final CommandFileService service = newService(workdir);

        service.create(new Command("New Command", "Description", "Prompt", "bi-slash"));

        assertTrue(Files.exists(agentsCommands.resolve("new-command.md")));
        assertFalse(Files.exists(openCodeCommands.resolve("new-command.md")));
    }

    @Test
    void shouldWriteNewCommandToLegacyOpenCodeDirectoryWhenAgentsDirectoryIsMissing() throws Exception {
        final Path workdir = tempDir.resolve("workspace");
        final Path agentsCommands = agentsCommandsDirectory(workdir);
        final Path openCodeCommands = openCodeCommandsDirectory(workdir);
        Files.createDirectories(openCodeCommands);

        final CommandFileService service = newService(workdir);

        service.create(new Command("New Command", "Description", "Prompt", "bi-slash"));

        assertFalse(Files.exists(agentsCommands.resolve("new-command.md")));
        assertTrue(Files.exists(openCodeCommands.resolve("new-command.md")));
    }

    @Test
    void shouldCreateAgentsDirectoryForNewCommandWhenNoWorkspaceCommandDirectoryExists() {
        final Path workdir = tempDir.resolve("workspace");
        final Path agentsCommands = agentsCommandsDirectory(workdir);
        final Path openCodeCommands = openCodeCommandsDirectory(workdir);

        final CommandFileService service = newService(workdir);

        service.create(new Command("New Command", "Description", "Prompt", "bi-slash"));

        assertTrue(Files.exists(agentsCommands.resolve("new-command.md")));
        assertFalse(Files.exists(openCodeCommands.resolve("new-command.md")));
    }

    private CommandFileService newService(final Path workingDirectory) {
        return newService(workingDirectory, tempDir.resolve("global-commands"));
    }

    private CommandFileService newService(final Path workingDirectory, final Path globalCommandsDirectory) {
        return new CommandFileService(
                globalCommandsDirectory,
                markdownParser,
                workspaceSettingsManager(workingDirectory)
        );
    }

    private FileManager<WorkspaceSettings> workspaceSettingsManager(final Path workingDirectory) {
        @SuppressWarnings("unchecked")
        final FileManager<WorkspaceSettings> workspaceSettingsManager = mock(FileManager.class);
        when(workspaceSettingsManager.getSettings()).thenReturn(workspaceSettings(workingDirectory));
        return workspaceSettingsManager;
    }

    private WorkspaceSettings workspaceSettings(final Path workingDirectory) {
        return new WorkspaceSettings(
                true,
                false,
                true,
                WorkspaceSettings.DEFAULT_TASKS_FILE,
                WorkspaceSettings.DEFAULT_DASHBOARD_TASK_LIMIT,
                "",
                WorkspaceSettings.DEFAULT_OPENCODE_HOSTNAME,
                WorkspaceSettings.DEFAULT_OPENCODE_PORT,
                workingDirectory.toString(),
                "",
                WorkspaceSettings.DEFAULT_DASHBOARD_TASKS_REFRESH_SECONDS,
                WorkspaceSettings.DEFAULT_DASHBOARD_GITHUB_REFRESH_SECONDS,
                WorkspaceSettings.DEFAULT_DASHBOARD_MEDIA_REFRESH_SECONDS,
                WorkspaceSettings.DEFAULT_DASHBOARD_GITHUB_PR_LIMIT,
                WorkspaceSettings.DEFAULT_DASHBOARD_JIRA_REFRESH_SECONDS,
                WorkspaceSettings.DEFAULT_DASHBOARD_JIRA_ISSUE_LIMIT,
                "",
                "",
                true,
                true,
                true,
                true,
                true,
                WorkspaceSettings.DEFAULT_DASHBOARD_CALENDAR_REFRESH_SECONDS,
                false,
                true,
                false,
                WorkspaceSettings.DEFAULT_CALENDAR_NOTIFICATION_MINUTES_BEFORE,
                true,
                true,
                true,
                false,
                "",
                "",
                "",
                "",
                false,
                "",
                WorkspaceSettings.DEFAULT_DASHBOARD_OBSIDIAN_NOTE_LIMIT,
                WorkspaceSettings.DEFAULT_DASHBOARD_OBSIDIAN_REFRESH_SECONDS,
                false,
                "",
                ""
        );
    }

    private Path agentsCommandsDirectory(final Path workingDirectory) {
        return AiWorkspacePaths.agentsDirectory(workingDirectory)
                .resolve(COMMANDS_DIRECTORY)
                .normalize();
    }

    private Path openCodeCommandsDirectory(final Path workingDirectory) {
        return AiWorkspacePaths.openCodeDirectory(workingDirectory)
                .resolve(COMMANDS_DIRECTORY)
                .normalize();
    }

    private void writeCommand(final Path file, final String id, final String name) throws Exception {
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                ---
                id: %s
                name: "%s"
                description: "Description"
                icon: bi-slash
                state: ENABLED
                ---

                Prompt
                """.formatted(id, name));
    }
}
