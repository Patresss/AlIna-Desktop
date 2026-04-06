package com.patres.alina.server.workspace;

import com.patres.alina.common.opencode.OpenCodeRuntimeStatus;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.opencode.OpenCodeRuntimeService;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceController {

    private final FileManager<WorkspaceSettings> workspaceSettingsManager;
    private final OpenCodeRuntimeService openCodeRuntimeService;

    public WorkspaceController(final FileManager<WorkspaceSettings> workspaceSettingsManager,
                               final OpenCodeRuntimeService openCodeRuntimeService) {
        this.workspaceSettingsManager = workspaceSettingsManager;
        this.openCodeRuntimeService = openCodeRuntimeService;
    }

    public WorkspaceSettings getWorkspaceSettings() {
        return workspaceSettingsManager.getSettings();
    }

    public void updateWorkspaceSettings(final WorkspaceSettings settings) {
        workspaceSettingsManager.saveDocument(settings);
    }

    public OpenCodeRuntimeStatus getOpenCodeRuntimeStatus() {
        return openCodeRuntimeService.getRuntimeStatus();
    }

    public void prepareOpenCodeForFreshChat() {
        openCodeRuntimeService.prepareForFreshChat();
    }
}
