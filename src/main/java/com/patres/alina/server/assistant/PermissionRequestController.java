package com.patres.alina.server.assistant;

import com.patres.alina.common.permission.PermissionApprovalAction;
import com.patres.alina.common.permission.PermissionResolutionModel;
import com.patres.alina.server.agent.AgentRuntime;
import com.patres.alina.server.agent.AgentRuntimeSelector;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import org.springframework.stereotype.Component;

@Component
public class PermissionRequestController {

    private final AgentRuntimeSelector agentRuntimeSelector;

    public PermissionRequestController(final AgentRuntimeSelector agentRuntimeSelector) {
        this.agentRuntimeSelector = agentRuntimeSelector;
    }

    public PermissionResolutionModel resolve(final String requestId,
                                             final PermissionApprovalAction action) {
        for (final AgentRuntime runtime : agentRuntimeSelector.all()) {
            if (runtime.ownsPermissionRequest(requestId)) {
                return runtime.resolvePermissionRequest(requestId, action);
            }
        }
        return PermissionResolutionModel.missing(LanguageManager.getLanguageString("chat.permission.missing"));
    }
}
