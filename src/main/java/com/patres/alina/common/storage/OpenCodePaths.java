package com.patres.alina.common.storage;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class OpenCodePaths {

    public static final String PROP_CONFIG_DIR = "opencode.config.dir";

    private OpenCodePaths() {
    }

    public static Path configDir() {
        final String override = System.getProperty(PROP_CONFIG_DIR);
        if (override != null && !override.isBlank()) {
            return Paths.get(override).toAbsolutePath().normalize();
        }
        return Paths.get(System.getProperty("user.home", "."), ".config", "opencode")
                .toAbsolutePath()
                .normalize();
    }

    public static Path configFile() {
        return configDir().resolve("opencode.json").normalize();
    }

    public static Path commandsDir() {
        return configDir().resolve("commands").normalize();
    }

    public static Path skillsDir() {
        return configDir().resolve("skills").normalize();
    }
}
