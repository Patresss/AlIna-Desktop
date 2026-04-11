package com.patres.alina.uidesktop.ui.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.patres.alina.common.storage.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Manages prompt history for the chat input, persisted as a JSON file.
 * Keeps the last {@value #MAX_HISTORY_SIZE} prompts and supports
 * UP/DOWN navigation similar to shell history.
 */
public class PromptHistoryManager {

    private static final Logger logger = LoggerFactory.getLogger(PromptHistoryManager.class);
    private static final int MAX_HISTORY_SIZE = 25;
    private static final String HISTORY_FILE = "prompt-history.json";
    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static final PromptHistoryManager INSTANCE = new PromptHistoryManager();

    private final LinkedList<String> history = new LinkedList<>();
    private final Path historyFilePath;

    private int navigationIndex = -1;
    private String savedCurrentText = "";
    private boolean navigating = false;

    private PromptHistoryManager() {
        this.historyFilePath = AppPaths.resolve(HISTORY_FILE);
        loadFromFile();
    }

    public static PromptHistoryManager getInstance() {
        return INSTANCE;
    }

    /**
     * Adds a prompt to the history. Duplicates of the same text are removed
     * (the newest occurrence is kept at the end). Persists to file immediately.
     */
    public synchronized void addPrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return;
        }
        history.remove(prompt);
        history.addLast(prompt);
        while (history.size() > MAX_HISTORY_SIZE) {
            history.removeFirst();
        }
        resetNavigation();
        saveToFile();
    }

    /**
     * Navigates up (to older prompts).
     *
     * @param currentText the current text in the text area (saved on first navigation)
     * @return the prompt to display, or {@code null} if history is empty
     */
    public synchronized String navigateUp(String currentText) {
        if (history.isEmpty()) {
            return null;
        }
        if (!navigating) {
            savedCurrentText = currentText;
            navigating = true;
            navigationIndex = history.size() - 1;
        } else if (navigationIndex > 0) {
            navigationIndex--;
        } else {
            return history.getFirst();
        }
        return history.get(navigationIndex);
    }

    /**
     * Navigates down (to newer prompts).
     *
     * @return the prompt to display, or the original saved text when past the newest entry
     */
    public synchronized String navigateDown() {
        if (!navigating) {
            return null;
        }
        if (navigationIndex < history.size() - 1) {
            navigationIndex++;
            return history.get(navigationIndex);
        } else {
            String original = savedCurrentText;
            resetNavigation();
            return original;
        }
    }

    public synchronized boolean isNavigating() {
        return navigating;
    }

    public synchronized void resetNavigation() {
        navigating = false;
        navigationIndex = -1;
        savedCurrentText = "";
    }

    private void loadFromFile() {
        try {
            if (Files.exists(historyFilePath)) {
                List<String> loaded = MAPPER.readValue(historyFilePath.toFile(), new TypeReference<List<String>>() {});
                history.clear();
                int start = Math.max(0, loaded.size() - MAX_HISTORY_SIZE);
                for (int i = start; i < loaded.size(); i++) {
                    history.addLast(loaded.get(i));
                }
                logger.info("Loaded {} prompt history entries", history.size());
            }
        } catch (IOException e) {
            logger.warn("Failed to load prompt history from {}: {}", historyFilePath, e.getMessage());
        }
    }

    private void saveToFile() {
        try {
            Files.createDirectories(historyFilePath.getParent());
            MAPPER.writeValue(historyFilePath.toFile(), new ArrayList<>(history));
        } catch (IOException e) {
            logger.warn("Failed to save prompt history to {}: {}", historyFilePath, e.getMessage());
        }
    }
}
