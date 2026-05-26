package com.patres.alina.server.assistant;

import com.patres.alina.common.permission.PermissionApprovalAction;
import com.patres.alina.common.permission.PermissionResolutionModel;
import com.patres.alina.server.ai.AiRuntimeRegistry;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import org.springframework.stereotype.Component;

@Component
public class PermissionRequestController {

    private final AiRuntimeRegistry aiRuntimeRegistry;

    public PermissionRequestController(final AiRuntimeRegistry aiRuntimeRegistry) {
        this.aiRuntimeRegistry = aiRuntimeRegistry;
    }

    public PermissionResolutionModel resolve(final String requestId,
                                             final PermissionApprovalAction action) {
        return aiRuntimeRegistry.findPermissionOwner(requestId)
                .map(runtime -> runtime.resolvePermissionRequest(requestId, action))
                .orElseGet(() -> PermissionResolutionModel.missing(LanguageManager.getLanguageString("chat.permission.missing")));
    }
}
