package com.patres.alina.server.workspace;

import com.patres.alina.common.ai.AiRuntimeStatus;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.ai.AiRuntimeRegistry;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceController {

    private final FileManager<WorkspaceSettings> workspaceSettingsManager;
    private final AiRuntimeRegistry aiRuntimeRegistry;

    public WorkspaceController(final FileManager<WorkspaceSettings> workspaceSettingsManager,
                               final AiRuntimeRegistry aiRuntimeRegistry) {
        this.workspaceSettingsManager = workspaceSettingsManager;
        this.aiRuntimeRegistry = aiRuntimeRegistry;
    }

    public WorkspaceSettings getWorkspaceSettings() {
        return workspaceSettingsManager.getSettings();
    }

    public void updateWorkspaceSettings(final WorkspaceSettings settings) {
        workspaceSettingsManager.saveDocument(settings);
    }

    public AiRuntimeStatus getAiRuntimeStatus() {
        return aiRuntimeRegistry.currentRuntime().getRuntimeStatus();
    }

    public void prepareAiRuntimeForFreshChat() {
        aiRuntimeRegistry.currentRuntime().prepareForFreshChat();
    }

    public String getAiSessionWebUrl(final String chatThreadId) {
        return aiRuntimeRegistry.currentRuntime().getSessionWebUrl(chatThreadId);
    }
}
