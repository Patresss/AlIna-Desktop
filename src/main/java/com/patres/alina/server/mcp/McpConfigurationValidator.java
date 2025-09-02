package com.patres.alina.server.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class McpConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(McpConfigurationValidator.class);

    public ValidationResult validate(final McpServersConfig config) {
        final List<String> errors = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();

        if (config == null) {
            errors.add("MCP configuration is null");
            return new ValidationResult(false, errors, warnings);
        }

        if (config.mcpServers().isEmpty()) {
            warnings.add("No MCP servers configured");
        }

        config.mcpServers().forEach((name, server) -> validateServer(name, server, errors, warnings));

        return new ValidationResult(errors.isEmpty(), errors, warnings);
    }

    private void validateServer(final String serverName,
                                final McpServerConfiguration config,
                                final List<String> errors,
                                final List<String> warnings) {
        if (config.command() == null || config.command().trim().isEmpty()) {
            errors.add("Server '" + serverName + "': command is required");
            return;
        }

        final String command = config.command().trim();
        if (!isCommandLikelyAccessible(command)) {
            warnings.add("Server '" + serverName + "': command '" + command + "' may not be accessible");
        }

        // Avoid List.of().contains(null) which throws NPE on JDK immutable lists
        if (config.args() != null && config.args().stream().anyMatch(java.util.Objects::isNull)) {
            errors.add("Server '" + serverName + "': args cannot contain null values");
        }

        if (config.env() != null) {
            config.env().forEach((k, v) -> {
                if (k == null || v == null) {
                    errors.add("Server '" + serverName + "': environment variables cannot have null keys or values");
                }
            });
        }
    }

    private boolean isCommandLikelyAccessible(final String command) {
        try {
            if (command.equals("node") || command.equals("python") || command.equals("npx")) {
                return true;
            }
            if (command.startsWith("/") || command.startsWith("C:") || command.startsWith("\\")) {
                final Path commandPath = Paths.get(command);
                return Files.exists(commandPath) && Files.isExecutable(commandPath);
            }
            return true; // default optimistic assumption
        } catch (final Exception e) {
            logger.debug("Could not verify command accessibility: {}", command, e);
            return true;
        }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(final boolean valid, final List<String> errors, final List<String> warnings) {
            this.valid = valid;
            this.errors = List.copyOf(errors);
            this.warnings = List.copyOf(warnings);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }
}
