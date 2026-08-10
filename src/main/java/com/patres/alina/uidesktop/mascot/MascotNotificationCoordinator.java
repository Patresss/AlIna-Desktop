package com.patres.alina.uidesktop.mascot;

import com.patres.alina.common.event.AgentInteractionResolvedEvent;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.EventBus;
import com.patres.alina.common.interaction.AgentInteractionAction;
import com.patres.alina.common.interaction.AgentInteractionKind;
import com.patres.alina.common.interaction.AgentInteractionResolutionModel;
import com.patres.alina.common.interaction.AgentInteractionResponse;
import com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public final class MascotNotificationCoordinator {

    private static final Logger logger = LoggerFactory.getLogger(MascotNotificationCoordinator.class);

    private final EventBus eventBus;
    private final MascotPopupView view;
    private final MascotInteractionGateway interactionGateway;
    private final BooleanSupplier enabledSupplier;
    private final Function<String, String> threadTitleResolver;
    private final Consumer<String> threadOpener;
    private final Consumer<Runnable> taskExecutor;
    private final Runnable permissionSound;
    private final Clock clock;
    private final MascotNotificationQueue queue = new MascotNotificationQueue();
    private final Set<String> soundedApprovalIds = new HashSet<>();

    private final Consumer<ChatMessageStreamEvent> streamSubscriber = this::handleStreamEvent;
    private final Consumer<AgentInteractionResolvedEvent> resolvedSubscriber = this::handleResolvedEvent;
    private final Consumer<UiSettingsUpdateEvent> settingsSubscriber = event -> refresh();

    private boolean mainWindowActive;
    private boolean closed;
    private String visibleNotificationId = "";
    private int visibleRemainingCount = -1;
    private String processingRequestId = "";

    MascotNotificationCoordinator(final EventBus eventBus,
                                  final MascotPopupView view,
                                  final MascotInteractionGateway interactionGateway,
                                  final BooleanSupplier enabledSupplier,
                                  final Function<String, String> threadTitleResolver,
                                  final Consumer<String> threadOpener,
                                  final Consumer<Runnable> taskExecutor,
                                  final Runnable permissionSound,
                                  final Clock clock,
                                  final boolean mainWindowActive) {
        this.eventBus = Objects.requireNonNull(eventBus);
        this.view = Objects.requireNonNull(view);
        this.interactionGateway = Objects.requireNonNull(interactionGateway);
        this.enabledSupplier = Objects.requireNonNull(enabledSupplier);
        this.threadTitleResolver = Objects.requireNonNull(threadTitleResolver);
        this.threadOpener = Objects.requireNonNull(threadOpener);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.permissionSound = Objects.requireNonNull(permissionSound);
        this.clock = Objects.requireNonNull(clock);
        this.mainWindowActive = mainWindowActive;
        subscribe();
    }

    public synchronized void setMainWindowActive(final boolean active) {
        if (closed) {
            return;
        }
        mainWindowActive = active;
        refresh();
    }

    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        eventBus.unsubscribe(ChatMessageStreamEvent.class, streamSubscriber);
        eventBus.unsubscribe(AgentInteractionResolvedEvent.class, resolvedSubscriber);
        eventBus.unsubscribe(UiSettingsUpdateEvent.class, settingsSubscriber);
        visibleNotificationId = "";
        processingRequestId = "";
        view.close();
    }

    private void subscribe() {
        eventBus.subscribe(ChatMessageStreamEvent.class, streamSubscriber);
        eventBus.subscribe(AgentInteractionResolvedEvent.class, resolvedSubscriber);
        eventBus.subscribe(UiSettingsUpdateEvent.class, settingsSubscriber);
    }

    private synchronized void handleStreamEvent(final ChatMessageStreamEvent event) {
        if (closed) {
            return;
        }
        final String threadId = normalize(event.getThreadId());
        if (threadId.isBlank()) {
            return;
        }
        switch (event.getEventType()) {
            case AGENT_INTERACTION -> handleInteraction(threadId, event);
            case COMPLETE -> addTerminal(MascotNotificationType.COMPLETE, threadId, "");
            case ERROR -> addTerminal(MascotNotificationType.ERROR, threadId, event.getErrorMessage());
            case CANCELLED -> {
                queue.removeThread(threadId);
                refresh();
            }
            default -> {
            }
        }
    }

    private void handleInteraction(final String threadId, final ChatMessageStreamEvent event) {
        final var interaction = event.getAgentInteraction();
        if (interaction == null || interaction.kind() != AgentInteractionKind.APPROVAL) {
            return;
        }
        final MascotNotification notification = MascotNotification.approval(threadId, interaction, clock.instant());
        queue.addApproval(notification);
        refresh();
    }

    private void addTerminal(final MascotNotificationType type,
                             final String threadId,
                             final String detail) {
        if (mainWindowActive || !isEnabled()) {
            return;
        }
        queue.addTerminal(MascotNotification.terminal(
                type,
                threadId,
                normalize(threadTitleResolver.apply(threadId)),
                detail,
                clock.instant()
        ));
        refresh();
    }

    private synchronized void handleResolvedEvent(final AgentInteractionResolvedEvent event) {
        if (closed) {
            return;
        }
        final String requestId = normalize(event.getRequestId());
        queue.removeRequest(requestId);
        soundedApprovalIds.remove(requestId);
        if (requestId.equals(processingRequestId)) {
            processingRequestId = "";
        }
        refresh();
    }

    private synchronized void refresh() {
        if (closed) {
            return;
        }
        if (!isEnabled()) {
            queue.clearTerminal();
            clearVisibleAndHide();
            return;
        }
        final var next = queue.next(clock.instant());
        if (mainWindowActive) {
            queue.clearTerminal();
            clearVisibleAndHide();
            return;
        }
        if (next.isEmpty()) {
            clearVisibleAndHide();
            return;
        }
        final MascotNotification notification = next.get();
        if (notification.isApproval()) {
            showApproval(notification);
        } else {
            showTerminal(notification);
        }
    }

    private void showApproval(final MascotNotification notification) {
        final int remainingCount = queue.remainingApprovalCount();
        if (notification.id().equals(visibleNotificationId)
                && remainingCount == visibleRemainingCount) {
            return;
        }
        visibleNotificationId = notification.id();
        visibleRemainingCount = remainingCount;
        view.showApproval(
                notification,
                remainingCount,
                () -> resolve(notification, AgentInteractionAction.APPROVE_ONCE),
                scopedAction(notification),
                () -> resolve(notification, AgentInteractionAction.DENY),
                () -> openThread(notification)
        );
        if (soundedApprovalIds.add(notification.requestId())) {
            permissionSound.run();
        }
    }

    private Optional<Runnable> scopedAction(final MascotNotification notification) {
        if (!notification.supportsScopedApproval()) {
            return Optional.empty();
        }
        return Optional.of(() -> resolve(notification, AgentInteractionAction.APPROVE_SCOPED));
    }

    private void showTerminal(final MascotNotification notification) {
        if (notification.id().equals(visibleNotificationId)) {
            return;
        }
        visibleNotificationId = notification.id();
        visibleRemainingCount = -1;
        view.showTerminal(
                notification,
                () -> openTerminal(notification),
                () -> expireTerminal(notification.id())
        );
    }

    private synchronized void resolve(final MascotNotification notification,
                                      final AgentInteractionAction action) {
        if (closed || !processingRequestId.isBlank()) {
            return;
        }
        processingRequestId = notification.requestId();
        view.showProcessing();
        taskExecutor.accept(() -> resolveInBackground(notification, action));
    }

    private void resolveInBackground(final MascotNotification notification,
                                     final AgentInteractionAction action) {
        synchronized (this) {
            if (closed) {
                return;
            }
        }
        try {
            final AgentInteractionResolutionModel resolution = interactionGateway.resolve(
                    notification.requestId(),
                    new AgentInteractionResponse(action, "{}")
            );
            applyResolution(notification, resolution);
        } catch (Exception e) {
            logger.warn("Cannot resolve mascot approval {}", notification.requestId(), e);
            resolutionFailed(notification.requestId(), e.getMessage());
        }
    }

    private synchronized void applyResolution(final MascotNotification notification,
                                              final AgentInteractionResolutionModel resolution) {
        if (closed) {
            return;
        }
        if (resolution == null) {
            resolutionFailed(notification.requestId(), "");
            return;
        }
        switch (resolution.status()) {
            case RESOLVED -> {
                queue.removeRequest(notification.requestId());
                soundedApprovalIds.remove(notification.requestId());
                processingRequestId = "";
                eventBus.publish(new AgentInteractionResolvedEvent(
                        notification.threadId(),
                        notification.requestId()
                ));
                if (!resolution.autoContinues()) {
                    interactionGateway.retryLastUserMessage(notification.threadId());
                }
                refresh();
            }
            case MISSING -> {
                queue.removeRequest(notification.requestId());
                soundedApprovalIds.remove(notification.requestId());
                processingRequestId = "";
                refresh();
            }
            case ERROR -> resolutionFailed(notification.requestId(), resolution.message());
        }
    }

    private synchronized void resolutionFailed(final String requestId, final String message) {
        if (closed || !normalize(requestId).equals(processingRequestId)) {
            return;
        }
        processingRequestId = "";
        view.showResolutionError(normalize(message));
    }

    private synchronized void openThread(final MascotNotification notification) {
        if (closed) {
            return;
        }
        view.hide();
        visibleNotificationId = "";
        threadOpener.accept(notification.threadId());
    }

    private synchronized void openTerminal(final MascotNotification notification) {
        queue.remove(notification.id());
        openThread(notification);
        refresh();
    }

    private synchronized void expireTerminal(final String notificationId) {
        if (closed) {
            return;
        }
        queue.remove(notificationId);
        if (notificationId.equals(visibleNotificationId)) {
            visibleNotificationId = "";
        }
        refresh();
    }

    private void clearVisibleAndHide() {
        visibleNotificationId = "";
        visibleRemainingCount = -1;
        view.hide();
    }

    private boolean isEnabled() {
        try {
            return enabledSupplier.getAsBoolean();
        } catch (Exception e) {
            logger.debug("Cannot read mascot notification setting", e);
            return false;
        }
    }

    private static String normalize(final String value) {
        return value == null ? "" : value.trim();
    }
}
