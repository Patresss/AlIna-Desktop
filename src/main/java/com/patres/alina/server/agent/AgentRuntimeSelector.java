package com.patres.alina.server.agent;

import com.patres.alina.common.agent.AgentBackend;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.settings.WorkspaceSettings;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentRuntimeSelector {

    private final FileManager<WorkspaceSettings> workspaceSettingsManager;
    private final Map<AgentBackend, AgentRuntime> runtimes;

    public AgentRuntimeSelector(final FileManager<WorkspaceSettings> workspaceSettingsManager,
                                final List<AgentRuntime> runtimes) {
        this.workspaceSettingsManager = workspaceSettingsManager;
        this.runtimes = new EnumMap<>(AgentBackend.class);
        runtimes.forEach(runtime -> this.runtimes.put(runtime.backend(), runtime));
    }

    public AgentRuntime active() {
        final AgentBackend configured = workspaceSettingsManager.getSettings().resolveAgentBackend();
        final AgentRuntime runtime = runtimes.get(configured);
        if (runtime != null) {
            return runtime;
        }
        final AgentRuntime fallback = runtimes.get(AgentBackend.OPENCODE);
        if (fallback != null) {
            return fallback;
        }
        throw new IllegalStateException("No agent runtime is configured.");
    }

    public List<AgentRuntime> all() {
        return List.copyOf(runtimes.values());
    }

    public AgentRuntime byBackend(final AgentBackend backend) {
        final AgentRuntime runtime = runtimes.get(backend);
        if (runtime == null) {
            throw new IllegalStateException("Agent runtime is not available: " + backend);
        }
        return runtime;
    }
}
