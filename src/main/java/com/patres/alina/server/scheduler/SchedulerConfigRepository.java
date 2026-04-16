package com.patres.alina.server.scheduler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.patres.alina.common.storage.AppPaths;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Reads and writes scheduler tasks to ~/.config/AlIna/scheduler/tasks.json
 */
@Component
public class SchedulerConfigRepository {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerConfigRepository.class);
    private static final String CONFIG_PATH = "scheduler/tasks.json";

    private final ObjectMapper mapper;
    private final Path configPath;

    public SchedulerConfigRepository() {
        this.configPath = AppPaths.resolve(CONFIG_PATH);
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public synchronized SchedulerConfig load() {
        try {
            File file = configPath.toFile();
            if (file.exists()) {
                String content = FileUtils.readFileToString(file, Charset.defaultCharset());
                SchedulerConfig config = mapper.readValue(content, SchedulerConfig.class);
                logger.info("Scheduler config loaded with {} tasks", config.tasks().size());
                return config;
            }
            logger.info("Scheduler config not found at {} - returning empty", file.getAbsolutePath());
            return SchedulerConfig.empty();
        } catch (Exception e) {
            logger.error("Failed to load scheduler config", e);
            return SchedulerConfig.empty();
        }
    }

    public synchronized void save(SchedulerConfig config) {
        try {
            File file = configPath.toFile();
            FileUtils.createParentDirectories(file);
            String content = mapper.writeValueAsString(config);
            FileUtils.write(file, content, Charset.defaultCharset());
            logger.info("Scheduler config saved with {} tasks to {}", config.tasks().size(), file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to save scheduler config", e);
            throw new RuntimeException("Cannot save scheduler config", e);
        }
    }

    public List<ScheduledTask> getAllTasks() {
        return load().tasks();
    }

    public Optional<ScheduledTask> getTask(String taskId) {
        return getAllTasks().stream()
                .filter(t -> t.id().equals(taskId))
                .findFirst();
    }

    public void addTask(ScheduledTask task) {
        SchedulerConfig config = load();
        List<ScheduledTask> tasks = new ArrayList<>(config.tasks());
        tasks.add(task);
        save(new SchedulerConfig(tasks));
    }

    public void updateTask(ScheduledTask updatedTask) {
        SchedulerConfig config = load();
        List<ScheduledTask> tasks = config.tasks().stream()
                .map(t -> t.id().equals(updatedTask.id()) ? updatedTask : t)
                .toList();
        save(new SchedulerConfig(tasks));
    }

    public void deleteTask(String taskId) {
        SchedulerConfig config = load();
        List<ScheduledTask> tasks = config.tasks().stream()
                .filter(t -> !t.id().equals(taskId))
                .toList();
        save(new SchedulerConfig(tasks));
    }
}
