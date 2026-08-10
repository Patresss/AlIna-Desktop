package com.patres.alina.uidesktop.mascot;

record MascotPopupLayout(
        int width,
        int height,
        int mascotIconSize,
        int mascotWidth,
        int bubbleTopPadding,
        int bubbleLeftPadding,
        int bubbleBottomPadding,
        int bubbleRightPadding,
        int detailHeight
) {

    private static final MascotPopupLayout NOTIFICATION = new MascotPopupLayout(
            468, 184, 116, 106,
            13, 21, 15, 14,
            36
    );

    static MascotPopupLayout forType(final MascotNotificationType type) {
        return NOTIFICATION;
    }

    int mascotHeight() {
        return height - 12;
    }
}
