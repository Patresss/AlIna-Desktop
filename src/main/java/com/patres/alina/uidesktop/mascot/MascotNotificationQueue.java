package com.patres.alina.uidesktop.mascot;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class MascotNotificationQueue {

    static final int MAX_TERMINAL_NOTIFICATIONS = 5;
    static final Duration TERMINAL_TTL = Duration.ofSeconds(60);

    private final Map<String, MascotNotification> approvalsByRequest = new LinkedHashMap<>();
    private final Map<String, MascotNotification> terminalByThread = new LinkedHashMap<>();

    void addApproval(final MascotNotification notification) {
        if (notification == null
                || !notification.isApproval()
                || notification.requestId().isBlank()
                || notification.threadId().isBlank()) {
            return;
        }
        approvalsByRequest.putIfAbsent(notification.requestId(), notification);
    }

    void addTerminal(final MascotNotification notification) {
        if (notification == null
                || notification.isApproval()
                || notification.threadId().isBlank()) {
            return;
        }
        terminalByThread.remove(notification.threadId());
        terminalByThread.put(notification.threadId(), notification);
        pruneTerminalSize();
    }

    Optional<MascotNotification> next(final Instant now) {
        pruneExpired(now);
        final Optional<MascotNotification> approval = approvalsByRequest.values().stream().findFirst();
        if (approval.isPresent()) {
            return approval;
        }
        return terminalByThread.values().stream()
                .min(Comparator.comparingInt(this::terminalPriority)
                        .thenComparing(MascotNotification::createdAt));
    }

    int remainingApprovalCount() {
        return Math.max(0, approvalsByRequest.size() - 1);
    }

    void removeRequest(final String requestId) {
        if (requestId != null) {
            approvalsByRequest.remove(requestId);
        }
    }

    void remove(final String notificationId) {
        if (notificationId == null || notificationId.isBlank()) {
            return;
        }
        approvalsByRequest.values().removeIf(notification -> notification.id().equals(notificationId));
        terminalByThread.values().removeIf(notification -> notification.id().equals(notificationId));
    }

    void removeThread(final String threadId) {
        if (threadId == null || threadId.isBlank()) {
            return;
        }
        approvalsByRequest.values().removeIf(notification -> notification.threadId().equals(threadId));
        terminalByThread.remove(threadId);
    }

    void clearTerminal() {
        terminalByThread.clear();
    }

    private void pruneExpired(final Instant now) {
        final Instant threshold = now.minus(TERMINAL_TTL);
        terminalByThread.values().removeIf(notification -> !notification.createdAt().isAfter(threshold));
    }

    private void pruneTerminalSize() {
        while (terminalByThread.size() > MAX_TERMINAL_NOTIFICATIONS) {
            final String oldestThread = terminalByThread.entrySet().stream()
                    .min(Map.Entry.comparingByValue(Comparator.comparing(MascotNotification::createdAt)))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldestThread == null) {
                return;
            }
            terminalByThread.remove(oldestThread);
        }
    }

    private int terminalPriority(final MascotNotification notification) {
        return notification.type() == MascotNotificationType.ERROR ? 0 : 1;
    }
}
