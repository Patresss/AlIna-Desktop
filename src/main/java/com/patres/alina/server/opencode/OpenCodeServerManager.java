package com.patres.alina.server.opencode;

import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.WorkspaceSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Component
public class OpenCodeServerManager {

    private static final Logger logger = LoggerFactory.getLogger(OpenCodeServerManager.class);
    private static final Duration SERVER_BOOT_TIMEOUT = Duration.ofSeconds(15);

    private final OpenCodeConfigurationService configurationService;
    private final OpenCodeHttpClient httpClient;

    private volatile Process serverProcess;
    private volatile String processSignature;

    public OpenCodeServerManager(final OpenCodeConfigurationService configurationService,
                                 final OpenCodeHttpClient httpClient) {
        this.configurationService = configurationService;
        this.httpClient = httpClient;
    }

    public synchronized void ensureRunning(final ConfigApplier configApplier) throws Exception {
        final WorkspaceSettings workspace = configurationService.workspaceSettings();
        final AssistantSettings assistant = configurationService.assistantSettings();
        final String desiredSignature = signature(workspace, assistant);

        if (serverProcess != null && serverProcess.isAlive() && desiredSignature.equals(processSignature) && httpClient.isHealthy()) {
            configApplier.apply(false);
            return;
        }

        if ((serverProcess == null || !serverProcess.isAlive()) && httpClient.isHealthy()) {
            if (destroyExistingOpenCodeServerOnConfiguredPort(workspace)) {
                logger.info("Restarting existing OpenCode server on {} to take ownership for AlIna", httpClient.baseUrl());
                restartServer(workspace, desiredSignature, configApplier);
                return;
            }
            processSignature = desiredSignature;
            logger.info("Using existing OpenCode server on {}", httpClient.baseUrl());
            configApplier.apply(true);
            return;
        }

        restartServer(workspace, desiredSignature, configApplier);
    }

    public synchronized void prepareForFreshChat(final ConfigApplier configApplier) throws Exception {
        processSignature = signature(configurationService.workspaceSettings(), configurationService.assistantSettings());
        if (httpClient.isHealthy()) {
            logger.info("Preparing existing OpenCode runtime for a fresh chat on {}", httpClient.baseUrl());
            configApplier.apply(true);
        } else {
            logger.info("OpenCode is not running yet; fresh chat will start a new session on first message");
        }
    }

    public boolean isManagedProcessAlive() {
        return serverProcess != null && serverProcess.isAlive();
    }

    private void restartServer(final WorkspaceSettings workspace,
                               final String desiredSignature,
                               final ConfigApplier configApplier) throws Exception {
        if (serverProcess != null && serverProcess.isAlive() && desiredSignature.equals(processSignature) && httpClient.isHealthy()) {
            configApplier.apply(false);
            return;
        }

        if (serverProcess != null) {
            serverProcess.destroyForcibly();
            serverProcess = null;
        }

        final ProcessBuilder processBuilder = new ProcessBuilder(
                OpenCodeConfigurationService.OPENCODE_COMMAND,
                "serve",
                "--hostname",
                workspace.openCodeHostname(),
                "--port",
                String.valueOf(workspace.openCodePort())
        );
        processBuilder.directory(configurationService.resolveWorkingDirectory().toFile());
        processBuilder.environment().putAll(configurationService.buildServerEnvironment());
        processBuilder.redirectErrorStream(true);

        serverProcess = processBuilder.start();
        processSignature = desiredSignature;
        pumpServerLogs(serverProcess.getInputStream());
        waitForHealth();
        configApplier.apply(true);
        logger.info("OpenCode server is ready on {}", httpClient.baseUrl());
    }

    private boolean destroyExistingOpenCodeServerOnConfiguredPort(final WorkspaceSettings workspace) throws InterruptedException {
        final String configuredPort = String.valueOf(workspace.openCodePort());
        final long currentPid = ProcessHandle.current().pid();
        final List<ProcessHandle> matchingProcesses = ProcessHandle.allProcesses()
                .filter(handle -> handle.pid() != currentPid)
                .filter(handle -> {
                    final String command = handle.info().command().orElse("");
                    if (command.isBlank() || !command.endsWith("opencode")) {
                        return false;
                    }
                    final String[] args = handle.info().arguments().orElse(new String[0]);
                    boolean serve = false;
                    boolean portMatch = false;
                    for (int i = 0; i < args.length; i++) {
                        final String arg = args[i];
                        if ("serve".equals(arg)) {
                            serve = true;
                        }
                        if ("--port".equals(arg) && i + 1 < args.length && configuredPort.equals(args[i + 1])) {
                            portMatch = true;
                        }
                    }
                    return serve && portMatch;
                })
                .toList();

        for (ProcessHandle processHandle : matchingProcesses) {
            logger.info("Stopping stale OpenCode process pid={} on configured port {}", processHandle.pid(), configuredPort);
            processHandle.destroyForcibly();
            processHandle.onExit().toCompletableFuture().join();
        }

        if (!matchingProcesses.isEmpty()) {
            Thread.sleep(250);
            return true;
        }
        return false;
    }

    private void waitForHealth() throws Exception {
        final long deadline = System.nanoTime() + SERVER_BOOT_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (httpClient.isHealthy()) {
                return;
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException("OpenCode server did not become healthy in time.");
    }

    private void pumpServerLogs(final InputStream inputStream) {
        Thread.startVirtualThread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[opencode] {}", line);
                }
            } catch (IOException e) {
                logger.debug("OpenCode log stream closed", e);
            }
        });
    }

    private String signature(final WorkspaceSettings workspace, final AssistantSettings assistant) {
        return String.join("|",
                workspace.openCodeHostname(),
                String.valueOf(workspace.openCodePort()),
                workspace.openCodeWorkingDirectory(),
                assistant.chatModel(),
                assistant.openAiApiKey(),
                String.valueOf(assistant.anthropicApiKey()),
                String.valueOf(assistant.googleApiKey())
        );
    }

    @FunctionalInterface
    public interface ConfigApplier {
        void apply(boolean force) throws Exception;
    }
}
