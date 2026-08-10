package com.patres.alina.uidesktop.mascot;

import com.patres.alina.common.agent.AgentBackend;
import com.patres.alina.common.event.AgentInteractionResolvedEvent;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.interaction.AgentInteractionAction;
import com.patres.alina.common.interaction.AgentInteractionApprovalScope;
import com.patres.alina.common.interaction.AgentInteractionKind;
import com.patres.alina.common.interaction.AgentInteractionRequest;
import com.patres.alina.common.interaction.AgentInteractionResolutionModel;
import com.patres.alina.common.interaction.AgentInteractionResponse;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class MascotNotificationCoordinatorTest {

    private static final Instant NOW = Instant.parse("2026-07-17T18:00:00Z");

    @Test
    void revealsPendingApprovalOnlyAfterMainWindowLosesFocus() {
        final Fixture fixture = new Fixture(true);

        fixture.eventBus.publish(interactionEvent("thread-1", "request-1"));

        assertThat(fixture.view.visible).isFalse();

        fixture.coordinator.setMainWindowActive(false);

        assertThat(fixture.view.visible).isTrue();
        assertThat(fixture.view.notification.requestId()).isEqualTo("request-1");
        assertThat(fixture.soundCount).hasSize(1);

        fixture.coordinator.setMainWindowActive(true);

        assertThat(fixture.view.visible).isFalse();
        fixture.coordinator.setMainWindowActive(false);
        assertThat(fixture.view.visible).isTrue();
        assertThat(fixture.soundCount).hasSize(1);
    }

    @Test
    void resolvesApprovalOnceAndSynchronizesChatCard() {
        final Fixture fixture = new Fixture(false);
        final List<AgentInteractionResolvedEvent> resolvedEvents = new ArrayList<>();
        fixture.eventBus.subscribe(AgentInteractionResolvedEvent.class, resolvedEvents::add);
        fixture.gateway.resolution = AgentInteractionResolutionModel.resolved(
                true,
                AgentInteractionApprovalScope.NONE,
                false,
                "Approved"
        );
        fixture.eventBus.publish(interactionEvent("thread-1", "request-1"));

        fixture.view.approveAction.run();

        assertThat(fixture.gateway.lastResponse.action()).isEqualTo(AgentInteractionAction.APPROVE_ONCE);
        assertThat(fixture.gateway.retriedThreads).containsExactly("thread-1");
        assertThat(resolvedEvents)
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getThreadId()).isEqualTo("thread-1");
                    assertThat(event.getRequestId()).isEqualTo("request-1");
                });
        assertThat(fixture.view.visible).isFalse();
    }

    @Test
    void exposesScopedApprovalOnlyWhenBackendProvidesItsScope() {
        for (AgentInteractionApprovalScope scope : List.of(
                AgentInteractionApprovalScope.SESSION,
                AgentInteractionApprovalScope.PERSISTENT
        )) {
            final Fixture fixture = new Fixture(false);
            fixture.eventBus.publish(interactionEvent("thread-1", "request-" + scope, scope));

            assertThat(fixture.view.notification.approvalScope()).isEqualTo(scope);
            assertThat(fixture.view.approveScopedAction).isNotNull();

            fixture.view.approveScopedAction.run();

            assertThat(fixture.gateway.lastResponse.action()).isEqualTo(AgentInteractionAction.APPROVE_SCOPED);
        }

        final Fixture fixture = new Fixture(false);
        fixture.eventBus.publish(interactionEvent(
                "thread-1",
                "request-none",
                AgentInteractionApprovalScope.NONE
        ));

        assertThat(fixture.view.approveScopedAction).isNull();
    }

    @Test
    void keepsApprovalVisibleAndReenablesActionsAfterResolutionError() {
        final Fixture fixture = new Fixture(false);
        fixture.gateway.resolution = AgentInteractionResolutionModel.error("Backend unavailable");
        fixture.eventBus.publish(interactionEvent("thread-1", "request-1"));

        fixture.view.denyAction.run();

        assertThat(fixture.gateway.lastResponse.action()).isEqualTo(AgentInteractionAction.DENY);
        assertThat(fixture.view.errorMessage).isEqualTo("Backend unavailable");
        assertThat(fixture.view.visible).isTrue();
    }

    @Test
    void showsTerminalNotificationOnlyWhenApplicationIsInactive() {
        final Fixture fixture = new Fixture(true);

        fixture.eventBus.publish(ChatMessageStreamEvent.complete("focused", "model", "agent", 1, 0));

        assertThat(fixture.view.visible).isFalse();

        fixture.coordinator.setMainWindowActive(false);
        fixture.eventBus.publish(ChatMessageStreamEvent.complete("background", "model", "agent", 1, 0));

        assertThat(fixture.view.notification.type()).isEqualTo(MascotNotificationType.COMPLETE);
        assertThat(fixture.view.notification.title()).isEqualTo("Title background");

        fixture.view.expiredAction.run();

        assertThat(fixture.view.visible).isFalse();
    }

    @Test
    void settingHidesMascotWithoutResolvingApproval() {
        final Fixture fixture = new Fixture(false);
        fixture.eventBus.publish(interactionEvent("thread-1", "request-1"));
        fixture.enabled.set(false);

        fixture.eventBus.publish(new com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent());

        assertThat(fixture.view.visible).isFalse();
        fixture.enabled.set(true);
        fixture.eventBus.publish(new com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent());
        assertThat(fixture.view.notification.requestId()).isEqualTo("request-1");
    }

    @Test
    void returningToApplicationDiscardsAllQueuedTerminalNotifications() {
        final Fixture fixture = new Fixture(false);
        fixture.eventBus.publish(ChatMessageStreamEvent.complete("first", "model", "agent", 1, 0));
        fixture.eventBus.publish(new ChatMessageStreamEvent(
                "second",
                ChatMessageStreamEvent.StreamEventType.ERROR,
                "Failed"
        ));

        assertThat(fixture.view.visible).isTrue();

        fixture.coordinator.setMainWindowActive(true);
        fixture.coordinator.setMainWindowActive(false);

        assertThat(fixture.view.visible).isFalse();
    }

    @Test
    void closingCoordinatorCancelsResolutionWorkThatHasNotStarted() {
        final AtomicReference<Runnable> pendingTask = new AtomicReference<>();
        final Fixture fixture = new Fixture(false, pendingTask::set);
        fixture.eventBus.publish(interactionEvent("thread-1", "request-1"));
        fixture.view.approveAction.run();

        fixture.coordinator.close();
        pendingTask.get().run();

        assertThat(fixture.gateway.lastResponse).isNull();
        assertThat(fixture.view.visible).isFalse();
    }

    private ChatMessageStreamEvent interactionEvent(final String threadId, final String requestId) {
        return interactionEvent(threadId, requestId, AgentInteractionApprovalScope.NONE);
    }

    private ChatMessageStreamEvent interactionEvent(final String threadId,
                                                     final String requestId,
                                                     final AgentInteractionApprovalScope scope) {
        return ChatMessageStreamEvent.interaction(
                threadId,
                new AgentInteractionRequest(
                        requestId,
                        AgentBackend.CODEX,
                        AgentInteractionKind.APPROVAL,
                        "Run command",
                        "Allow ./gradlew test?",
                        scope,
                        "{}"
                )
        );
    }

    private static final class Fixture {
        private final DefaultEventBus eventBus = new DefaultEventBus();
        private final FakeView view = new FakeView();
        private final FakeGateway gateway = new FakeGateway();
        private final AtomicBoolean enabled = new AtomicBoolean(true);
        private final List<Boolean> soundCount = new ArrayList<>();
        private final MascotNotificationCoordinator coordinator;

        private Fixture(final boolean active) {
            this(active, Runnable::run);
        }

        private Fixture(final boolean active, final Consumer<Runnable> taskExecutor) {
            coordinator = new MascotNotificationCoordinator(
                    eventBus,
                    view,
                    gateway,
                    enabled::get,
                    threadId -> "Title " + threadId,
                    threadId -> {
                    },
                    taskExecutor,
                    () -> soundCount.add(true),
                    Clock.fixed(NOW, ZoneOffset.UTC),
                    active
            );
        }
    }

    private static final class FakeGateway implements MascotInteractionGateway {
        private AgentInteractionResolutionModel resolution = AgentInteractionResolutionModel.resolved(
                true,
                AgentInteractionApprovalScope.NONE,
                true,
                "Approved"
        );
        private AgentInteractionResponse lastResponse;
        private final List<String> retriedThreads = new ArrayList<>();

        @Override
        public AgentInteractionResolutionModel resolve(final String requestId,
                                                       final AgentInteractionResponse response) {
            lastResponse = response;
            return resolution;
        }

        @Override
        public void retryLastUserMessage(final String threadId) {
            retriedThreads.add(threadId);
        }
    }

    private static final class FakeView implements MascotPopupView {
        private MascotNotification notification;
        private boolean visible;
        private Runnable approveAction;
        private Runnable approveScopedAction;
        private Runnable denyAction;
        private Runnable expiredAction;
        private String errorMessage = "";

        @Override
        public void showApproval(final MascotNotification notification,
                                 final int remainingCount,
                                 final Runnable approveAction,
                                 final Optional<Runnable> approveScopedAction,
                                 final Runnable denyAction,
                                 final Runnable openThreadAction) {
            this.notification = notification;
            this.approveAction = approveAction;
            this.approveScopedAction = approveScopedAction.orElse(null);
            this.denyAction = denyAction;
            visible = true;
            errorMessage = "";
        }

        @Override
        public void showTerminal(final MascotNotification notification,
                                 final Runnable openThreadAction,
                                 final Runnable expiredAction) {
            this.notification = notification;
            this.expiredAction = expiredAction;
            visible = true;
        }

        @Override
        public void showProcessing() {
        }

        @Override
        public void showResolutionError(final String message) {
            errorMessage = message;
        }

        @Override
        public void hide() {
            visible = false;
        }

        @Override
        public void close() {
            visible = false;
        }
    }
}
