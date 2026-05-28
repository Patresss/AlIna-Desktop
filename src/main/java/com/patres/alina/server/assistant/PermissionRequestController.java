package com.patres.alina.server.assistant;

import com.patres.alina.common.permission.PermissionApprovalAction;
import com.patres.alina.common.permission.PermissionResolutionModel;
import com.patres.alina.server.opencode.OpenCodeRuntimeService;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import org.springframework.stereotype.Component;

@Component
public class PermissionRequestController {

    private final OpenCodeRuntimeService openCodeRuntimeService;

    public PermissionRequestController(final OpenCodeRuntimeService openCodeRuntimeService) {
        this.openCodeRuntimeService = openCodeRuntimeService;
    }

    public PermissionResolutionModel resolve(final String requestId,
                                             final PermissionApprovalAction action) {
        if (!openCodeRuntimeService.ownsPermissionRequest(requestId)) {
            return PermissionResolutionModel.missing(LanguageManager.getLanguageString("chat.permission.missing"));
        }
        return openCodeRuntimeService.resolvePermissionRequest(requestId, action);
    }
}
