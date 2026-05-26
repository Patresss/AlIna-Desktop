package com.patres.alina.server.ai;

import com.patres.alina.common.ai.AiProvider;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.settings.WorkspaceSettings;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AiRuntimeRegistry {

    private final FileManager<WorkspaceSettings> workspaceSettingsManager;
    private final Map<AiProvider, AiRuntime> runtimes = new EnumMap<>(AiProvider.class);
    private final Map<AiProvider, AiSessionService> sessions = new EnumMap<>(AiProvider.class);

    public AiRuntimeRegistry(final FileManager<WorkspaceSettings> workspaceSettingsManager,
                             final List<AiRuntime> runtimes,
                             final List<AiSessionService> sessions) {
        this.workspaceSettingsManager = workspaceSettingsManager;
        runtimes.forEach(runtime -> this.runtimes.put(runtime.provider(), runtime));
        sessions.forEach(session -> this.sessions.put(session.provider(), session));
    }

    public AiProvider currentProvider() {
        return workspaceSettingsManager.getSettings().aiProvider();
    }

    public AiRuntime currentRuntime() {
        return runtime(currentProvider());
    }

    public AiSessionService currentSessionService() {
        return sessionService(currentProvider());
    }

    public AiRuntime runtime(final AiProvider provider) {
        final AiRuntime runtime = runtimes.get(provider);
        if (runtime == null) {
            throw new IllegalStateException("AI runtime is not registered: " + provider);
        }
        return runtime;
    }

    public AiSessionService sessionService(final AiProvider provider) {
        final AiSessionService sessionService = sessions.get(provider);
        if (sessionService == null) {
            throw new IllegalStateException("AI session service is not registered: " + provider);
        }
        return sessionService;
    }

    public void cancelStreaming(final String chatThreadId) {
        runtimes.values().forEach(runtime -> runtime.cancelStreaming(chatThreadId));
    }

    public Optional<AiRuntime> findPermissionOwner(final String requestId) {
        return runtimes.values().stream()
                .filter(runtime -> runtime.ownsPermissionRequest(requestId))
                .findFirst();
    }
}
