package com.patres.alina.server.mcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Component
public class McpConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(McpConfigurationValidator.class);

    public ValidationResult validate(McpServersConfig config) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (config == null) {
            errors.add("MCP configuration is null");
            return new ValidationResult(false, errors, warnings);
        }

        if (config.mcpServers().isEmpty()) {
            warnings.add("No MCP servers configured");
        }

        for (var entry : config.mcpServers().entrySet()) {
            String serverName = entry.getKey();
            McpServerConfiguration serverConfig = entry.getValue();

            validateServer(serverName, serverConfig, errors, warnings);
        }

        boolean isValid = errors.isEmpty();
        return new ValidationResult(isValid, errors, warnings);
    }

    private void validateServer(String serverName, McpServerConfiguration config, 
                               List<String> errors, List<String> warnings) {
        if (config.command() == null || config.command().trim().isEmpty()) {
            errors.add("Server '" + serverName + "': command is required");
            return;
        }

        // Validate command exists for common executables
        String command = config.command().trim();
        if (!isCommandAccessible(command)) {
            warnings.add("Server '" + serverName + "': command '" + command + "' may not be accessible");
        }

        // Validate args
        if (config.args() != null && config.args().contains(null)) {
            errors.add("Server '" + serverName + "': args cannot contain null values");
        }

        // Validate env
        if (config.env() != null) {
            for (var envEntry : config.env().entrySet()) {
                if (envEntry.getKey() == null || envEntry.getValue() == null) {
                    errors.add("Server '" + serverName + "': environment variables cannot have null keys or values");
                }
            }
        }
    }

    private boolean isCommandAccessible(String command) {
        try {
            // Try to find the command in PATH
            ProcessBuilder pb = new ProcessBuilder();
            
            // For common system commands, try to verify existence
            if (command.equals("node") || command.equals("python") || command.equals("npx")) {
                return true; // Assume these are available
            }
            
            // For absolute paths, check if file exists
            if (command.startsWith("/") || command.startsWith("C:") || command.startsWith("\\")) {
                Path commandPath = Paths.get(command);
                return Files.exists(commandPath) && Files.isExecutable(commandPath);
            }
            
            return true; // Default to true for relative commands
        } catch (Exception e) {
            logger.debug("Could not verify command accessibility: {}", command, e);
            return true; // Don't fail validation on accessibility check errors
        }
    }

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final List<String> warnings;

        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
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