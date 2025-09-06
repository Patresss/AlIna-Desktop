package com.patres.alina.common.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class AppPaths {

    private static final Logger logger = LoggerFactory.getLogger(AppPaths.class);

    private static final String APP_DIR_NAME = "AlIna";
    private static final String PROP_STORAGE_BASE = "storage.local.base.path";
    private static final String ENV_APPDATA = "APPDATA";
    private static final String ENV_XDG_DATA_HOME = "XDG_DATA_HOME";
    private static final String DIR_DATA = "data";
    private static final String PROP_OS_NAME = "os.name";
    private static final String PROP_USER_HOME = "user.home";

    private static volatile Path cachedBaseDir;

    private AppPaths() {
    }

    public static Path baseDataDir() {
        final Path current = cachedBaseDir;
        if (current != null) {
            return current;
        }
        synchronized (AppPaths.class) {
            if (cachedBaseDir == null) {
                cachedBaseDir = determineBaseDataDir();
            }
            return cachedBaseDir;
        }
    }

    public static Path resolve(final String relative) {
        final Path safeRelative = validateRelative(relative);
        final Path base = baseDataDir().normalize();
        final Path candidate = base.resolve(safeRelative).normalize();
        if (!candidate.startsWith(base)) {
            throw new SecurityException("Path escapes base data directory: " + relative);
        }
        return candidate;
    }

    private static Path determineBaseDataDir() {
        Path override = overrideBaseDir();
        if (override != null) {
            return ensureDir(override);
        }
        Path projectLocal = projectLocalDataDir();
        return ensureDir(Objects.requireNonNullElseGet(projectLocal, AppPaths::platformDefaultBaseDir));
    }

    private static Path overrideBaseDir() {
        final String override = System.getProperty(PROP_STORAGE_BASE);
        if (override == null || override.isBlank()) {
            return null;
        }
        return Paths.get(override).toAbsolutePath().normalize();
    }

    private static Path projectLocalDataDir() {
        final Path dir = Paths.get(DIR_DATA);
        return Files.isDirectory(dir) ? dir.toAbsolutePath().normalize() : null;
    }

    private static Path platformDefaultBaseDir() {
        final OperatingSystem os = OperatingSystem.detect(System.getProperty(PROP_OS_NAME, ""));
        final String userHome = System.getProperty(PROP_USER_HOME, ".");
        return switch (os) {
            case MAC -> Paths.get(userHome, "Library", "Application Support", APP_DIR_NAME).toAbsolutePath().normalize();
            case WINDOWS -> {
                final String appData = getenvOrDefault(ENV_APPDATA, Paths.get(userHome, "AppData", "Roaming").toString());
                yield Paths.get(appData, APP_DIR_NAME).toAbsolutePath().normalize();
            }
            case LINUX, OTHER -> {
                final String xdgDataHome = getenvOrDefault(ENV_XDG_DATA_HOME, Paths.get(userHome, ".local", "share").toString());
                yield Paths.get(xdgDataHome, APP_DIR_NAME).toAbsolutePath().normalize();
            }
        };
    }

    private static String getenvOrDefault(final String name, final String fallback) {
        final String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static Path validateRelative(final String relative) {
        Objects.requireNonNull(relative, "relative path");
        if (relative.isBlank()) {
            throw new IllegalArgumentException("Relative path cannot be blank");
        }
        final Path p = Paths.get(relative);
        if (p.isAbsolute()) {
            throw new IllegalArgumentException("Absolute paths are not allowed: " + relative);
        }
        return p.normalize();
    }

    private static Path ensureDir(final Path dir) {
        try {
            Files.createDirectories(dir);
            return dir;
        } catch (Exception e) {
            logger.warn("Failed to create directory: {}", dir, e);
            return dir;
        }
    }
}
