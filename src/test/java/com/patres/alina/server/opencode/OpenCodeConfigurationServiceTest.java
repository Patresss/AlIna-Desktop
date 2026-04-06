package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.settings.AssistantSettings;
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

class OpenCodeConfigurationServiceTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void tearDown() throws Exception {
        setAppPathsBaseDir(null);
    }

    @Test
    void shouldBuildGlobalConfigFromNativeOpenCodeConfigAndMergeTrustedDirectories() throws Exception {
        final Path appData = tempDir.resolve("app-data");
        final Path workdir = tempDir.resolve("workdir");
        setAppPathsBaseDir(appData);
        Files.createDirectories(workdir);
        writeOpenCodeConfig("""
                {
                  "$schema": "https://opencode.ai/config.json",
                  "permission": {
                    "*": "ask",
                    "websearch": "allow",
                    "bash": {
                      "git status *": "allow"
                    }
                  }
                }
                """);

        final OpenCodeConfigurationService service = newService(workdir);

        final ObjectNode config = service.buildGlobalConfig();

        assertEquals("allow", config.path("permission").path("websearch").asText());
        assertEquals("allow", config.path("permission").path("bash").path("git status").asText());
        assertEquals("allow", config.path("permission").path("bash").path("git status *").asText());
        assertEquals("allow", config.path("permission").path("external_directory").path(appData.toString() + "/**").asText());
        assertEquals("allow", config.path("permission").path("external_directory").path(workdir.toString() + "/**").asText());
        assertTrue(config.path("skills").isMissingNode());
    }

    @Test
    void shouldKeepReadPermissionAsGlobalAllowWhenSpecificAllowPatternsExist() throws Exception {
        final Path appData = tempDir.resolve("app-data");
        setAppPathsBaseDir(appData);
        writeOpenCodeConfig("""
                {
                  "$schema": "https://opencode.ai/config.json",
                  "permission": {
                    "read": {
                      "*": "allow",
                      "/tmp/example.md": "allow"
                    }
                  }
                }
                """);

        final OpenCodeConfigurationService service = newService(tempDir.resolve("workdir"));

        final ObjectNode config = service.buildGlobalConfig();

        assertEquals("allow", config.path("permission").path("read").asText());
    }

    @Test
    void shouldNormalizeUnsupportedPermissionObjectsToScalarDecision() throws Exception {
        final Path appData = tempDir.resolve("app-data");
        setAppPathsBaseDir(appData);
        writeOpenCodeConfig("""
                {
                  "$schema": "https://opencode.ai/config.json",
                  "permission": {
                    "edit": {
                      "*": "ask",
                      "/tmp/example.md": "allow"
                    },
                    "webfetch": {
                      "*": "ask",
                      "https://example.com": "allow"
                    }
                  }
                }
                """);

        final OpenCodeConfigurationService service = newService(tempDir.resolve("workdir"));

        final ObjectNode config = service.buildGlobalConfig();

        assertEquals("allow", config.path("permission").path("edit").asText());
        assertEquals("allow", config.path("permission").path("webfetch").asText());
    }

    private OpenCodeConfigurationService newService(final Path workingDirectory) {
        @SuppressWarnings("unchecked")
        final FileManager<WorkspaceSettings> workspaceSettingsManager = mock(FileManager.class);
        @SuppressWarnings("unchecked")
        final FileManager<AssistantSettings> assistantSettingsManager = mock(FileManager.class);
        when(assistantSettingsManager.getSettings()).thenReturn(new AssistantSettings());
        when(workspaceSettingsManager.getSettings()).thenReturn(new WorkspaceSettings(
                true,
                false,
                true,
                WorkspaceSettings.DEFAULT_TASKS_FILE,
                WorkspaceSettings.DEFAULT_DASHBOARD_TASK_LIMIT,
                WorkspaceSettings.DEFAULT_OPENCODE_HOSTNAME,
                WorkspaceSettings.DEFAULT_OPENCODE_PORT,
                workingDirectory.toString()
        ));

        return new OpenCodeConfigurationService(
                workspaceSettingsManager,
                assistantSettingsManager,
                new ObjectMapper()
        );
    }

    private void writeOpenCodeConfig(final String content) throws Exception {
        final Path config = OpenCodePaths.configFile();
        Files.createDirectories(config.getParent());
        Files.writeString(config, content);
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
