package com.patres.alina.uidesktop.mascot;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.interaction.AgentInteractionResolutionModel;
import com.patres.alina.common.interaction.AgentInteractionResponse;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.util.NotificationSoundPlayer;
import javafx.application.Platform;
import javafx.stage.Stage;

import java.time.Clock;

import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;

public final class MascotNotifications {

    private MascotNotifications() {
    }

    public static MascotNotificationCoordinator install(final Stage primaryStage,
                                                        final ApplicationWindow applicationWindow) {
        final MascotNotificationCoordinator coordinator = new MascotNotificationCoordinator(
                DefaultEventBus.getInstance(),
                new MascotPopup(),
                new BackendGateway(),
                () -> UI_SETTINGS.getSettings().isMascotNotificationsEnabled(),
                applicationWindow::getThreadTitle,
                threadId -> openThread(primaryStage, applicationWindow, threadId),
                task -> Thread.startVirtualThread(task),
                NotificationSoundPlayer::playIfEnabled,
                Clock.systemDefaultZone(),
                primaryStage.isFocused() && !primaryStage.isIconified()
        );
        primaryStage.focusedProperty().addListener((_, _, _) -> updateFocus(primaryStage, coordinator));
        primaryStage.iconifiedProperty().addListener((_, _, _) -> updateFocus(primaryStage, coordinator));
        return coordinator;
    }

    private static void updateFocus(final Stage primaryStage,
                                    final MascotNotificationCoordinator coordinator) {
        coordinator.setMainWindowActive(primaryStage.isFocused() && !primaryStage.isIconified());
    }

    private static void openThread(final Stage primaryStage,
                                   final ApplicationWindow applicationWindow,
                                   final String threadId) {
        Platform.runLater(() -> {
            applicationWindow.activateThread(threadId);
            primaryStage.setIconified(false);
            primaryStage.show();
            primaryStage.toFront();
            primaryStage.requestFocus();
        });
    }

    private static final class BackendGateway implements MascotInteractionGateway {
        @Override
        public AgentInteractionResolutionModel resolve(final String requestId,
                                                       final AgentInteractionResponse response) {
            return BackendApi.resolveAgentInteraction(requestId, response);
        }

        @Override
        public void retryLastUserMessage(final String threadId) {
            BackendApi.retryLastUserMessage(threadId);
        }
    }
}
