package com.patres.alina.server.opencode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.ChatThreadTitleUpdatedEvent;
import com.patres.alina.common.event.Event;
import com.patres.alina.common.message.TodoItem;
import com.patres.alina.common.opencode.OpenCodeRuntimeStatus;
import com.patres.alina.common.permission.PermissionApprovalAction;
import com.patres.alina.common.permission.PermissionResolutionModel;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.WorkspaceSettings;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class OpenCodeRuntimeService {

    private static final Logger logger = LoggerFactory.getLogger(OpenCodeRuntimeService.class);

    private final OpenCodeConfigurationService configurationService;
    private final OpenCodeSessionRegistry sessionRegistry;
    private final OpenCodeHttpClient httpClient;
    private final OpenCodePermissionBridge permissionBridge;
    private final OpenCodeServerManager serverManager;
    private final OpenCodeModelService modelService;
    private final ObjectMapper objectMapper;

    private final Map<String, ActiveStream> activeStreams = new ConcurrentHashMap<>();

    private volatile String appliedConfigJson;

    public OpenCodeRuntimeService(final OpenCodeConfigurationService configurationService,
                                  final OpenCodeSessionRegistry sessionRegistry,
                                  final OpenCodeHttpClient httpClient,
                                  final OpenCodePermissionBridge permissionBridge,
                                  final OpenCodeServerManager serverManager,
                                  final OpenCodeModelService modelService,
                                  final ObjectMapper objectMapper) {
        this.configurationService = configurationService;
        this.sessionRegistry = sessionRegistry;
        this.httpClient = httpClient;
        this.permissionBridge = permissionBridge;
        this.serverManager = serverManager;
        this.modelService = modelService;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return true;
    }

    public boolean ownsPermissionRequest(final String requestId) {
        return permissionBridge.owns(requestId);
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
                httpClient.post("/session/%s/abort".formatted(stream.sessionId), objectMapper.createObjectNode());
            } catch (Exception e) {
                logger.debug("Cannot abort OpenCode session {}", stream.sessionId, e);
            }
        }
        closeQuietly(stream);
    }

    public PermissionResolutionModel resolvePermissionRequest(final String requestId,
                                                             final PermissionApprovalAction action) {
        return permissionBridge.resolve(requestId, action, (resolvedRequestId, pendingPermission) -> {
            final ActiveStream stream = activeStreams.get(pendingPermission.threadId());
            if (stream != null) {
                stream.pendingPermissionRequestIds.remove(resolvedRequestId);
            }
        });
    }

    public List<String> getAvailableModels() {
        return modelService.getAvailableModels();
    }

    public String resolveEffectiveModelIdentifier() {
        return modelService.resolveEffectiveModelIdentifier();
    }

    /**
     * Builds the full OpenCode web UI URL for the session mapped to the given chat thread.
     * The web UI path uses the base64-encoded working directory as the project segment:
     * {@code http://host:port/{base64(directory)}/session/{sessionId}}.
     * Returns {@code null} when the thread has no mapped session or when the directory
     * cannot be resolved from the OpenCode API.
     */
    public String getSessionWebUrl(final String chatThreadId) {
        if (chatThreadId == null || chatThreadId.isBlank()) {
            return null;
        }
        final String sessionId = sessionRegistry.get(chatThreadId);
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        try {
            final JsonNode sessionInfo = httpClient.get("/session/" + sessionId);
            final String directory = sessionInfo.path("directory").asText(null);
            if (directory == null || directory.isBlank()) {
                logger.warn("OpenCode session {} has no directory", sessionId);
                return null;
            }
            final String encodedDirectory = java.util.Base64.getEncoder().encodeToString(
                    directory.getBytes(StandardCharsets.UTF_8)
            );
            return httpClient.baseUrl() + "/" + encodedDirectory + "/session/" + sessionId;
        } catch (Exception e) {
            logger.warn("Cannot resolve OpenCode session web URL for thread {}", chatThreadId, e);
            return null;
        }
    }

    public OpenCodeRuntimeStatus getRuntimeStatus() {
        final WorkspaceSettings workspace = configurationService.workspaceSettings();
        final Path workingDirectory = configurationService.resolveWorkingDirectory();

        boolean healthy = false;
        String version = null;
        String statusMessage = null;

        try {
            final JsonNode health = httpClient.get("/global/health");
            healthy = health.path("healthy").asBoolean(false);
            version = health.path("version").asText(null);
            statusMessage = healthy ? "OpenCode is reachable." : "OpenCode reported unhealthy status.";
        } catch (Exception e) {
            statusMessage = e.getMessage();
        }

        return new OpenCodeRuntimeStatus(
                workspace.openCodeHostname(),
                workspace.openCodePort(),
                httpClient.baseUrl(),
                workingDirectory.toString(),
                Files.isDirectory(workingDirectory),
                serverManager.isManagedProcessAlive(),
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
            permissionBridge.clear();
            modelService.resetCache();
            serverManager.prepareForFreshChat(this::applyConfigIfNeeded);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot prepare OpenCode runtime for a fresh chat", e);
        }
    }

    private void ensureServerRunning() throws Exception {
        serverManager.ensureRunning(this::applyConfigIfNeeded);
    }

    private void applyConfigIfNeeded(final boolean force) throws Exception {
        final ObjectNode config = configurationService.buildGlobalConfig();
        config.put("model", resolveEffectiveModelIdentifier());
        final String json = objectMapper.writeValueAsString(config);
        if (!force && json.equals(appliedConfigJson)) {
            return;
        }
        httpClient.patch("/global/config", config);
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

        // If the chatThreadId itself is already an OpenCode sessionId (loaded from history),
        // register it and reuse it instead of creating a new session.
        if (chatThreadId != null && chatThreadId.startsWith("ses_")) {
            sessionRegistry.put(chatThreadId, chatThreadId);
            return new SessionBootstrap(chatThreadId, false);
        }

        final ObjectNode body = objectMapper.createObjectNode();
        // Leave title empty so OpenCode's built-in title agent can generate it automatically
        if (title != null && !title.isBlank()) {
            body.put("title", title);
        }
        final JsonNode response = httpClient.post("/session", body);
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
        model.put("providerID", modelService.providerPart(effectiveModel));
        model.put("modelID", modelService.modelPart(effectiveModel));

        final ArrayNode parts = body.putArray("parts");
        final ObjectNode text = parts.addObject();
        text.put("type", "text");
        text.put("text", userMessage);

        httpClient.postNoContent("/session/%s/prompt_async".formatted(sessionId), body);
    }

    private InputStream openEventStream() throws Exception {
        return httpClient.openEventStream();
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
                case "session.updated" -> handleSessionUpdated(properties);
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
            if (!messageId.equals(stream.assistantMessageId)) {
                // New assistant message started (e.g. after compaction) — reset completion flag
                // so we don't prematurely close the stream when a prior message finished.
                stream.completed.set(false);
                stream.assistantMessageId = messageId;
            }
        }

        final String modelId = info.path("modelID").asText("");
        final String agent = info.path("agent").asText("");
        // Detect compaction/summary messages — they use a dedicated mode and agent.
        // Their tokens must not be forwarded to the UI and their "stop" must not end the stream.
        final String mode = info.path("mode").asText("");
        final boolean isCompaction = "compaction".equalsIgnoreCase(mode)
                || "compaction".equalsIgnoreCase(agent);

        if (!isCompaction) {
            if (!modelId.isBlank()) {
                final String providerId = info.path("providerID").asText("");
                stream.modelUsed = providerId.isBlank() ? modelId : providerId + "/" + modelId;
            }
            if (!agent.isBlank()) {
                stream.agentUsed = agent;
            }
            final JsonNode tokensNode = info.path("tokens");
            if (!tokensNode.isMissingNode()) {
                final long total = tokensNode.path("total").asLong(0);
                if (total > 0) {
                    stream.tokensTotal = total;
                }
            }
            final double costValue = info.path("cost").asDouble(0.0);
            if (costValue > 0.0) {
                stream.cost = costValue;
            }
        }

        final String finish = info.path("finish").asText("");
        if ("stop".equalsIgnoreCase(finish) && !isCompaction) {
            // Compaction messages (mode/agent == "compaction") signal the end of a context-compression
            // turn, NOT the end of the user's request. OpenCode continues streaming a real assistant
            // response after compaction, so we must not treat this "stop" as final completion.
            stream.completed.set(true);
        }
    }

    private void handleMessagePartDelta(final ActiveStream stream,
                                        final JsonNode properties,
                                        final FluxSink<String> sink) {
        if (!matchesSession(stream, properties.path("sessionID").asText())) {
            return;
        }
        final String incomingMessageId = properties.path("messageID").asText(null);
        if (incomingMessageId != null && !incomingMessageId.isBlank() && stream.assistantMessageId == null) {
            stream.assistantMessageId = incomingMessageId;
        }
        if (!matchesAssistantMessage(stream, incomingMessageId)) {
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
            final String toolName = part.path("tool").asText("");
            if ("todowrite".equalsIgnoreCase(toolName)) {
                publishTodoUpdate(stream.threadId, part);
            }
        }
    }

    private void handlePermissionAsked(final JsonNode properties) {
        final String sessionId = properties.path("sessionID").asText(null);
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        final String threadId = threadIdForSession(sessionId);
        if (threadId == null) {
            return;
        }
        final OpenCodePermissionBridge.RegisteredPermission registeredPermission = permissionBridge.registerFromEvent(properties, threadId);
        if (registeredPermission == null) {
            return;
        }
        final ActiveStream stream = activeStreams.get(threadId);
        if (stream != null) {
            stream.pendingPermissionRequestIds.add(registeredPermission.requestId());
        }
        Event.publish(registeredPermission.event());
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

    private void handleSessionUpdated(final JsonNode properties) {
        final String sessionId = properties.path("session").path("id").asText(null);
        final String newTitle = properties.path("session").path("title").asText(null);
        if (sessionId == null || sessionId.isBlank() || newTitle == null || newTitle.isBlank()) {
            return;
        }
        final String threadId = threadIdForSession(sessionId);
        if (threadId == null) {
            return;
        }
        Event.publish(new ChatThreadTitleUpdatedEvent(threadId, newTitle));
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

    private void publishTodoUpdate(final String threadId, final JsonNode part) {
        try {
            final JsonNode todosNode = extractTodosFromPart(part);
            if (todosNode == null || !todosNode.isArray()) {
                return;
            }
            final List<TodoItem> items = new ArrayList<>();
            for (final JsonNode item : todosNode) {
                items.add(new TodoItem(
                        item.path("content").asText(""),
                        item.path("status").asText("pending"),
                        item.path("priority").asText("medium")
                ));
            }
            if (!items.isEmpty()) {
                logger.debug("TodoWrite: publishing {} todo items", items.size());
                Event.publish(ChatMessageStreamEvent.todoUpdate(threadId, items));
            }
        } catch (Exception e) {
            logger.warn("Cannot parse todowrite input from tool part", e);
        }
    }

    /**
     * Attempts to extract the todos JSON array from a todowrite tool part.
     * OpenCode places tool arguments inside {@code state.input} (and results
     * inside {@code state.metadata} / {@code state.output}).
     * This method probes all known locations.
     */
    private JsonNode extractTodosFromPart(final JsonNode part) {
        // Primary location: state.input.todos (present in "running" and "completed" states)
        final JsonNode state = part.path("state");
        final JsonNode stateInput = state.path("input");
        if (stateInput.isObject() && stateInput.has("todos")) {
            return stateInput.path("todos");
        }
        if (stateInput.isTextual()) {
            final JsonNode parsed = parseJsonQuietly(stateInput.asText());
            if (parsed != null && parsed.has("todos")) {
                return parsed.path("todos");
            }
        }
        // state.metadata.todos (present in "completed" state)
        final JsonNode metadata = state.path("metadata");
        if (metadata.isObject() && metadata.has("todos")) {
            return metadata.path("todos");
        }
        // state.output as stringified JSON array of todos (present in "completed" state)
        final JsonNode stateOutput = state.path("output");
        if (stateOutput.isTextual()) {
            final JsonNode parsed = parseJsonQuietly(stateOutput.asText());
            if (parsed != null && parsed.isArray()) {
                return parsed;
            }
        }
        if (stateOutput.isArray()) {
            return stateOutput;
        }
        // Fallbacks: part-level input/output/arguments
        final JsonNode input = part.path("input");
        if (input.isObject() && input.has("todos")) {
            return input.path("todos");
        }
        if (input.isTextual()) {
            final JsonNode parsed = parseJsonQuietly(input.asText());
            if (parsed != null && parsed.has("todos")) {
                return parsed.path("todos");
            }
        }
        final JsonNode output = part.path("output");
        if (output.isObject() && output.has("todos")) {
            return output.path("todos");
        }
        if (output.isTextual()) {
            final JsonNode parsed = parseJsonQuietly(output.asText());
            if (parsed != null && parsed.has("todos")) {
                return parsed.path("todos");
            }
        }
        return null;
    }

    private JsonNode parseJsonQuietly(final String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }

    public String getModelUsedForThread(final String threadId) {
        final ActiveStream stream = activeStreams.get(threadId);
        return stream != null ? stream.modelUsed : null;
    }

    public String getAgentUsedForThread(final String threadId) {
        final ActiveStream stream = activeStreams.get(threadId);
        return stream != null ? stream.agentUsed : null;
    }

    public long getTokensTotalForThread(final String threadId) {
        final ActiveStream stream = activeStreams.get(threadId);
        return stream != null ? stream.tokensTotal : 0;
    }

    public double getCostForThread(final String threadId) {
        final ActiveStream stream = activeStreams.get(threadId);
        return stream != null ? stream.cost : 0.0;
    }

    private String threadIdForSession(final String sessionId) {
        // First check active streams (fast path during streaming)
        final String fromStream = activeStreams.values().stream()
                .filter(stream -> sessionId.equals(stream.sessionId))
                .map(stream -> stream.threadId)
                .findFirst()
                .orElse(null);
        if (fromStream != null) {
            return fromStream;
        }
        // Fallback: reverse lookup in registry (handles title updates arriving after stream ends)
        return sessionRegistry.getThreadId(sessionId);
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

    private String resolveTextPhase(final JsonNode part) {
        final JsonNode openai = part.path("metadata").path("openai");
        final String phase = openai.path("phase").asText("");
        return phase == null ? "" : phase.trim();
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
        private volatile String modelUsed;
        private volatile String agentUsed;
        private volatile long tokensTotal;
        private volatile double cost;
        private volatile Closeable eventStream;

        private ActiveStream(final String threadId, final FluxSink<String> sink) {
            this.threadId = threadId;
            this.sink = sink;
        }
    }
}
