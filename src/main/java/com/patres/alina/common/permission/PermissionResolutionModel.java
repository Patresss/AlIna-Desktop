package com.patres.alina.common.permission;

public record PermissionResolutionModel(
        boolean found,
        boolean approved,
        boolean persisted,
        boolean autoContinues,
        String message
) {
    public static PermissionResolutionModel missing(final String message) {
        return new PermissionResolutionModel(false, false, false, false, message);
    }

    public static PermissionResolutionModel denied(final String message) {
        return new PermissionResolutionModel(true, false, false, false, message);
    }

    public static PermissionResolutionModel approved(final boolean persisted,
                                                     final boolean autoContinues,
                                                     final String message) {
        return new PermissionResolutionModel(true, true, persisted, autoContinues, message);
    }
}
