package com.patres.alina.uidesktop.mascot;

import com.patres.alina.common.agent.AgentBackend;
import com.patres.alina.common.interaction.AgentInteractionApprovalScope;
import com.patres.alina.common.interaction.AgentInteractionKind;
import com.patres.alina.common.interaction.AgentInteractionRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MascotNotificationQueueTest {

    private static final Instant NOW = Instant.parse("2026-07-17T18:00:00Z");

    @Test
    void keepsApprovalsInFifoOrderAndDeduplicatesRequests() {
        final MascotNotificationQueue queue = new MascotNotificationQueue();
        queue.addApproval(approval("thread-1", "request-1", NOW));
        queue.addApproval(approval("thread-1", "request-1", NOW.plusSeconds(1)));
        queue.addApproval(approval("thread-2", "request-2", NOW.plusSeconds(2)));

        assertThat(queue.next(NOW.plusSeconds(3)))
                .get()
                .extracting(MascotNotification::requestId)
                .isEqualTo("request-1");
        assertThat(queue.remainingApprovalCount()).isEqualTo(1);

        queue.removeRequest("request-1");

        assertThat(queue.next(NOW.plusSeconds(3)))
                .get()
                .extracting(MascotNotification::requestId)
                .isEqualTo("request-2");
    }

    @Test
    void prioritizesApprovalsThenErrorsThenCompletions() {
        final MascotNotificationQueue queue = new MascotNotificationQueue();
        final MascotNotification complete = terminal(MascotNotificationType.COMPLETE, "complete", NOW);
        final MascotNotification error = terminal(MascotNotificationType.ERROR, "error", NOW.plusSeconds(1));
        queue.addTerminal(complete);
        queue.addTerminal(error);

        assertThat(queue.next(NOW.plusSeconds(2))).contains(error);

        queue.addApproval(approval("approval", "request", NOW.plusSeconds(2)));

        assertThat(queue.next(NOW.plusSeconds(3)))
                .get()
                .extracting(MascotNotification::type)
                .isEqualTo(MascotNotificationType.APPROVAL);
    }

    @Test
    void replacesTerminalNotificationForTheSameThreadAndExpiresOldItems() {
        final MascotNotificationQueue queue = new MascotNotificationQueue();
        final MascotNotification first = terminal(MascotNotificationType.COMPLETE, "thread", NOW);
        final MascotNotification replacement = terminal(MascotNotificationType.ERROR, "thread", NOW.plusSeconds(5));
        queue.addTerminal(first);
        queue.addTerminal(replacement);

        assertThat(queue.next(NOW.plusSeconds(6))).contains(replacement);
        assertThat(queue.next(NOW.plusSeconds(66))).isEmpty();
    }

    @Test
    void retainsOnlyFiveNewestTerminalNotifications() {
        final MascotNotificationQueue queue = new MascotNotificationQueue();
        for (int index = 0; index < 7; index++) {
            queue.addTerminal(terminal(
                    MascotNotificationType.COMPLETE,
                    "thread-" + index,
                    NOW.plusSeconds(index)
            ));
        }

        assertThat(queue.next(NOW.plusSeconds(10)))
                .get()
                .extracting(MascotNotification::threadId)
                .isEqualTo("thread-2");
    }

    @Test
    void removesEveryItemForCancelledThread() {
        final MascotNotificationQueue queue = new MascotNotificationQueue();
        queue.addApproval(approval("thread", "request", NOW));
        queue.addTerminal(terminal(MascotNotificationType.COMPLETE, "thread", NOW));

        queue.removeThread("thread");

        assertThat(queue.next(NOW)).isEmpty();
        assertThat(queue.next(NOW)).isEmpty();
    }

    @Test
    void preservesApprovalScopeFromInteraction() {
        final MascotNotification notification = MascotNotification.approval(
                "thread",
                new AgentInteractionRequest(
                        "request",
                        AgentBackend.OPENCODE,
                        AgentInteractionKind.APPROVAL,
                        "Run command",
                        "Allow command?",
                        AgentInteractionApprovalScope.PERSISTENT,
                        "{}"
                ),
                NOW
        );

        assertThat(notification.approvalScope()).isEqualTo(AgentInteractionApprovalScope.PERSISTENT);
        assertThat(notification.supportsScopedApproval()).isTrue();
    }

    private MascotNotification approval(final String threadId,
                                         final String requestId,
                                         final Instant createdAt) {
        return MascotNotification.approval(
                threadId,
                new AgentInteractionRequest(
                        requestId,
                        AgentBackend.CODEX,
                        AgentInteractionKind.APPROVAL,
                        "Run command",
                        "Allow command?",
                        AgentInteractionApprovalScope.NONE,
                        "{}"
                ),
                createdAt
        );
    }

    private MascotNotification terminal(final MascotNotificationType type,
                                         final String threadId,
                                         final Instant createdAt) {
        return MascotNotification.terminal(type, threadId, type.name(), "Details", createdAt);
    }
}
