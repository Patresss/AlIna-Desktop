package com.patres.alina.server.opencode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.common.storage.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
public class OpenCodeSessionRegistry {

    private static final Logger logger = LoggerFactory.getLogger(OpenCodeSessionRegistry.class);

    private final ObjectMapper objectMapper;
    private final Path registryPath = AppPaths.resolve("conversations/opencode-sessions.json");
    private Map<String, String> sessions;

    public OpenCodeSessionRegistry(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized String get(final String threadId) {
        return load().get(threadId);
    }

    public synchronized void put(final String threadId, final String sessionId) {
        final Map<String, String> mapping = load();
        mapping.put(threadId, sessionId);
        save(mapping);
    }

    public synchronized void remove(final String threadId) {
        final Map<String, String> mapping = load();
        if (mapping.remove(threadId) != null) {
            save(mapping);
        }
    }

    public synchronized void clear() {
        final Map<String, String> mapping = load();
        if (mapping.isEmpty()) {
            return;
        }
        mapping.clear();
        save(mapping);
    }

    private Map<String, String> load() {
        if (sessions != null) {
            return sessions;
        }
        try {
            final Path parent = registryPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(registryPath)) {
                sessions = new HashMap<>();
                return sessions;
            }
            sessions = new HashMap<>(objectMapper.readValue(
                    registryPath.toFile(),
                    new TypeReference<Map<String, String>>() {
                    }
            ));
            return sessions;
        } catch (IOException e) {
            logger.warn("Cannot load OpenCode session registry from {}", registryPath, e);
            sessions = new HashMap<>();
            return sessions;
        }
    }

    private void save(final Map<String, String> mapping) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(registryPath.toFile(), mapping);
            sessions = new HashMap<>(mapping);
        } catch (IOException e) {
            logger.warn("Cannot save OpenCode session registry to {}", registryPath, e);
        }
    }
}
