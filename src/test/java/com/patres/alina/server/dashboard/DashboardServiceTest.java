package com.patres.alina.server.dashboard;

import com.patres.alina.common.dashboard.DashboardTaskUpdateRequest;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.storage.AppPaths;
import com.patres.alina.common.storage.OpenCodePaths;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() throws Exception {
        setAppPathsBaseDir(null);
    }

    @Test
    void shouldReadOnlyUncheckedDashTasksFromConfiguredFile() throws Exception {
        final Path appData = tempDir.resolve("app-data");
        setAppPathsBaseDir(appData);
        final Path taskFile = appData.resolve("profile/default/focus.md");
        Files.createDirectories(taskFile.getParent());
        Files.writeString(taskFile, """
                # Focus

                - [ ] Kupić rower
                - [x] Zrobione zadanie
                - zwykły bullet
                * [ ] Inny format nie jest dashboard taskiem
                  - [ ] Zadanie z wcięciem
                """);

        final DashboardService service = new DashboardService(workspaceSettingsManager(new WorkspaceSettings()));

        final var state = service.getState();

        assertEquals(2, state.tasks().size());
        assertEquals("Kupić rower", state.tasks().get(0).title());
        assertEquals("Zadanie z wcięciem", state.tasks().get(1).title());
    }

    @Test
    void shouldMarkUncheckedTaskAsCompleted() throws Exception {
        final Path appData = tempDir.resolve("app-data");
        setAppPathsBaseDir(appData);
        final Path taskFile = appData.resolve("profile/default/focus.md");
        Files.createDirectories(taskFile.getParent());
        Files.writeString(taskFile, "- [ ] Kupić rower\n");

        final DashboardService service = new DashboardService(workspaceSettingsManager(new WorkspaceSettings()));
        final var task = service.getState().tasks().getFirst();

        service.updateTask(new DashboardTaskUpdateRequest(task.sourceFile(), task.lineNumber(), true));

        assertTrue(Files.readString(taskFile).contains("- [x] Kupić rower"));
    }

    private FileManager<WorkspaceSettings> workspaceSettingsManager(final WorkspaceSettings settings) {
        @SuppressWarnings("unchecked")
        final FileManager<WorkspaceSettings> workspaceSettingsManager = mock(FileManager.class);
        when(workspaceSettingsManager.getSettings()).thenReturn(settings);
        return workspaceSettingsManager;
    }

    private void setAppPathsBaseDir(final Path path) throws Exception {
        final Field field = AppPaths.class.getDeclaredField("cachedBaseDir");
        field.setAccessible(true);
        field.set(null, path);
        if (path == null) {
            System.clearProperty(OpenCodePaths.PROP_CONFIG_DIR);
        } else {
            System.setProperty(OpenCodePaths.PROP_CONFIG_DIR, path.resolve("opencode").toString());
        }
    }
}
