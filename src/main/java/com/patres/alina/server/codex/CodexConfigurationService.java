package com.patres.alina.server.codex;

import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.settings.WorkspaceSettings;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;

@Service
public class CodexConfigurationService {

    private final FileManager<WorkspaceSettings> workspaceSettingsManager;
    private final FileManager<AssistantSettings> assistantSettingsManager;

    public CodexConfigurationService(final FileManager<WorkspaceSettings> workspaceSettingsManager,
                                     final FileManager<AssistantSettings> assistantSettingsManager) {
        this.workspaceSettingsManager = workspaceSettingsManager;
        this.assistantSettingsManager = assistantSettingsManager;
    }

    public WorkspaceSettings workspaceSettings() {
        return workspaceSettingsManager.getSettings();
    }

    public AssistantSettings assistantSettings() {
        return assistantSettingsManager.getSettings();
    }

    public String command() {
        return workspaceSettings().codexCommand();
    }

    public String sandbox() {
        return workspaceSettings().codexSandbox();
    }

    public Path resolveWorkingDirectory() {
        return Path.of(workspaceSettings().codexWorkingDirectory()).toAbsolutePath().normalize();
    }

    public String resolveEffectiveModelIdentifier() {
        return toCodexModel(assistantSettings().chatModel());
    }

    public String toCodexModel(final String modelIdentifier) {
        if (modelIdentifier == null || modelIdentifier.isBlank()) {
            return AssistantSettings.DEFAULT_CHAT_MODEL;
        }
        final String trimmed = modelIdentifier.trim();
        final int slash = trimmed.lastIndexOf('/');
        return slash >= 0 ? trimmed.substring(slash + 1) : trimmed;
    }

    public Map<String, String> buildProcessEnvironment() {
        return Map.of();
    }
}
