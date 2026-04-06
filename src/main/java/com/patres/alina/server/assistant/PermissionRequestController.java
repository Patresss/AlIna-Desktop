package com.patres.alina.server.assistant;

import com.patres.alina.common.permission.PermissionApprovalAction;
import com.patres.alina.common.permission.PermissionResolutionModel;
import com.patres.alina.server.opencode.OpenCodeRuntimeService;
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
            return PermissionResolutionModel.missing("To zapytanie o zgodę nie jest już aktywne.");
        }
        return openCodeRuntimeService.resolvePermissionRequest(requestId, action);
    }
}
