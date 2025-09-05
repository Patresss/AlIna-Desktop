package com.patres.alina.common.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class AppPaths {

    private static final Logger logger = LoggerFactory.getLogger(AppPaths.class);
    private static final String APP_DIR_NAME = "AlIna";

    private AppPaths() {
    }

    public static Path baseDataDir() {
        // 1) Explicit override via system property
        String override = System.getProperty("storage.local.base.path");
        if (override != null && !override.isBlank()) {
            return ensureDir(Paths.get(override).toAbsolutePath());
        }

        // 2) Development mode: prefer project-local ./data for IDE runs
        if (Files.isDirectory(Paths.get("data"))) {
            return ensureDir(Paths.get("data").toAbsolutePath());
        }

        // 3) Platform-specific default location (installed app)
        String os = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home", ".");
        Path base;
        if (os.contains("mac")) {
            base = Paths.get(userHome, "Library", "Application Support", APP_DIR_NAME);
        } else if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if (appData == null || appData.isBlank()) {
                appData = Paths.get(userHome, "AppData", "Roaming").toString();
            }
            base = Paths.get(appData, APP_DIR_NAME);
        } else {
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            if (xdgDataHome == null || xdgDataHome.isBlank()) {
                xdgDataHome = Paths.get(userHome, ".local", "share").toString();
            }
            base = Paths.get(xdgDataHome, APP_DIR_NAME);
        }

        return ensureDir(base.toAbsolutePath());
    }

    public static Path resolve(String relative) {
        return baseDataDir().resolve(relative).toAbsolutePath();
    }

    private static Path ensureDir(Path dir) {
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (Exception e) {
            logger.warn("Failed to create directory: {}", dir, e);
            return dir;
        }
    }
}
