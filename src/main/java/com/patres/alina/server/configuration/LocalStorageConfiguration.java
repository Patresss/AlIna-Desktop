package com.patres.alina.server.configuration;

import com.patres.alina.common.storage.OpenCodePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.patres.alina.common.storage.AppPaths;

import java.nio.file.Path;

@Configuration
public class LocalStorageConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(LocalStorageConfiguration.class);

    @Bean
    public Path localStorageBasePath() {
        final Path dataDir = AppPaths.baseDataDir();
        logger.info("Using local storage base directory: {}", dataDir);
        return dataDir;
    }

    @Bean
    public Path commandsStoragePath(final Path localStorageBasePath) {
        final Path commandsDir = OpenCodePaths.commandsDir();
        logger.info("Using legacy global commands storage directory: {}", commandsDir);
        return commandsDir;
    }
}
