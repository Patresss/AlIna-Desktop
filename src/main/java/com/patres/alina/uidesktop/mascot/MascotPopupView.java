package com.patres.alina.uidesktop.mascot;

import java.util.Optional;

interface MascotPopupView {

    void showApproval(MascotNotification notification,
                      int remainingCount,
                      Runnable approveAction,
                      Optional<Runnable> approveScopedAction,
                      Runnable denyAction,
                      Runnable openThreadAction);

    void showTerminal(MascotNotification notification,
                      Runnable openThreadAction,
                      Runnable expiredAction);

    void showProcessing();

    void showResolutionError(String message);

    void hide();

    void close();
}
