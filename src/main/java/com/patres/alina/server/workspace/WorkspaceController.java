package com.patres.alina.server.workspace;

import com.patres.alina.common.agent.AgentRuntimeStatus;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.agent.AgentRuntimeSelector;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceController {

    private final FileManager<WorkspaceSettings> workspaceSettingsManager;
    private final AgentRuntimeSelector agentRuntimeSelector;

    public WorkspaceController(final FileManager<WorkspaceSettings> workspaceSettingsManager,
                               final AgentRuntimeSelector agentRuntimeSelector) {
        this.workspaceSettingsManager = workspaceSettingsManager;
        this.agentRuntimeSelector = agentRuntimeSelector;
    }

    public WorkspaceSettings getWorkspaceSettings() {
        return workspaceSettingsManager.getSettings();
    }

    public void updateWorkspaceSettings(final WorkspaceSettings settings) {
        workspaceSettingsManager.saveDocument(settings);
    }

    public AgentRuntimeStatus getAgentRuntimeStatus() {
        return agentRuntimeSelector.active().getRuntimeStatus();
    }

    public void prepareAgentForFreshChat() {
        agentRuntimeSelector.active().prepareForFreshChat();
    }

    public String getAgentSessionWebUrl(final String chatThreadId) {
        return agentRuntimeSelector.active().getSessionWebUrl(chatThreadId);
    }
}
