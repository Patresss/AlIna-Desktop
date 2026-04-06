package com.patres.alina.server.dashboard;

import com.patres.alina.common.dashboard.DashboardState;
import com.patres.alina.common.dashboard.DashboardTask;
import com.patres.alina.common.dashboard.DashboardTaskUpdateRequest;
import com.patres.alina.common.event.DashboardUpdatedEvent;
import com.patres.alina.common.event.Event;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.storage.AppPaths;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private static final Pattern ACTIVE_TASK_PATTERN = Pattern.compile("^(.*?- \\[ \\]\\s+)(.+)$");
    private static final Pattern TOGGLE_TASK_PATTERN = Pattern.compile("^(.*?- \\[)( |x|X)(\\]\\s+.*)$");

    private final FileManager<WorkspaceSettings> workspaceSettingsManager;

    public DashboardService(final FileManager<WorkspaceSettings> workspaceSettingsManager) {
        this.workspaceSettingsManager = workspaceSettingsManager;
    }

    @PostConstruct
    void initialize() {
        ensureTaskFile();
    }

    public DashboardState getState() {
        ensureTaskFile();
        final WorkspaceSettings settings = workspaceSettingsManager.getSettings();
        return new DashboardState(
                settings.showDashboard(),
                settings.dashboardCollapsed(),
                readActiveTasks(resolveTasksFile(settings), settings.dashboardTaskLimit())
        );
    }

    public void updateTask(final DashboardTaskUpdateRequest request) {
        if (request == null || request.sourceFile() == null || request.lineNumber() <= 0) {
            return;
        }
        setTaskCompleted(Paths.get(request.sourceFile()).normalize(), request.lineNumber(), request.completed());
    }

    private List<DashboardTask> readActiveTasks(final Path tasksFile, final int limit) {
        ensureTextFile(tasksFile, defaultTasksContent());
        try {
            final List<String> lines = Files.readAllLines(tasksFile, StandardCharsets.UTF_8);
            final List<DashboardTask> tasks = new ArrayList<>();
            for (int i = 0; i < lines.size() && tasks.size() < Math.max(1, limit); i++) {
                final Matcher matcher = ACTIVE_TASK_PATTERN.matcher(lines.get(i));
                if (!matcher.matches()) {
                    continue;
                }
                tasks.add(new DashboardTask(
                        matcher.group(2).trim(),
                        tasksFile.toString(),
                        i + 1
                ));
            }
            return tasks;
        } catch (IOException e) {
            logger.warn("Cannot read dashboard tasks from {}", tasksFile, e);
            return List.of();
        }
    }

    private void setTaskCompleted(final Path sourceFile, final int lineNumber, final boolean completed) {
        try {
            final List<String> lines = Files.readAllLines(sourceFile, StandardCharsets.UTF_8);
            if (lineNumber > lines.size()) {
                return;
            }
            final Matcher matcher = TOGGLE_TASK_PATTERN.matcher(lines.get(lineNumber - 1));
            if (!matcher.matches()) {
                return;
            }
            lines.set(lineNumber - 1, matcher.group(1) + (completed ? "x" : " ") + matcher.group(3));
            Files.write(sourceFile, lines, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
            Event.publish(new DashboardUpdatedEvent());
        } catch (IOException e) {
            logger.warn("Cannot update dashboard task at {}:{}", sourceFile, lineNumber, e);
        }
    }

    private void ensureTaskFile() {
        ensureDirectory(AppPaths.resolve("profile/default"));
        ensureTextFile(resolveTasksFile(workspaceSettingsManager.getSettings()), defaultTasksContent());
    }

    private void ensureTextFile(final Path file, final String content) {
        ensureParentDirectory(file);
        if (Files.exists(file)) {
            return;
        }
        try {
            Files.writeString(file, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            logger.warn("Cannot create dashboard task file {}", file, e);
        }
    }

    private void ensureDirectory(final Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            logger.warn("Cannot create directory {}", directory, e);
        }
    }

    private void ensureParentDirectory(final Path file) {
        final Path parent = file.getParent();
        if (parent != null) {
            ensureDirectory(parent);
        }
    }

    private Path resolveTasksFile(final WorkspaceSettings settings) {
        final String configuredValue = settings.tasksFile();
        final String value = configuredValue == null || configuredValue.isBlank()
                ? WorkspaceSettings.DEFAULT_TASKS_FILE
                : configuredValue.trim();
        final Path raw = Paths.get(value);
        if (raw.isAbsolute()) {
            return raw.normalize();
        }
        return AppPaths.resolve(value);
    }

    private String defaultTasksContent() {
        return """
                # Focus Tasks

                - [ ] Ustaw plik tasków pod swój workflow
                """;
    }
}
