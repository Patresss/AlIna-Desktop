package com.patres.alina.server.scheduler;

import com.patres.alina.common.event.bus.DefaultEventBus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Core scheduler service using ScheduledExecutorService.
 * Uses Spring's CronExpression for reliable cron parsing (6-field format:
 * second minute hour dayOfMonth month dayOfWeek).
 * Also accepts 5-field Unix cron — auto-normalized by prepending "0" as seconds.
 */
@Service
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final SchedulerConfigRepository repository;
    private final ScheduledExecutorService executor;
    private final Map<String, ScheduledFuture<?>> scheduledFutures = new ConcurrentHashMap<>();

    public SchedulerService(SchedulerConfigRepository repository) {
        this.repository = repository;
        this.executor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "alina-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void init() {
        logger.info("Starting scheduler service...");
        scheduleAllEnabled();
    }

    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down scheduler service...");
        executor.shutdownNow();
    }

    /**
     * Reloads all tasks from config and re-schedules enabled ones.
     */
    public void reloadAll() {
        cancelAll();
        scheduleAllEnabled();
    }

    /**
     * Returns all tasks from the config.
     */
    public List<ScheduledTask> getAllTasks() {
        return repository.getAllTasks();
    }

    /**
     * Adds a new task, persists it, and schedules it if enabled.
     */
    public void addTask(ScheduledTask task) {
        repository.addTask(task);
        if (task.enabled()) {
            scheduleTask(task);
        }
    }

    /**
     * Updates a task, persists it, and re-schedules.
     */
    public void updateTask(ScheduledTask task) {
        cancelTask(task.id());
        repository.updateTask(task);
        if (task.enabled()) {
            scheduleTask(task);
        }
    }

    /**
     * Deletes a task, removes from config and cancels scheduling.
     */
    public void deleteTask(String taskId) {
        cancelTask(taskId);
        repository.deleteTask(taskId);
    }

    /**
     * Immediately executes a task (ignoring cron schedule).
     */
    public void runNow(String taskId) {
        repository.getTask(taskId).ifPresent(this::fireTask);
    }

    // ═══════════════════════════════════════════
    // Internal scheduling logic
    // ═══════════════════════════════════════════

    private void scheduleAllEnabled() {
        List<ScheduledTask> tasks = repository.getAllTasks();
        long enabledCount = 0;
        for (ScheduledTask task : tasks) {
            if (task.enabled()) {
                scheduleTask(task);
                enabledCount++;
            }
        }
        logger.info("Scheduled {} enabled tasks out of {} total", enabledCount, tasks.size());
    }

    private void scheduleTask(ScheduledTask task) {
        try {
            String springCron = normalizeCron(task.cron());
            CronExpression cronExpression = CronExpression.parse(springCron);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextRun = cronExpression.next(now);

            if (nextRun == null) {
                logger.warn("Could not compute next run for task '{}' with cron '{}'", task.name(), task.cron());
                return;
            }

            // +1ms buffer to guarantee we fire after the cron second boundary, not before
            long delayMillis = Duration.between(now, nextRun).toMillis() + 1;

            repository.updateTask(task.withNextRun(nextRun));

            ScheduledFuture<?> future = executor.schedule(() -> {
                fireTask(task);
                scheduleTask(task);
            }, delayMillis, TimeUnit.MILLISECONDS);

            scheduledFutures.put(task.id(), future);
            logger.info("Task '{}' scheduled to run at {} (in {} ms)", task.name(), nextRun, delayMillis);
        } catch (Exception e) {
            logger.error("Failed to schedule task '{}'", task.name(), e);
        }
    }

    private void fireTask(ScheduledTask task) {
        logger.info("Firing scheduled task: '{}' with prompt: '{}'", task.name(), truncate(task.prompt(), 80));
        try {
            repository.updateTask(task.withLastRun(LocalDateTime.now()));
            DefaultEventBus.getInstance().publish(new SchedulerTaskTriggeredEvent(task));
        } catch (Exception e) {
            logger.error("Error executing scheduled task '{}'", task.name(), e);
        }
    }

    private void cancelTask(String taskId) {
        ScheduledFuture<?> future = scheduledFutures.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void cancelAll() {
        scheduledFutures.values().forEach(f -> f.cancel(false));
        scheduledFutures.clear();
    }

    // ═══════════════════════════════════════════
    // Cron normalization
    // ═══════════════════════════════════════════

    /**
     * Normalizes a cron expression: if 5-field Unix format is given,
     * prepends "0" as the seconds field to convert to Spring 6-field format.
     */
    private String normalizeCron(String cron) {
        String trimmed = cron.trim();
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 5) {
            logger.debug("Auto-converting 5-field Unix cron '{}' to 6-field Spring format", cron);
            return "0 " + trimmed;
        }
        return trimmed;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
