package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.Event;
import com.patres.alina.common.opencode.OpenCodeRuntimeStatus;
import com.patres.alina.common.permission.PermissionApprovalAction;
import com.patres.alina.common.permission.PermissionResolutionModel;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.storage.OpenCodePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class OpenCodeRuntimeService {

    private static final Logger logger = LoggerFactory.getLogger(OpenCodeRuntimeService.class);
    private static final Duration SERVER_BOOT_TIMEOUT = Duration.ofSeconds(15);

    private final OpenCodeConfigurationService configurationService;
    private final OpenCodeSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private final Map<String, ActiveStream> activeStreams = new ConcurrentHashMap<>();
    private final Map<String, PendingPermission> pendingPermissions = new ConcurrentHashMap<>();

    private volatile Process serverProcess;
    private volatile String processSignature;
    private volatile String appliedConfigJson;
    private volatile List<String> cachedModels = List.of();
    private volatile Instant cachedModelsAt = Instant.EPOCH;
    public OpenCodeRuntimeService(final OpenCodeConfigurationService configurationService,
                                  final OpenCodeSessionRegistry sessionRegistry,
                                  final ObjectMapper objectMapper) {
        this.configurationService = configurationService;
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean ownsPermissionRequest(final String requestId) {
        return pendingPermissions.containsKey(requestId);
    }

    public Flux<String> sendMessageStream(final String chatThreadId,
                                          final String chatThreadTitle,
                                          final String userMessage,
                                          final String systemPrompt,
                                          final String historySummary,
                                          final String modelOverride,
                                          final boolean forceNewSession) {
        return Flux.create(sink -> {
            final ActiveStream stream = new ActiveStream(chatThreadId, sink);
            activeStreams.put(chatThreadId, stream);
            sink.onDispose(() -> closeQuietly(stream));

            Thread.startVirtualThread(() -> {
                try {
                    ensureServerRunning();
                    final SessionBootstrap session = getOrCreateSession(chatThreadId, chatThreadTitle, forceNewSession);
                    stream.sessionId = session.sessionId();

                    final InputStream eventStream = openEventStream();
                    stream.eventStream = eventStream;

                    final String composedSystemPrompt = composeSystemPrompt(systemPrompt, historySummary, session.newlyCreated());
                    sendPromptAsync(session.sessionId(), userMessage, composedSystemPrompt, modelOverride);
                    consumeEvents(stream, eventStream, sink);
                } catch (Exception e) {
                    if (!stream.cancelled.get()) {
                        sink.error(e);
                    }
                } finally {
                    activeStreams.remove(chatThreadId, stream);
                    closeQuietly(stream);
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    public void cancelStreaming(final String chatThreadId) {
        final ActiveStream stream = activeStreams.remove(chatThreadId);
        if (stream == null) {
            return;
        }
        stream.cancelled.set(true);
        if (stream.sessionId != null) {
            try {
                post("/session/%s/abort".formatted(stream.sessionId), objectMapper.createObjectNode());
            } catch (Exception e) {
                logger.debug("Cannot abort OpenCode session {}", stream.sessionId, e);
            }
        }
        closeQuietly(stream);
    }

    public PermissionResolutionModel resolvePermissionRequest(final String requestId,
                                                             final PermissionApprovalAction action) {
        final PendingPermission pendingPermission = pendingPermissions.remove(requestId);
        if (pendingPermission == null) {
            return PermissionResolutionModel.missing("To zapytanie o zgodę OpenCode nie jest już aktywne.");
        }

        final ObjectNode body = objectMapper.createObjectNode();
        switch (action) {
            case APPROVE_ONCE -> {
                body.put("response", "once");
                body.put("remember", false);
            }
            case APPROVE_ALWAYS -> {
                body.put("response", "always");
                body.put("remember", true);
            }
            case DENY -> {
                body.put("response", "reject");
                body.put("remember", false);
            }
        }

        try {
            post("/session/%s/permissions/%s".formatted(pendingPermission.sessionId(), requestId), body);
            final ActiveStream stream = activeStreams.get(pendingPermission.threadId());
            if (stream != null) {
                stream.pendingPermissionRequestIds.remove(requestId);
            }
            if (action == PermissionApprovalAction.DENY) {
                return PermissionResolutionModel.denied("Dostęp odrzucony.");
            }
            final boolean persisted = action == PermissionApprovalAction.APPROVE_ALWAYS;
            final String message = persisted
                    ? "Trwała zgoda została przekazana do OpenCode. OpenCode kontynuuje."
                    : "Jednorazowa zgoda przyjęta. OpenCode kontynuuje.";
            return PermissionResolutionModel.approved(persisted, true, message);
        } catch (Exception e) {
            logger.warn("Cannot resolve OpenCode permission request {}", requestId, e);
            return PermissionResolutionModel.denied("Nie udało się przekazać decyzji do OpenCode: " + e.getMessage());
        }
    }

    public List<String> getAvailableModels() {
        try {
            if (Instant.now().isBefore(cachedModelsAt.plusSeconds(30)) && !cachedModels.isEmpty()) {
                return cachedModels;
            }

            final TreeSet<String> models = new TreeSet<>(fetchAvailableModelsFromCli());
            if (models.isEmpty()) {
                ensureServerRunning();
                final JsonNode config = get("/global/config");
                final String current = config.path("model").asText(null);
                if (current != null && !current.isBlank()) {
                    models.add(current);
                }
            }
            if (models.isEmpty()) {
                models.add(configurationService.assistantSettings().resolveModelIdentifier());
            }
            cachedModels = List.copyOf(models);
            cachedModelsAt = Instant.now();
            return cachedModels;
        } catch (Exception e) {
            logger.warn("Cannot fetch available models from OpenCode", e);
            return List.of(configurationService.assistantSettings().resolveModelIdentifier());
        }
    }

    public String resolveEffectiveModelIdentifier() {
        return resolveEffectiveModelIdentifier(configurationService.assistantSettings(), getAvailableModels());
    }

    public OpenCodeRuntimeStatus getRuntimeStatus() {
        final WorkspaceSettings workspace = configurationService.workspaceSettings();
        final Path executable = Path.of(workspace.openCodeExecutablePath()).toAbsolutePath().normalize();
        final Path workingDirectory = configurationService.resolveWorkingDirectory();

        boolean healthy = false;
        String version = null;
        String statusMessage = null;

        try {
            final JsonNode health = get("/global/health");
            healthy = health.path("healthy").asBoolean(false);
            version = health.path("version").asText(null);
            statusMessage = healthy ? "OpenCode is reachable." : "OpenCode reported unhealthy status.";
        } catch (Exception e) {
            statusMessage = e.getMessage();
        }

        return new OpenCodeRuntimeStatus(
                executable.toString(),
                Files.exists(executable),
                Files.isExecutable(executable),
                workspace.openCodeHostname(),
                workspace.openCodePort(),
                baseUrl(),
                workingDirectory.toString(),
                Files.isDirectory(workingDirectory),
                serverProcess != null && serverProcess.isAlive(),
                healthy,
                version,
                statusMessage
        );
    }

    public synchronized void prepareForFreshChat() {
        try {
            activeStreams.values().forEach(stream -> {
                stream.cancelled.set(true);
                closeQuietly(stream);
            });
            activeStreams.clear();
            pendingPermissions.clear();
            cachedModels = List.of();
            cachedModelsAt = Instant.EPOCH;
            appliedConfigJson = null;

            final WorkspaceSettings workspace = configurationService.workspaceSettings();
            final AssistantSettings assistant = configurationService.assistantSettings();
            final String desiredSignature = signature(workspace, assistant);
            processSignature = desiredSignature;

            if (isHealthy()) {
                logger.info("Preparing existing OpenCode runtime for a fresh chat on {}", baseUrl());
                applyConfigIfNeeded();
            } else {
                logger.info("OpenCode is not running yet; fresh chat will start a new session on first message");
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot prepare OpenCode runtime for a fresh chat", e);
        }
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

    private synchronized void restartServer(final WorkspaceSettings workspace,
                                            final AssistantSettings assistant,
                                            final String desiredSignature) throws Exception {
        if (serverProcess != null && serverProcess.isAlive() && desiredSignature.equals(processSignature) && isHealthy()) {
            applyConfigIfNeeded();
            return;
        }

        if (serverProcess != null) {
            serverProcess.destroyForcibly();
            serverProcess = null;
        }

        final Path executable = Path.of(workspace.openCodeExecutablePath()).toAbsolutePath().normalize();
        if (!Files.isExecutable(executable)) {
            throw new IllegalStateException("OpenCode executable is not available: " + executable);
        }

        final ProcessBuilder processBuilder = new ProcessBuilder(
                executable.toString(),
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
        appliedConfigJson = null;
        pumpServerLogs(serverProcess.getInputStream());
        waitForHealth();
        applyConfigIfNeeded();
        logger.info("OpenCode server is ready on {}", baseUrl());
    }

    public String resolveEffectiveModelIdentifier(final AssistantSettings settings,
                                                  final List<String> availableModels) {
        final String requested = settings.resolveModelIdentifier();
        if (availableModels == null || availableModels.isEmpty()) {
            return requested;
        }
        if (availableModels.contains(requested)) {
            return requested;
        }

        final String requestedProvider = settings.resolveProviderId();
        final String requestedModelId = settings.resolveModelId();

        return availableModels.stream()
                .max(Comparator.comparingInt(model -> scoreModelCandidate(model, requestedProvider, requestedModelId)))
                .orElse(requested);
    }

    private void ensureServerRunning() throws Exception {
        final WorkspaceSettings workspace = configurationService.workspaceSettings();
        final AssistantSettings assistant = configurationService.assistantSettings();
        final String desiredSignature = signature(workspace, assistant);

        if (serverProcess != null && serverProcess.isAlive() && desiredSignature.equals(processSignature) && isHealthy()) {
            applyConfigIfNeeded();
            return;
        }

        if ((serverProcess == null || !serverProcess.isAlive()) && isHealthy()) {
            if (destroyExistingOpenCodeServerOnConfiguredPort(workspace)) {
                logger.info("Restarting existing OpenCode server on {} to take ownership for AlIna", baseUrl());
                restartServer(workspace, assistant, desiredSignature);
                return;
            }
            processSignature = desiredSignature;
            appliedConfigJson = null;
            logger.info("Using existing OpenCode server on {}", baseUrl());
            applyConfigIfNeeded();
            return;
        }

        restartServer(workspace, assistant, desiredSignature);
    }

    private void applyConfigIfNeeded() throws Exception {
        final ObjectNode config = configurationService.buildGlobalConfig();
        config.put("model", resolveEffectiveModelIdentifier());
        final String json = objectMapper.writeValueAsString(config);
        if (json.equals(appliedConfigJson)) {
            return;
        }
        patch("/global/config", config);
        appliedConfigJson = json;
    }

    private SessionBootstrap getOrCreateSession(final String chatThreadId,
                                                final String title,
                                                final boolean forceNewSession) throws Exception {
        if (forceNewSession) {
            sessionRegistry.remove(chatThreadId);
        }

        final String existing = sessionRegistry.get(chatThreadId);
        if (existing != null && !existing.isBlank()) {
            return new SessionBootstrap(existing, false);
        }

        final ObjectNode body = objectMapper.createObjectNode();
        body.put("title", title == null || title.isBlank() ? chatThreadId : title);
        final JsonNode response = post("/session", body);
        final String sessionId = response.path("id").asText();
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalStateException("OpenCode did not return a session id.");
        }
        sessionRegistry.put(chatThreadId, sessionId);
        return new SessionBootstrap(sessionId, true);
    }

    private void sendPromptAsync(final String sessionId,
                                 final String userMessage,
                                 final String systemPrompt,
                                 final String modelOverride) throws Exception {
        final ObjectNode body = objectMapper.createObjectNode();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            body.put("system", systemPrompt);
        }
        body.put("agent", "general");
        final ObjectNode model = body.putObject("model");
        final AssistantSettings assistant = configurationService.assistantSettings();
        final String effectiveModel = modelOverride == null || modelOverride.isBlank()
                ? resolveEffectiveModelIdentifier()
                : modelOverride.trim();
        model.put("providerID", providerPart(effectiveModel));
        model.put("modelID", modelPart(effectiveModel));

        final ArrayNode parts = body.putArray("parts");
        final ObjectNode text = parts.addObject();
        text.put("type", "text");
        text.put("text", userMessage);

        final HttpRequest request = HttpRequest.newBuilder(uri("/session/%s/prompt_async".formatted(sessionId)))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 204) {
            throw new IllegalStateException("OpenCode prompt_async failed: HTTP " + response.statusCode() + " " + response.body());
        }
    }

    private InputStream openEventStream() throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri("/event"))
                .header("accept", "text/event-stream")
                .GET()
                .build();
        final HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Cannot subscribe to OpenCode events: HTTP " + response.statusCode());
        }
        return response.body();
    }

    private void consumeEvents(final ActiveStream stream,
                               final InputStream eventStream,
                               final FluxSink<String> sink) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(eventStream, StandardCharsets.UTF_8))) {
            String line;
            StringBuilder payload = new StringBuilder();
            while (!stream.cancelled.get() && (line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    payload.append(line.substring(6));
                    continue;
                }
                if (!line.isEmpty()) {
                    continue;
                }
                if (!payload.isEmpty()) {
                    processEvent(stream, payload.toString(), sink);
                    payload.setLength(0);
                    if (stream.completed.get()) {
                        sink.complete();
                        return;
                    }
                }
            }
        }
    }

    private void processEvent(final ActiveStream stream,
                              final String json,
                              final FluxSink<String> sink) {
        try {
            final JsonNode event = objectMapper.readTree(json);
            final String type = event.path("type").asText();
            final JsonNode properties = event.path("properties");

            switch (type) {
                case "message.updated" -> handleMessageUpdated(stream, properties);
                case "message.part.delta" -> handleMessagePartDelta(stream, properties, sink);
                case "message.part.updated" -> handleMessagePartUpdated(stream, properties, sink);
                case "permission.asked" -> handlePermissionAsked(properties);
                case "session.error" -> handleSessionError(stream, properties, sink);
                case "session.idle" -> handleSessionIdle(stream, properties);
                default -> {
                }
            }
        } catch (Exception e) {
            logger.debug("Cannot process OpenCode event {}", json, e);
        }
    }

    private void handleMessageUpdated(final ActiveStream stream, final JsonNode properties) {
        if (!matchesSession(stream, properties.path("sessionID").asText())) {
            return;
        }
        final JsonNode info = properties.path("info");
        if (!"assistant".equals(info.path("role").asText())) {
            return;
        }
        final String messageId = info.path("id").asText(null);
        if (messageId != null && !messageId.isBlank()) {
            stream.assistantMessageId = messageId;
        }

        final String finish = info.path("finish").asText("");
        if ("stop".equalsIgnoreCase(finish)) {
            stream.completed.set(true);
        }
    }

    private void handleMessagePartDelta(final ActiveStream stream,
                                        final JsonNode properties,
                                        final FluxSink<String> sink) {
        if (!matchesSession(stream, properties.path("sessionID").asText())) {
            return;
        }
        if (!matchesAssistantMessage(stream, properties.path("messageID").asText())) {
            return;
        }
        if (!"text".equals(properties.path("field").asText())) {
            return;
        }
        final String partId = properties.path("partID").asText("");
        final String partType = stream.partTypes.getOrDefault(partId, "");
        final String phase = stream.textPartPhases.getOrDefault(partId, "");
        final String delta = properties.path("delta").asText("");

        if ("reasoning".equalsIgnoreCase(partType)) {
            final String updated = stream.reasoningParts.getOrDefault(partId, "") + delta;
            stream.reasoningParts.put(partId, updated);
            publishReasoning(stream.threadId, updated);
            return;
        }

        if ("commentary".equalsIgnoreCase(phase)) {
            stream.deltaCommentaryParts.add(partId);
            final String updated = stream.commentaryParts.getOrDefault(partId, "") + delta;
            stream.commentaryParts.put(partId, updated);
            publishCommentary(stream.threadId, updated);
            return;
        }

        stream.deltaTextParts.add(partId);
        sink.next(delta);
    }

    private void handleMessagePartUpdated(final ActiveStream stream,
                                          final JsonNode properties,
                                          final FluxSink<String> sink) {
        if (!matchesSession(stream, properties.path("sessionID").asText())) {
            return;
        }
        final JsonNode part = properties.path("part");
        final String type = part.path("type").asText();
        final String messageId = part.path("messageID").asText();
        final String partId = part.path("id").asText();
        if (partId != null && !partId.isBlank() && type != null && !type.isBlank()) {
            stream.partTypes.put(partId, type);
        }

        if ("reasoning".equals(type) && matchesAssistantMessage(stream, messageId)) {
            final String currentText = part.path("text").asText("");
            final String previousText = stream.reasoningParts.getOrDefault(partId, "");
            if (!currentText.isBlank() && !currentText.equals(previousText)) {
                publishReasoning(stream.threadId, currentText);
            }
            stream.reasoningParts.put(partId, currentText);
            return;
        }

        if ("text".equals(type) && matchesAssistantMessage(stream, messageId)) {
            final String currentText = part.path("text").asText("");
            final String phase = resolveTextPhase(part);
            if (!phase.isBlank()) {
                stream.textPartPhases.put(partId, phase);
            }

            final String effectivePhase = stream.textPartPhases.getOrDefault(partId, "");
            if ("commentary".equalsIgnoreCase(effectivePhase)) {
                final String previousText = stream.commentaryParts.getOrDefault(partId, "");
                stream.commentaryParts.put(partId, currentText);
                if (!stream.deltaCommentaryParts.contains(partId) && !currentText.equals(previousText) && !currentText.isBlank()) {
                    publishCommentary(stream.threadId, currentText);
                }
                return;
            }

            final String delta = resolveUpdatedTextDelta(stream.textParts, stream.deltaTextParts, partId, currentText);
            if (!delta.isEmpty()) {
                sink.next(delta);
            }
            return;
        }

        if (!matchesAssistantMessage(stream, messageId)) {
            return;
        }

        if ("tool".equals(type)) {
            final String callId = part.path("callID").asText();
            final JsonNode state = part.path("state");
            final String stateStatus = state.path("status").asText();
            final String dedupeKey = callId + ":" + stateStatus;
            if (stream.seenActivity.add(dedupeKey)) {
                publishActivity(stream.threadId, part.path("tool").asText(), state);
            }
        }
    }

    private void handlePermissionAsked(final JsonNode properties) {
        final String requestId = properties.path("id").asText(null);
        final String sessionId = properties.path("sessionID").asText(null);
        if (requestId == null || sessionId == null) {
            return;
        }
        final String threadId = threadIdForSession(sessionId);
        if (threadId == null) {
            return;
        }
        final String permission = resolvePermissionKey(properties);
        final List<String> patterns = extractPatterns(properties.path("patterns"));
        pendingPermissions.put(requestId, new PendingPermission(sessionId, threadId, permission));
        final ActiveStream stream = activeStreams.get(threadId);
        if (stream != null) {
            stream.pendingPermissionRequestIds.add(requestId);
        }

        final String message = buildPermissionMessage(properties);
        Event.publish(new ChatMessageStreamEvent(
                threadId,
                mapPermissionType(permission),
                requestId,
                permission,
                "OpenCode " + permission,
                message,
                OpenCodePaths.configFile().toString(),
                patterns.isEmpty() ? "" : patterns.toString()
        ));
    }

    private void handleSessionError(final ActiveStream stream,
                                    final JsonNode properties,
                                    final FluxSink<String> sink) {
        if (!matchesSession(stream, properties.path("sessionID").asText())) {
            return;
        }
        final JsonNode error = properties.path("error");
        final String message = error.path("data").path("message").asText(error.path("name").asText("OpenCode session error"));
        stream.completed.set(true);
        sink.error(new IllegalStateException(message));
    }

    private void handleSessionIdle(final ActiveStream stream, final JsonNode properties) {
        if (!matchesSession(stream, properties.path("sessionID").asText())) {
            return;
        }
        // Do not treat session.idle as final completion.
        // OpenCode can go idle between tool-call stages and then continue
        // with another assistant message in the same session.
    }

    private void publishActivity(final String threadId, final String toolName, final JsonNode state) {
        final String title = state.path("title").asText(toolName);
        final ChatMessageStreamEvent.ActivityType activityType = "skill".equalsIgnoreCase(toolName)
                ? ChatMessageStreamEvent.ActivityType.SKILL
                : ChatMessageStreamEvent.ActivityType.TOOL;
        final String detail = state.path("status").asText("");
        Event.publish(new ChatMessageStreamEvent(
                threadId,
                activityType,
                title,
                detail
        ));
    }

    private void publishReasoning(final String threadId, final String content) {
        Event.publish(new ChatMessageStreamEvent(threadId, content, true));
    }

    private void publishCommentary(final String threadId, final String content) {
        Event.publish(ChatMessageStreamEvent.commentary(threadId, content));
    }

    private String threadIdForSession(final String sessionId) {
        return activeStreams.values().stream()
                .filter(stream -> sessionId.equals(stream.sessionId))
                .map(stream -> stream.threadId)
                .findFirst()
                .orElse(null);
    }

    private boolean matchesSession(final ActiveStream stream, final String sessionId) {
        return sessionId != null && !sessionId.isBlank() && sessionId.equals(stream.sessionId);
    }

    private boolean matchesAssistantMessage(final ActiveStream stream, final String messageId) {
        return stream.assistantMessageId != null && stream.assistantMessageId.equals(messageId);
    }

    private String composeSystemPrompt(final String systemPrompt,
                                       final String historySummary,
                                       final boolean includeHistorySummary) {
        if (!includeHistorySummary || historySummary == null || historySummary.isBlank()) {
            return systemPrompt;
        }
        if (systemPrompt == null || systemPrompt.isBlank()) {
            return historySummary;
        }
        return systemPrompt + System.lineSeparator() + System.lineSeparator() + historySummary;
    }

    private String buildPermissionMessage(final JsonNode properties) {
        final String permission = resolvePermissionKey(properties);
        final String patterns = extractPatterns(properties.path("patterns")).toString();
        return "OpenCode wymaga zgody dla: " + permission
                + System.lineSeparator()
                + "Patterns: " + patterns;
    }

    private ChatMessageStreamEvent.PermissionType mapPermissionType(final String permission) {
        if ("bash".equalsIgnoreCase(permission)) {
            return ChatMessageStreamEvent.PermissionType.BASH;
        }
        if (permission != null && permission.startsWith("mcp_")) {
            return ChatMessageStreamEvent.PermissionType.MCP;
        }
        return ChatMessageStreamEvent.PermissionType.TOOL;
    }

    private String resolvePermissionKey(final JsonNode properties) {
        final String permission = properties.path("permission").asText("");
        final String tool = properties.path("tool").asText("");
        if (permission == null || permission.isBlank()) {
            return tool == null || tool.isBlank() ? "tool" : tool;
        }
        if (("tool".equalsIgnoreCase(permission) || "mcp".equalsIgnoreCase(permission)) && tool != null && !tool.isBlank()) {
            return tool;
        }
        return permission;
    }

    private String resolveTextPhase(final JsonNode part) {
        final JsonNode openai = part.path("metadata").path("openai");
        final String phase = openai.path("phase").asText("");
        return phase == null ? "" : phase.trim();
    }

    private List<String> extractPatterns(final JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        final java.util.ArrayList<String> patterns = new java.util.ArrayList<>();
        node.forEach(item -> {
            final String value = item.asText(null);
            if (value != null && !value.isBlank()) {
                patterns.add(value);
            }
        });
        return List.copyOf(patterns);
    }

    private boolean isHealthy() {
        try {
            final HttpRequest request = HttpRequest.newBuilder(uri("/global/health")).GET().build();
            final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private void waitForHealth() throws Exception {
        final long deadline = System.nanoTime() + SERVER_BOOT_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            if (isHealthy()) {
                return;
            }
            Thread.sleep(250);
        }
        throw new IllegalStateException("OpenCode server did not become healthy in time.");
    }

    private JsonNode get(final String path) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri(path))
                .GET()
                .build();
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenCode request failed: HTTP " + response.statusCode() + " " + response.body());
        }
        if (response.body() == null || response.body().isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(response.body());
    }

    private JsonNode post(final String path, final JsonNode body) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenCode request failed: HTTP " + response.statusCode() + " " + response.body());
        }
        if (response.body() == null || response.body().isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(response.body());
    }

    private JsonNode patch(final String path, final JsonNode body) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("content-type", "application/json")
                .method("PATCH", HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("OpenCode config update failed: HTTP " + response.statusCode() + " " + response.body());
        }
        return response.body() == null || response.body().isBlank()
                ? objectMapper.createObjectNode()
                : objectMapper.readTree(response.body());
    }

    private URI uri(final String path) {
        return URI.create(baseUrl() + path);
    }

    private List<String> fetchAvailableModelsFromCli() throws IOException, InterruptedException {
        final WorkspaceSettings workspace = configurationService.workspaceSettings();
        final Path executable = Path.of(workspace.openCodeExecutablePath()).toAbsolutePath().normalize();
        if (!Files.isExecutable(executable)) {
            return List.of();
        }

        final ProcessBuilder processBuilder = new ProcessBuilder(executable.toString(), "models");
        processBuilder.directory(configurationService.resolveWorkingDirectory().toFile());
        processBuilder.environment().putAll(configurationService.buildServerEnvironment());
        processBuilder.redirectErrorStream(true);

        final Process process = processBuilder.start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return List.of();
        }
        if (process.exitValue() != 0) {
            return List.of();
        }

        return output.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> line.contains("/"))
                .toList();
    }

    private String baseUrl() {
        final WorkspaceSettings workspace = configurationService.workspaceSettings();
        return "http://" + workspace.openCodeHostname() + ":" + workspace.openCodePort();
    }

    static String resolveUpdatedTextDelta(final Map<String, String> textParts,
                                          final Set<String> deltaTextParts,
                                          final String partId,
                                          final String currentText) {
        final String previousText = textParts.getOrDefault(partId, "");
        textParts.put(partId, currentText);

        if (partId != null && !partId.isBlank() && deltaTextParts.contains(partId)) {
            return "";
        }
        if (currentText.startsWith(previousText) && currentText.length() > previousText.length()) {
            return currentText.substring(previousText.length());
        }
        return "";
    }

    static boolean isReasoningPart(final Map<String, String> partTypes, final String partId) {
        if (partId == null || partId.isBlank()) {
            return false;
        }
        return "reasoning".equalsIgnoreCase(partTypes.getOrDefault(partId, ""));
    }

    private int scoreModelCandidate(final String candidate,
                                    final String requestedProvider,
                                    final String requestedModelId) {
        final String candidateProvider = providerPart(candidate);
        final String candidateModel = modelPart(candidate);
        int score = 0;

        if (candidate.equalsIgnoreCase(requestedProvider + "/" + requestedModelId)) {
            return Integer.MAX_VALUE;
        }
        if (candidateProvider.equalsIgnoreCase(requestedProvider)) {
            score += 1000;
        }
        if (candidateModel.equalsIgnoreCase(requestedModelId)) {
            score += 500;
        }
        score += longestCommonPrefix(candidateModel.toLowerCase(), requestedModelId.toLowerCase()) * 10;
        score += sharedKeywordBonus(candidateModel.toLowerCase(), requestedModelId.toLowerCase(), "mini", 60);
        score += sharedKeywordBonus(candidateModel.toLowerCase(), requestedModelId.toLowerCase(), "codex", 60);
        score += sharedKeywordBonus(candidateModel.toLowerCase(), requestedModelId.toLowerCase(), "flash", 40);
        score += sharedKeywordBonus(candidateModel.toLowerCase(), requestedModelId.toLowerCase(), "pro", 30);
        score += sharedKeywordBonus(candidateModel.toLowerCase(), requestedModelId.toLowerCase(), "opus", 30);
        score += sharedKeywordBonus(candidateModel.toLowerCase(), requestedModelId.toLowerCase(), "sonnet", 30);
        return score;
    }

    private int longestCommonPrefix(final String left, final String right) {
        final int max = Math.min(left.length(), right.length());
        int count = 0;
        while (count < max && left.charAt(count) == right.charAt(count)) {
            count++;
        }
        return count;
    }

    private int sharedKeywordBonus(final String candidate,
                                   final String requested,
                                   final String keyword,
                                   final int value) {
        return candidate.contains(keyword) && requested.contains(keyword) ? value : 0;
    }

    private String providerPart(final String modelIdentifier) {
        if (modelIdentifier == null || !modelIdentifier.contains("/")) {
            return configurationService.assistantSettings().resolveProviderId();
        }
        return modelIdentifier.substring(0, modelIdentifier.indexOf('/'));
    }

    private String modelPart(final String modelIdentifier) {
        if (modelIdentifier == null || !modelIdentifier.contains("/")) {
            return configurationService.assistantSettings().resolveModelId();
        }
        return modelIdentifier.substring(modelIdentifier.indexOf('/') + 1);
    }

    private String signature(final WorkspaceSettings workspace, final AssistantSettings assistant) {
        return String.join("|",
                workspace.openCodeExecutablePath(),
                workspace.openCodeHostname(),
                String.valueOf(workspace.openCodePort()),
                workspace.openCodeWorkingDirectory(),
                assistant.chatModel(),
                assistant.openAiApiKey(),
                String.valueOf(assistant.anthropicApiKey()),
                String.valueOf(assistant.googleApiKey())
        );
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

    private void closeQuietly(final ActiveStream stream) {
        final Closeable closeable = stream.eventStream;
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    private record SessionBootstrap(String sessionId, boolean newlyCreated) {
    }

    private record PendingPermission(String sessionId,
                                     String threadId,
                                     String permissionKey) {
    }

    private static final class ActiveStream {
        private final String threadId;
        private final FluxSink<String> sink;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final Map<String, String> textParts = new ConcurrentHashMap<>();
        private final Map<String, String> partTypes = new ConcurrentHashMap<>();
        private final Map<String, String> reasoningParts = new ConcurrentHashMap<>();
        private final Map<String, String> commentaryParts = new ConcurrentHashMap<>();
        private final Map<String, String> textPartPhases = new ConcurrentHashMap<>();
        private final Set<String> deltaTextParts = ConcurrentHashMap.newKeySet();
        private final Set<String> deltaCommentaryParts = ConcurrentHashMap.newKeySet();
        private final Set<String> seenActivity = ConcurrentHashMap.newKeySet();
        private final Set<String> pendingPermissionRequestIds = ConcurrentHashMap.newKeySet();
        private volatile String sessionId;
        private volatile String assistantMessageId;
        private volatile Closeable eventStream;

        private ActiveStream(final String threadId, final FluxSink<String> sink) {
            this.threadId = threadId;
            this.sink = sink;
        }
    }
}
