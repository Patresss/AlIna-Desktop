package com.patres.alina.server.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.settings.WorkspaceSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Component
public class CodexAppServerClient {

    private static final Logger logger = LoggerFactory.getLogger(CodexAppServerClient.class);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration VERSION_TIMEOUT = Duration.ofSeconds(5);

    private final FileManager<WorkspaceSettings> workspaceSettingsManager;
    private final ObjectMapper objectMapper;
    private final AtomicLong nextRequestId = new AtomicLong(1);
    private final ConcurrentHashMap<Long, CompletableFuture<JsonNode>> pendingRequests = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Consumer<JsonNode>> messageListeners = new CopyOnWriteArrayList<>();

    private volatile Process process;
    private volatile BufferedWriter stdin;
    private volatile String processSignature;
    private volatile boolean initialized;

    public CodexAppServerClient(final FileManager<WorkspaceSettings> workspaceSettingsManager,
                                final ObjectMapper objectMapper) {
        this.workspaceSettingsManager = workspaceSettingsManager;
        this.objectMapper = objectMapper;
    }

    public void addMessageListener(final Consumer<JsonNode> listener) {
        messageListeners.add(listener);
    }

    public JsonNode request(final String method, final JsonNode params) throws Exception {
        ensureRunning();
        final long id = nextRequestId.getAndIncrement();
        final CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        final ObjectNode message = objectMapper.createObjectNode();
        message.put("method", method);
        message.set("params", params == null ? objectMapper.createObjectNode() : params);
        message.put("id", id);
        send(message);

        try {
            return future.get(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            pendingRequests.remove(id);
            throw new IllegalStateException("Codex app-server request timed out: " + method, e);
        }
    }

    public void notify(final String method, final JsonNode params) throws Exception {
        ensureRunning();
        final ObjectNode message = objectMapper.createObjectNode();
        message.put("method", method);
        message.set("params", params == null ? objectMapper.createObjectNode() : params);
        send(message);
    }

    public void respond(final long id, final JsonNode result) throws Exception {
        ensureRunning();
        final ObjectNode message = objectMapper.createObjectNode();
        message.put("id", id);
        message.set("result", result == null ? objectMapper.createObjectNode() : result);
        send(message);
    }

    public void respondError(final long id, final int code, final String messageText) throws Exception {
        ensureRunning();
        final ObjectNode message = objectMapper.createObjectNode();
        final ObjectNode error = message.putObject("error");
        message.put("id", id);
        error.put("code", code);
        error.put("message", messageText == null || messageText.isBlank() ? "Request rejected by client" : messageText);
        send(message);
    }

    public synchronized void ensureRunning() throws Exception {
        final WorkspaceSettings settings = workspaceSettingsManager.getSettings();
        final String desiredSignature = signature(settings);
        if (process != null && process.isAlive() && initialized && Objects.equals(desiredSignature, processSignature)) {
            return;
        }
        stop();
        start(settings, desiredSignature);
    }

    public synchronized void stop() {
        initialized = false;
        pendingRequests.forEach((_, future) -> future.completeExceptionally(
                new IllegalStateException("Codex app-server process stopped")
        ));
        pendingRequests.clear();
        closeQuietly(stdin);
        stdin = null;
        if (process != null) {
            process.destroyForcibly();
            process = null;
        }
    }

    public boolean isRunning() {
        return process != null && process.isAlive() && initialized;
    }

    public String readVersion() {
        final WorkspaceSettings settings = workspaceSettingsManager.getSettings();
        final List<String> command = List.of(settings.codexCommand(), "--version");
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            final Process versionProcess = processBuilder.start();
            final boolean finished = versionProcess.waitFor(VERSION_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            final String output = new String(versionProcess.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            if (!finished) {
                versionProcess.destroyForcibly();
                return null;
            }
            return versionProcess.exitValue() == 0 && !output.isBlank() ? output : null;
        } catch (Exception e) {
            logger.debug("Cannot read Codex version", e);
            return null;
        }
    }

    private void start(final WorkspaceSettings settings, final String desiredSignature) throws Exception {
        final Path workingDirectory = resolveWorkingDirectory(settings);
        if (!Files.isDirectory(workingDirectory)) {
            throw new IllegalStateException("Codex working directory does not exist: " + workingDirectory);
        }
        final ProcessBuilder processBuilder = new ProcessBuilder(settings.codexCommand(), "app-server");
        processBuilder.directory(workingDirectory.toFile());
        processBuilder.redirectErrorStream(false);

        process = processBuilder.start();
        processSignature = desiredSignature;
        stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        pumpServerMessages(process.getInputStream());
        pumpServerLogs(process.getErrorStream());
        initialize();
        logger.info("Codex app-server is ready via stdio in {}", workingDirectory);
    }

    private void initialize() throws Exception {
        final long id = nextRequestId.getAndIncrement();
        final CompletableFuture<JsonNode> future = new CompletableFuture<>();
        pendingRequests.put(id, future);

        final ObjectNode params = objectMapper.createObjectNode();
        final ObjectNode clientInfo = params.putObject("clientInfo");
        clientInfo.put("name", "alina_desktop");
        clientInfo.put("title", "AlIna Desktop");
        clientInfo.put("version", "1.0.2");
        params.putObject("capabilities").put("experimentalApi", true);

        final ObjectNode message = objectMapper.createObjectNode();
        message.put("method", "initialize");
        message.put("id", id);
        message.set("params", params);
        send(message);
        future.get(REQUEST_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

        final ObjectNode initializedMessage = objectMapper.createObjectNode();
        initializedMessage.put("method", "initialized");
        initializedMessage.set("params", objectMapper.createObjectNode());
        send(initializedMessage);
        initialized = true;
    }

    private synchronized void send(final ObjectNode message) throws IOException {
        if (stdin == null) {
            throw new IOException("Codex app-server stdin is closed.");
        }
        stdin.write(objectMapper.writeValueAsString(message));
        stdin.newLine();
        stdin.flush();
    }

    private void pumpServerMessages(final InputStream inputStream) {
        Thread.startVirtualThread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    handleIncomingLine(line);
                }
            } catch (IOException e) {
                logger.debug("Codex app-server message stream closed", e);
            } finally {
                initialized = false;
                pendingRequests.forEach((_, future) -> future.completeExceptionally(
                        new IllegalStateException("Codex app-server message stream closed")
                ));
                pendingRequests.clear();
            }
        });
    }

    private void handleIncomingLine(final String line) {
        try {
            final JsonNode message = objectMapper.readTree(line);
            if (message.has("id") && !message.has("method")) {
                completeResponse(message);
                return;
            }
            for (final Consumer<JsonNode> listener : messageListeners) {
                listener.accept(message);
            }
        } catch (Exception e) {
            logger.debug("Cannot process Codex app-server message: {}", line, e);
        }
    }

    private void completeResponse(final JsonNode message) {
        final long id = message.path("id").asLong();
        final CompletableFuture<JsonNode> future = pendingRequests.remove(id);
        if (future == null) {
            return;
        }
        if (message.has("error")) {
            final JsonNode error = message.path("error");
            final String errorMessage = error.path("message").asText("Codex app-server request failed");
            future.completeExceptionally(new IllegalStateException(errorMessage));
            return;
        }
        future.complete(message.path("result"));
    }

    private void pumpServerLogs(final InputStream inputStream) {
        Thread.startVirtualThread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("[codex app-server] {}", line);
                }
            } catch (IOException e) {
                logger.debug("Codex app-server log stream closed", e);
            }
        });
    }

    private String signature(final WorkspaceSettings settings) {
        return settings.codexCommand() + "|" + resolveWorkingDirectory(settings);
    }

    private Path resolveWorkingDirectory(final WorkspaceSettings settings) {
        return Path.of(settings.codexWorkingDirectory()).toAbsolutePath().normalize();
    }

    private void closeQuietly(final BufferedWriter writer) {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException ignored) {
        }
    }
}
