package com.patres.alina.server.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.agent.AgentBackend;
import com.patres.alina.common.agent.AgentRuntimeStatus;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.ChatThreadTitleUpdatedEvent;
import com.patres.alina.common.event.Event;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.message.ImageAttachment;
import com.patres.alina.common.message.TodoItem;
import com.patres.alina.common.permission.PermissionApprovalAction;
import com.patres.alina.common.permission.PermissionResolutionModel;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.agent.AgentMessageRequest;
import com.patres.alina.server.agent.AgentRuntime;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CodexAgentRuntime implements AgentRuntime {

    private static final Logger logger = LoggerFactory.getLogger(CodexAgentRuntime.class);
    private static final long STREAM_AWAIT_TIMEOUT_HOURS = 12;

    private final CodexAppServerClient client;
    private final FileManager<WorkspaceSettings> workspaceSettingsManager;
    private final FileManager<AssistantSettings> assistantSettingsManager;
    private final ObjectMapper objectMapper;

    private final Map<String, ActiveStream> activeStreams = new ConcurrentHashMap<>();
    private final Map<String, String> chatThreadToCodexThread = new ConcurrentHashMap<>();
    private final Map<String, String> codexThreadToChatThread = new ConcurrentHashMap<>();
    private final Map<String, PendingPermission> pendingPermissions = new ConcurrentHashMap<>();
    private final Map<String, String> itemToChatThread = new ConcurrentHashMap<>();
    private volatile List<String> cachedModels = List.of();

    public CodexAgentRuntime(final CodexAppServerClient client,
                             final FileManager<WorkspaceSettings> workspaceSettingsManager,
                             final FileManager<AssistantSettings> assistantSettingsManager,
                             final ObjectMapper objectMapper) {
        this.client = client;
        this.workspaceSettingsManager = workspaceSettingsManager;
        this.assistantSettingsManager = assistantSettingsManager;
        this.objectMapper = objectMapper;
        this.client.addMessageListener(this::handleServerMessage);
    }

    @Override
    public AgentBackend backend() {
        return AgentBackend.CODEX;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public Flux<String> sendMessageStream(final AgentMessageRequest request) {
        return Flux.create(sink -> {
            final ActiveStream stream = new ActiveStream(request.chatThreadId(), sink);
            activeStreams.put(request.chatThreadId(), stream);
            sink.onDispose(() -> {
                stream.cancelled.set(true);
                stream.completed.countDown();
            });

            Thread.startVirtualThread(() -> {
                try {
                    client.ensureRunning();
                    final String codexThreadId = getOrCreateThread(request);
                    stream.codexThreadId = codexThreadId;
                    stream.modelUsed = resolveCodexModel(request.modelOverride());
                    chatThreadToCodexThread.put(request.chatThreadId(), codexThreadId);
                    codexThreadToChatThread.put(codexThreadId, request.chatThreadId());

                    final JsonNode turnResponse = startTurn(codexThreadId, request, stream.modelUsed);
                    final JsonNode turn = turnResponse.path("turn");
                    stream.turnId = turn.path("id").asText(null);
                    indexItems(request.chatThreadId(), turn.path("items"));

                    stream.completed.await(STREAM_AWAIT_TIMEOUT_HOURS, TimeUnit.HOURS);
                } catch (Exception e) {
                    if (!stream.cancelled.get()) {
                        sink.error(e);
                    }
                } finally {
                    activeStreams.remove(request.chatThreadId(), stream);
                    if (stream.codexThreadId != null) {
                        codexThreadToChatThread.put(stream.codexThreadId, request.chatThreadId());
                    }
                }
            });
        }, FluxSink.OverflowStrategy.BUFFER);
    }

    @Override
    public void cancelStreaming(final String chatThreadId) {
        final ActiveStream stream = activeStreams.remove(chatThreadId);
        if (stream == null) {
            return;
        }
        stream.cancelled.set(true);
        try {
            if (stream.codexThreadId != null && stream.turnId != null) {
                final ObjectNode params = objectMapper.createObjectNode();
                params.put("threadId", stream.codexThreadId);
                params.put("turnId", stream.turnId);
                client.request("turn/interrupt", params);
            }
        } catch (Exception e) {
            logger.debug("Cannot interrupt Codex turn for thread {}", chatThreadId, e);
        } finally {
            stream.completed.countDown();
        }
    }

    @Override
    public boolean ownsPermissionRequest(final String requestId) {
        return pendingPermissions.containsKey(requestId);
    }

    @Override
    public PermissionResolutionModel resolvePermissionRequest(final String requestId,
                                                             final PermissionApprovalAction action) {
        final PendingPermission pendingPermission = pendingPermissions.remove(requestId);
        if (pendingPermission == null) {
            return PermissionResolutionModel.missing(LanguageManager.getLanguageString("chat.permission.missing"));
        }
        try {
            client.respond(pendingPermission.rpcId(), buildPermissionResult(pendingPermission, action));
            removePendingFromStream(pendingPermission);
            if (action == PermissionApprovalAction.DENY) {
                return PermissionResolutionModel.denied(LanguageManager.getLanguageString("chat.permission.denied"));
            }
            final boolean persisted = action == PermissionApprovalAction.APPROVE_ALWAYS;
            final String message = persisted
                    ? LanguageManager.getLanguageString("chat.permission.approvedAlways")
                    : LanguageManager.getLanguageString("chat.permission.approvedOnce");
            return PermissionResolutionModel.approved(persisted, true, message);
        } catch (Exception e) {
            logger.warn("Cannot resolve Codex permission request {}", requestId, e);
            return PermissionResolutionModel.denied(LanguageManager.getLanguageString("chat.permission.error", e.getMessage()));
        }
    }

    @Override
    public List<String> getAvailableModels() {
        try {
            final ObjectNode params = objectMapper.createObjectNode();
            params.put("limit", 100);
            params.put("includeHidden", false);
            final JsonNode response = client.request("model/list", params);
            final List<String> models = new ArrayList<>();
            final JsonNode data = response.path("data");
            if (data.isArray()) {
                for (final JsonNode model : data) {
                    final String id = model.path("id").asText(model.path("model").asText(null));
                    if (id != null && !id.isBlank()) {
                        models.add(id);
                    }
                }
            }
            if (!models.isEmpty()) {
                cachedModels = List.copyOf(models);
                return cachedModels;
            }
        } catch (Exception e) {
            logger.warn("Cannot fetch available models from Codex app-server", e);
        }
        final String configured = configuredCodexModel();
        return configured == null || configured.isBlank() ? List.of() : List.of(configured);
    }

    @Override
    public String resolveEffectiveModelIdentifier() {
        return resolveSupportedCodexModel(configuredCodexModel());
    }

    @Override
    public String getModelUsedForThread(final String threadId) {
        final ActiveStream stream = activeStreams.get(threadId);
        return stream != null ? stream.modelUsed : null;
    }

    @Override
    public String getAgentUsedForThread(final String threadId) {
        return AgentBackend.CODEX.displayName();
    }

    @Override
    public long getTokensTotalForThread(final String threadId) {
        final ActiveStream stream = activeStreams.get(threadId);
        return stream != null ? stream.tokensTotal : 0;
    }

    @Override
    public double getCostForThread(final String threadId) {
        return 0.0;
    }

    @Override
    public AgentRuntimeStatus getRuntimeStatus() {
        final WorkspaceSettings settings = workspaceSettingsManager.getSettings();
        final Path workingDirectory = resolveWorkingDirectory(settings);
        boolean healthy = client.isRunning();
        String statusMessage = healthy ? "Codex app-server is running." : "Codex app-server is not running.";
        String version = client.readVersion();
        if (!healthy) {
            try {
                client.ensureRunning();
                healthy = client.isRunning();
                statusMessage = healthy ? "Codex app-server started successfully." : statusMessage;
            } catch (Exception e) {
                statusMessage = e.getMessage();
            }
        }
        if (version == null || version.isBlank()) {
            version = client.readVersion();
        }
        return new AgentRuntimeStatus(
                AgentBackend.CODEX,
                AgentBackend.CODEX.displayName(),
                "stdio JSON-RPC",
                null,
                0,
                null,
                settings.codexCommand() + " app-server",
                workingDirectory.toString(),
                Files.isDirectory(workingDirectory),
                client.isRunning(),
                healthy,
                version,
                statusMessage
        );
    }

    @Override
    public void prepareForFreshChat() {
        activeStreams.values().forEach(stream -> {
            stream.cancelled.set(true);
            stream.completed.countDown();
        });
        activeStreams.clear();
        pendingPermissions.clear();
        itemToChatThread.clear();
    }

    @Override
    public String getSessionWebUrl(final String chatThreadId) {
        return null;
    }

    @Override
    public Optional<ChatThread> getChatThread(final String chatThreadId) {
        try {
            final String codexThreadId = resolveCodexThreadId(chatThreadId);
            final ObjectNode params = objectMapper.createObjectNode();
            params.put("threadId", codexThreadId);
            params.put("includeTurns", false);
            final JsonNode response = client.request("thread/read", params);
            return toChatThread(response.path("thread"));
        } catch (Exception e) {
            logger.warn("Failed to fetch Codex thread {}", chatThreadId, e);
            return Optional.empty();
        }
    }

    @Override
    public List<ChatThread> getChatThreads() {
        try {
            final ObjectNode params = objectMapper.createObjectNode();
            params.put("limit", 100);
            final JsonNode response = client.request("thread/list", params);
            final List<ChatThread> threads = new ArrayList<>();
            final JsonNode data = response.path("data");
            if (data.isArray()) {
                for (final JsonNode node : data) {
                    toChatThread(node).ifPresent(threads::add);
                }
            }
            threads.sort(Comparator.comparing(ChatThread::modifiedAt,
                    Comparator.nullsLast(Comparator.reverseOrder())));
            return threads;
        } catch (Exception e) {
            logger.warn("Failed to fetch Codex threads", e);
            return List.of();
        }
    }

    @Override
    public List<ChatMessageResponseModel> getMessagesByThreadId(final String chatThreadId) {
        try {
            final String codexThreadId = resolveCodexThreadId(chatThreadId);
            final ObjectNode params = objectMapper.createObjectNode();
            params.put("threadId", codexThreadId);
            params.put("includeTurns", true);
            final JsonNode response = client.request("thread/read", params);
            final List<ChatMessageResponseModel> messages = new ArrayList<>();
            collectMessages(response.path("thread").path("turns"), codexThreadId, messages);
            collectMessages(response.path("turns"), codexThreadId, messages);
            return messages;
        } catch (Exception e) {
            logger.warn("Failed to fetch Codex messages for thread {}", chatThreadId, e);
            return List.of();
        }
    }

    @Override
    public void deleteChatThread(final String chatThreadId) {
        try {
            final ObjectNode params = objectMapper.createObjectNode();
            params.put("threadId", resolveCodexThreadId(chatThreadId));
            client.request("thread/archive", params);
        } catch (Exception e) {
            logger.warn("Failed to archive Codex thread {}", chatThreadId, e);
        }
    }

    @Override
    public void renameChatThread(final String chatThreadId, final String newName) {
        try {
            final String codexThreadId = resolveCodexThreadId(chatThreadId);
            final ObjectNode params = objectMapper.createObjectNode();
            params.put("threadId", codexThreadId);
            params.put("name", newName);
            client.request("thread/name/set", params);
            Event.publish(new ChatThreadTitleUpdatedEvent(chatThreadId, newName));
        } catch (Exception e) {
            logger.warn("Failed to rename Codex thread {}", chatThreadId, e);
        }
    }

    private String getOrCreateThread(final AgentMessageRequest request) throws Exception {
        final String model = resolveCodexModel(request.modelOverride());
        if (request.forceNewSession()) {
            chatThreadToCodexThread.remove(request.chatThreadId());
        }
        final String existing = chatThreadToCodexThread.get(request.chatThreadId());
        if (existing != null && !existing.isBlank()) {
            resumeThread(existing, model, developerInstructions(request));
            return existing;
        }
        if (isCodexThreadId(request.chatThreadId())) {
            resumeThread(request.chatThreadId(), model, developerInstructions(request));
            chatThreadToCodexThread.put(request.chatThreadId(), request.chatThreadId());
            return request.chatThreadId();
        }

        final ObjectNode params = objectMapper.createObjectNode();
        if (model != null && !model.isBlank()) {
            params.put("model", model);
            params.put("modelProvider", "openai");
        }
        params.put("cwd", resolveWorkingDirectory(workspaceSettingsManager.getSettings()).toString());
        params.put("approvalsReviewer", "user");
        putDeveloperInstructions(params, developerInstructions(request));
        params.put("serviceName", "alina_desktop");
        params.put("sessionStartSource", request.forceNewSession() ? "clear" : "startup");
        final JsonNode response = client.request("thread/start", params);
        final String codexThreadId = response.path("thread").path("id").asText();
        if (codexThreadId == null || codexThreadId.isBlank()) {
            throw new IllegalStateException("Codex app-server did not return a thread id.");
        }
        chatThreadToCodexThread.put(request.chatThreadId(), codexThreadId);
        codexThreadToChatThread.put(codexThreadId, request.chatThreadId());
        return codexThreadId;
    }

    private void resumeThread(final String codexThreadId,
                              final String model,
                              final String developerInstructions) throws Exception {
        final ObjectNode params = objectMapper.createObjectNode();
        params.put("threadId", codexThreadId);
        if (model != null && !model.isBlank()) {
            params.put("model", model);
            params.put("modelProvider", "openai");
        }
        params.put("cwd", resolveWorkingDirectory(workspaceSettingsManager.getSettings()).toString());
        params.put("approvalsReviewer", "user");
        putDeveloperInstructions(params, developerInstructions);
        client.request("thread/resume", params);
    }

    private JsonNode startTurn(final String codexThreadId,
                               final AgentMessageRequest request,
                               final String model) throws Exception {
        final ObjectNode params = objectMapper.createObjectNode();
        params.put("threadId", codexThreadId);
        params.put("cwd", resolveWorkingDirectory(workspaceSettingsManager.getSettings()).toString());
        params.put("approvalsReviewer", "user");
        if (model != null && !model.isBlank()) {
            params.put("model", model);
        }
        final ArrayNode input = params.putArray("input");
        final ObjectNode text = input.addObject();
        text.put("type", "text");
        text.put("text", request.userMessage() == null ? "" : request.userMessage());
        addImageInputs(input, request.imageAttachments());
        return client.request("turn/start", params);
    }

    private void addImageInputs(final ArrayNode input, final List<ImageAttachment> imageAttachments) {
        for (final ImageAttachment image : imageAttachments) {
            final ObjectNode imageInput = input.addObject();
            imageInput.put("type", "image");
            imageInput.put("url", image.toDataUri());
        }
    }

    private String developerInstructions(final AgentMessageRequest request) {
        final StringBuilder builder = new StringBuilder();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            builder.append(request.systemPrompt().trim()).append(System.lineSeparator()).append(System.lineSeparator());
        }
        if (request.historySummary() != null && !request.historySummary().isBlank()) {
            builder.append(request.historySummary().trim());
        }
        return builder.toString();
    }

    private void putDeveloperInstructions(final ObjectNode params, final String developerInstructions) {
        if (developerInstructions != null && !developerInstructions.isBlank()) {
            params.put("developerInstructions", developerInstructions);
        }
    }

    private void handleServerMessage(final JsonNode message) {
        try {
            if (!message.has("method")) {
                return;
            }
            final String method = message.path("method").asText("");
            if (message.has("id")) {
                handleServerRequest(method, message.path("id").asLong(), message.path("params"));
                return;
            }
            handleNotification(method, message.path("params"));
        } catch (Exception e) {
            logger.debug("Cannot process Codex app-server event {}", message, e);
        }
    }

    private void handleServerRequest(final String method, final long rpcId, final JsonNode params) throws Exception {
        switch (method) {
            case "item/commandExecution/requestApproval",
                 "item/fileChange/requestApproval",
                 "item/permissions/requestApproval",
                 "item/tool/requestUserInput",
                 "mcpServer/elicitation/request" -> registerPermissionRequest(method, rpcId, params);
            case "item/tool/call" -> client.respondError(rpcId, -32601, "AlIna does not expose dynamic Codex tools.");
            default -> client.respondError(rpcId, -32601, "Unsupported Codex app-server request: " + method);
        }
    }

    private void handleNotification(final String method, final JsonNode params) {
        switch (method) {
            case "thread/started" -> handleThreadStarted(params);
            case "thread/name/updated" -> handleThreadNameUpdated(params);
            case "turn/started" -> handleTurnStarted(params);
            case "turn/completed" -> handleTurnCompleted(params);
            case "turn/plan/updated" -> handlePlanUpdated(params);
            case "thread/tokenUsage/updated" -> handleTokenUsageUpdated(params);
            case "item/started" -> handleItemStarted(params);
            case "item/completed" -> handleItemCompleted(params);
            case "item/agentMessage/delta" -> handleAgentMessageDelta(params);
            case "item/plan/delta" -> handlePlanDelta(params);
            case "item/reasoning/summaryTextDelta", "item/reasoning/textDelta" -> handleReasoningDelta(params);
            case "item/reasoning/summaryPartAdded" -> handleReasoningSummaryPartAdded(params);
            case "item/commandExecution/outputDelta" -> handleCommandOutputDelta(params);
            case "serverRequest/resolved" -> handleServerRequestResolved(params);
            case "error" -> handleError(params);
            default -> {
            }
        }
    }

    private void registerPermissionRequest(final String method, final long rpcId, final JsonNode params) {
        final String codexThreadId = eventThreadId(params);
        final String chatThreadId = chatThreadIdForCodexThread(codexThreadId);
        if (chatThreadId == null) {
            return;
        }
        final String requestId = String.valueOf(rpcId);
        final PendingPermission pendingPermission = new PendingPermission(requestId, rpcId, method, chatThreadId, codexThreadId, params);
        pendingPermissions.put(requestId, pendingPermission);
        final ActiveStream stream = activeStreams.get(chatThreadId);
        if (stream != null) {
            stream.pendingPermissionRequestIds.add(requestId);
        }
        Event.publish(new ChatMessageStreamEvent(
                chatThreadId,
                permissionType(method, params),
                requestId,
                permissionValue(method, params),
                permissionTitle(method, params),
                permissionMessage(method, params),
                resolvePermissionConfigPath(),
                permissionMatchedRule(method, params)
        ));
    }

    private JsonNode buildPermissionResult(final PendingPermission pendingPermission,
                                           final PermissionApprovalAction action) {
        if ("item/permissions/requestApproval".equals(pendingPermission.method())) {
            final ObjectNode result = objectMapper.createObjectNode();
            if (action == PermissionApprovalAction.DENY) {
                result.set("permissions", objectMapper.createObjectNode());
                return result;
            }
            result.set("permissions", pendingPermission.params().path("permissions").deepCopy());
            result.put("scope", action == PermissionApprovalAction.APPROVE_ALWAYS ? "session" : "turn");
            return result;
        }
        if ("mcpServer/elicitation/request".equals(pendingPermission.method())) {
            final ObjectNode result = objectMapper.createObjectNode();
            result.put("action", action == PermissionApprovalAction.DENY ? "decline" : "accept");
            result.set("content", action == PermissionApprovalAction.DENY
                    ? objectMapper.getNodeFactory().nullNode()
                    : objectMapper.createObjectNode());
            return result;
        }
        if ("item/tool/requestUserInput".equals(pendingPermission.method())) {
            final ObjectNode result = objectMapper.createObjectNode();
            result.set("answers", objectMapper.createObjectNode());
            return result;
        }
        final ObjectNode result = objectMapper.createObjectNode();
        final String decision = switch (action) {
            case APPROVE_ONCE -> "accept";
            case APPROVE_ALWAYS -> "acceptForSession";
            case DENY -> "decline";
        };
        result.put("decision", decision);
        return result;
    }

    private void removePendingFromStream(final PendingPermission pendingPermission) {
        final ActiveStream stream = activeStreams.get(pendingPermission.chatThreadId());
        if (stream != null) {
            stream.pendingPermissionRequestIds.remove(pendingPermission.requestId());
        }
    }

    private void handleThreadStarted(final JsonNode params) {
        final JsonNode thread = params.path("thread");
        final String codexThreadId = thread.path("id").asText(null);
        if (codexThreadId == null || codexThreadId.isBlank()) {
            return;
        }
        codexThreadToChatThread.putIfAbsent(codexThreadId, codexThreadId);
    }

    private void handleThreadNameUpdated(final JsonNode params) {
        final String codexThreadId = eventThreadId(params);
        final String chatThreadId = chatThreadIdForCodexThread(codexThreadId);
        final String name = params.path("name").asText(params.path("thread").path("name").asText(null));
        if (chatThreadId != null && name != null && !name.isBlank()) {
            Event.publish(new ChatThreadTitleUpdatedEvent(chatThreadId, name));
        }
    }

    private void handleTurnStarted(final JsonNode params) {
        final JsonNode turn = params.path("turn");
        final String codexThreadId = eventThreadId(params);
        final ActiveStream stream = streamForCodexThread(codexThreadId);
        if (stream == null) {
            return;
        }
        final String turnId = turn.path("id").asText(null);
        if (turnId != null && !turnId.isBlank()) {
            stream.turnId = turnId;
        }
    }

    private void handleTurnCompleted(final JsonNode params) {
        final String codexThreadId = eventThreadId(params);
        final ActiveStream stream = streamForCodexThread(codexThreadId);
        if (stream == null || stream.cancelled.get()) {
            return;
        }
        final JsonNode turn = params.path("turn");
        final String status = turn.path("status").asText("");
        if ("failed".equalsIgnoreCase(status)) {
            final String message = turn.path("error").path("message").asText("Codex turn failed");
            stream.sink.error(new IllegalStateException(message));
        } else {
            stream.sink.complete();
        }
        stream.completed.countDown();
    }

    private void handlePlanUpdated(final JsonNode params) {
        final ActiveStream stream = streamForCodexThread(eventThreadId(params));
        if (stream == null) {
            return;
        }
        final JsonNode plan = params.path("plan");
        if (!plan.isArray()) {
            return;
        }
        final List<TodoItem> todos = new ArrayList<>();
        for (final JsonNode item : plan) {
            final String step = item.path("step").asText("");
            if (step.isBlank()) {
                continue;
            }
            todos.add(new TodoItem(step, normalizePlanStatus(item.path("status").asText("")), "medium"));
        }
        if (!todos.isEmpty()) {
            Event.publish(ChatMessageStreamEvent.todoUpdate(stream.chatThreadId, todos));
        }
    }

    private void handleTokenUsageUpdated(final JsonNode params) {
        final ActiveStream stream = streamForCodexThread(eventThreadId(params));
        if (stream == null) {
            return;
        }
        final JsonNode usage = params.path("usage").isMissingNode() ? params : params.path("usage");
        long total = usage.path("total").asLong(0);
        if (total == 0) {
            total = usage.path("input").asLong(0)
                    + usage.path("output").asLong(0)
                    + usage.path("reasoning").asLong(0)
                    + usage.path("cacheRead").asLong(0)
                    + usage.path("cacheWrite").asLong(0);
        }
        if (total > 0) {
            stream.tokensTotal = total;
        }
    }

    private void handleItemStarted(final JsonNode params) {
        handleItemLifecycle(params, false);
    }

    private void handleItemCompleted(final JsonNode params) {
        handleItemLifecycle(params, true);
    }

    private void handleItemLifecycle(final JsonNode params, final boolean completed) {
        final JsonNode item = params.path("item");
        final ActiveStream stream = streamForItemEvent(params, item);
        if (stream == null) {
            return;
        }
        final String itemId = item.path("id").asText(params.path("itemId").asText(""));
        if (!itemId.isBlank()) {
            itemToChatThread.put(itemId, stream.chatThreadId);
        }
        final String type = item.path("type").asText("");
        if ("agentMessage".equals(type)) {
            final String phase = item.path("phase").asText("");
            if (!itemId.isBlank() && !phase.isBlank()) {
                stream.itemPhases.put(itemId, phase);
            }
            if (completed) {
                publishCompletedAgentMessage(stream, itemId, item.path("text").asText(""), phase);
            }
            return;
        }
        if ("reasoning".equals(type)) {
            final String summary = joinedText(item.path("summary"));
            final String content = joinedText(item.path("content"));
            publishReasoningIfPresent(stream, itemId, !summary.isBlank() ? summary : content);
            return;
        }
        publishActivityForItem(stream, item, completed);
    }

    private void handleAgentMessageDelta(final JsonNode params) {
        final ActiveStream stream = streamForDelta(params);
        if (stream == null || stream.cancelled.get()) {
            return;
        }
        final String itemId = itemId(params);
        final String delta = params.path("delta").asText(params.path("text").asText(""));
        if (delta.isEmpty()) {
            return;
        }
        final String phase = stream.itemPhases.getOrDefault(itemId, "");
        if ("commentary".equalsIgnoreCase(phase)) {
            final String updated = stream.commentaryParts.getOrDefault(itemId, "") + delta;
            stream.commentaryParts.put(itemId, updated);
            Event.publish(ChatMessageStreamEvent.commentary(stream.chatThreadId, updated));
            return;
        }
        stream.messageParts.merge(itemId, delta, String::concat);
        stream.sink.next(delta);
    }

    private void handlePlanDelta(final JsonNode params) {
        final ActiveStream stream = streamForDelta(params);
        if (stream == null) {
            return;
        }
        final String itemId = itemId(params);
        final String delta = params.path("delta").asText(params.path("text").asText(""));
        if (delta.isBlank()) {
            return;
        }
        final String updated = stream.commentaryParts.getOrDefault(itemId, "") + delta;
        stream.commentaryParts.put(itemId, updated);
        Event.publish(ChatMessageStreamEvent.commentary(stream.chatThreadId, updated));
    }

    private void handleReasoningDelta(final JsonNode params) {
        final ActiveStream stream = streamForDelta(params);
        if (stream == null) {
            return;
        }
        final String itemId = itemId(params);
        final String delta = params.path("delta").asText(params.path("text").asText(""));
        if (delta.isBlank()) {
            return;
        }
        final String updated = stream.reasoningParts.getOrDefault(itemId, "") + delta;
        stream.reasoningParts.put(itemId, updated);
        Event.publish(new ChatMessageStreamEvent(stream.chatThreadId, updated, true));
    }

    private void handleReasoningSummaryPartAdded(final JsonNode params) {
        final ActiveStream stream = streamForDelta(params);
        if (stream == null) {
            return;
        }
        final String itemId = itemId(params);
        final String summary = params.path("summary").asText(params.path("text").asText(""));
        publishReasoningIfPresent(stream, itemId, summary);
    }

    private void publishCompletedAgentMessage(final ActiveStream stream,
                                              final String itemId,
                                              final String currentText,
                                              final String phase) {
        if (currentText == null || currentText.isBlank()) {
            return;
        }
        if ("commentary".equalsIgnoreCase(phase)) {
            final String previous = stream.commentaryParts.getOrDefault(itemId, "");
            stream.commentaryParts.put(itemId, currentText);
            if (!currentText.equals(previous)) {
                Event.publish(ChatMessageStreamEvent.commentary(stream.chatThreadId, currentText));
            }
            return;
        }
        final String previous = stream.messageParts.getOrDefault(itemId, "");
        stream.messageParts.put(itemId, currentText);
        final String delta = currentText.startsWith(previous) ? currentText.substring(previous.length()) : currentText;
        if (!delta.isBlank()) {
            stream.sink.next(delta);
        }
    }

    private void handleCommandOutputDelta(final JsonNode params) {
        final ActiveStream stream = streamForDelta(params);
        if (stream == null) {
            return;
        }
        final String itemId = itemId(params);
        final String streamName = params.path("stream").asText("");
        final String dedupeKey = itemId + ":output:" + streamName;
        if (stream.seenActivity.add(dedupeKey)) {
            Event.publish(new ChatMessageStreamEvent(
                    stream.chatThreadId,
                    ChatMessageStreamEvent.ActivityType.TOOL,
                    "command",
                    streamName
            ));
        }
    }

    private void handleServerRequestResolved(final JsonNode params) {
        final String requestId = params.path("requestId").asText(null);
        if (requestId == null || requestId.isBlank()) {
            return;
        }
        final PendingPermission pendingPermission = pendingPermissions.remove(requestId);
        if (pendingPermission != null) {
            removePendingFromStream(pendingPermission);
        }
    }

    private void handleError(final JsonNode params) {
        final String codexThreadId = eventThreadId(params);
        final ActiveStream stream = streamForCodexThread(codexThreadId);
        if (stream == null) {
            return;
        }
        final String message = params.path("error").path("message").asText("Codex app-server error");
        stream.sink.error(new IllegalStateException(message));
        stream.completed.countDown();
    }

    private void publishActivityForItem(final ActiveStream stream, final JsonNode item, final boolean completed) {
        final String type = item.path("type").asText("");
        final Activity activity = switch (type) {
            case "commandExecution" -> new Activity(ChatMessageStreamEvent.ActivityType.TOOL,
                    commandTitle(item), item.path("status").asText(completed ? "completed" : "running"));
            case "fileChange" -> new Activity(ChatMessageStreamEvent.ActivityType.TOOL,
                    "file changes", item.path("status").asText(completed ? "completed" : "running"));
            case "mcpToolCall" -> new Activity(ChatMessageStreamEvent.ActivityType.MCP,
                    item.path("tool").asText("MCP tool"), item.path("server").asText(""));
            case "dynamicToolCall" -> new Activity(ChatMessageStreamEvent.ActivityType.TOOL,
                    item.path("tool").asText("dynamic tool"), item.path("status").asText(""));
            case "collabToolCall" -> new Activity(ChatMessageStreamEvent.ActivityType.TOOL,
                    item.path("tool").asText("collaboration"), item.path("status").asText(""));
            case "webSearch" -> new Activity(ChatMessageStreamEvent.ActivityType.TOOL,
                    "web search", item.path("query").asText(""));
            case "imageView" -> new Activity(ChatMessageStreamEvent.ActivityType.TOOL,
                    "image view", item.path("path").asText(""));
            case "contextCompaction" -> new Activity(ChatMessageStreamEvent.ActivityType.TOOL,
                    "context compaction", completed ? "completed" : "running");
            case "enteredReviewMode", "exitedReviewMode" -> new Activity(ChatMessageStreamEvent.ActivityType.TOOL,
                    "review", completed ? "completed" : "running");
            default -> null;
        };
        if (activity == null) {
            return;
        }
        final String itemId = item.path("id").asText("");
        final String dedupeKey = itemId + ":" + activity.name() + ":" + activity.detail() + ":" + completed;
        if (stream.seenActivity.add(dedupeKey)) {
            Event.publish(new ChatMessageStreamEvent(stream.chatThreadId, activity.type(), activity.name(), activity.detail()));
        }
    }

    private void publishReasoningIfPresent(final ActiveStream stream, final String itemId, final String summary) {
        if (summary == null || summary.isBlank()) {
            return;
        }
        stream.reasoningParts.put(itemId, summary);
        Event.publish(new ChatMessageStreamEvent(stream.chatThreadId, summary, true));
    }

    private String joinedText(final JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText("");
        }
        if (!node.isArray()) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        for (final JsonNode item : node) {
            final String text = item.asText("");
            if (!text.isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append(System.lineSeparator());
                }
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private void indexItems(final String chatThreadId, final JsonNode items) {
        if (!items.isArray()) {
            return;
        }
        for (final JsonNode item : items) {
            final String id = item.path("id").asText(null);
            if (id != null && !id.isBlank()) {
                itemToChatThread.put(id, chatThreadId);
            }
        }
    }

    private ActiveStream streamForItemEvent(final JsonNode params, final JsonNode item) {
        final String codexThreadId = eventThreadId(params);
        ActiveStream stream = streamForCodexThread(codexThreadId);
        if (stream != null) {
            return stream;
        }
        final String itemId = item.path("id").asText(params.path("itemId").asText(null));
        final String chatThreadId = itemToChatThread.get(itemId);
        return chatThreadId == null ? null : activeStreams.get(chatThreadId);
    }

    private ActiveStream streamForDelta(final JsonNode params) {
        final String codexThreadId = eventThreadId(params);
        ActiveStream stream = streamForCodexThread(codexThreadId);
        if (stream != null) {
            return stream;
        }
        final String chatThreadId = itemToChatThread.get(itemId(params));
        return chatThreadId == null ? null : activeStreams.get(chatThreadId);
    }

    private ActiveStream streamForCodexThread(final String codexThreadId) {
        final String chatThreadId = chatThreadIdForCodexThread(codexThreadId);
        return chatThreadId == null ? null : activeStreams.get(chatThreadId);
    }

    private String chatThreadIdForCodexThread(final String codexThreadId) {
        if (codexThreadId == null || codexThreadId.isBlank()) {
            return null;
        }
        final String mapped = codexThreadToChatThread.get(codexThreadId);
        return mapped != null ? mapped : codexThreadId;
    }

    private String resolveCodexThreadId(final String chatThreadId) {
        return chatThreadToCodexThread.getOrDefault(chatThreadId, chatThreadId);
    }

    private String eventThreadId(final JsonNode params) {
        final String direct = params.path("threadId").asText(null);
        if (direct != null && !direct.isBlank()) {
            return direct;
        }
        final String turnThread = params.path("turn").path("threadId").asText(null);
        if (turnThread != null && !turnThread.isBlank()) {
            return turnThread;
        }
        final String itemThread = params.path("item").path("threadId").asText(null);
        if (itemThread != null && !itemThread.isBlank()) {
            return itemThread;
        }
        return params.path("thread").path("id").asText(null);
    }

    private String itemId(final JsonNode params) {
        final String itemId = params.path("itemId").asText(null);
        if (itemId != null && !itemId.isBlank()) {
            return itemId;
        }
        final String id = params.path("id").asText(null);
        if (id != null && !id.isBlank()) {
            return id;
        }
        return params.path("item").path("id").asText("");
    }

    private ChatMessageStreamEvent.PermissionType permissionType(final String method, final JsonNode params) {
        if (method.contains("commandExecution")) {
            return ChatMessageStreamEvent.PermissionType.BASH;
        }
        if (method.contains("mcpServer") || method.contains("requestUserInput")) {
            return ChatMessageStreamEvent.PermissionType.MCP;
        }
        return ChatMessageStreamEvent.PermissionType.TOOL;
    }

    private String permissionValue(final String method, final JsonNode params) {
        if (method.contains("commandExecution")) {
            return commandFromParams(params);
        }
        if (method.contains("fileChange")) {
            return "file changes";
        }
        if (method.contains("requestUserInput")) {
            return "tool input";
        }
        if (method.contains("mcpServer")) {
            return params.path("serverName").asText("MCP");
        }
        return "permissions";
    }

    private String permissionTitle(final String method, final JsonNode params) {
        return "Codex approval required: " + permissionValue(method, params);
    }

    private String permissionMessage(final String method, final JsonNode params) {
        final String reason = params.path("reason").asText("");
        final String cwd = params.path("cwd").asText("");
        final StringBuilder builder = new StringBuilder("Codex requires approval.");
        if (!reason.isBlank()) {
            builder.append(System.lineSeparator()).append("Reason: ").append(reason);
        }
        if (!cwd.isBlank()) {
            builder.append(System.lineSeparator()).append("Working directory: ").append(cwd);
        }
        if (method.contains("commandExecution")) {
            builder.append(System.lineSeparator()).append("Command: ").append(commandFromParams(params));
        }
        if (method.contains("permissions")) {
            builder.append(System.lineSeparator()).append("Permissions: ").append(params.path("permissions"));
        }
        if (method.contains("mcpServer")) {
            builder.append(System.lineSeparator()).append(params.path("message").asText(""));
        }
        return builder.toString();
    }

    private String permissionMatchedRule(final String method, final JsonNode params) {
        if (method.contains("commandExecution")) {
            final JsonNode amendment = params.path("proposedExecpolicyAmendment");
            return amendment.isMissingNode() || amendment.isNull() ? "" : amendment.toString();
        }
        if (method.contains("fileChange")) {
            return params.path("grantRoot").asText("");
        }
        return "";
    }

    private String resolvePermissionConfigPath() {
        return Path.of(System.getProperty("user.home", "."), ".codex", "config.toml")
                .toAbsolutePath()
                .normalize()
                .toString();
    }

    private String commandTitle(final JsonNode item) {
        final String command = commandFromParams(item);
        return command.isBlank() ? "command" : command;
    }

    private String commandFromParams(final JsonNode params) {
        final JsonNode command = params.path("command");
        if (command.isArray()) {
            final List<String> parts = new ArrayList<>();
            command.forEach(part -> parts.add(part.asText("")));
            return String.join(" ", parts).trim();
        }
        return command.asText("");
    }

    private void collectMessages(final JsonNode turns, final String codexThreadId, final List<ChatMessageResponseModel> messages) {
        if (!turns.isArray()) {
            return;
        }
        for (final JsonNode turn : turns) {
            final JsonNode items = turn.path("items");
            if (!items.isArray()) {
                continue;
            }
            for (final JsonNode item : items) {
                toMessage(item, codexThreadId).ifPresent(messages::add);
            }
        }
    }

    private Optional<ChatMessageResponseModel> toMessage(final JsonNode item, final String codexThreadId) {
        final String type = item.path("type").asText("");
        final ChatMessageRole role = switch (type) {
            case "userMessage" -> ChatMessageRole.USER;
            case "agentMessage" -> ChatMessageRole.ASSISTANT;
            default -> null;
        };
        if (role == null) {
            return Optional.empty();
        }
        final String text = role == ChatMessageRole.USER ? userMessageText(item.path("content")) : item.path("text").asText("");
        if (text.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new ChatMessageResponseModel(
                text,
                role,
                parseTime(item),
                ChatMessageStyleType.NONE,
                codexThreadId,
                null
        ));
    }

    private String userMessageText(final JsonNode content) {
        if (!content.isArray()) {
            return "";
        }
        final StringBuilder builder = new StringBuilder();
        for (final JsonNode item : content) {
            if ("text".equals(item.path("type").asText())) {
                final String text = item.path("text").asText("");
                if (!text.isBlank()) {
                    builder.append(text);
                }
            }
        }
        return builder.toString();
    }

    private Optional<ChatThread> toChatThread(final JsonNode node) {
        final String id = node.path("id").asText(null);
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        final String name = node.path("name").asText(node.path("title").asText(id));
        final LocalDateTime createdAt = parseTime(node.path("createdAt"), node.path("created_at"), node.path("created"));
        final LocalDateTime updatedAt = parseTime(node.path("updatedAt"), node.path("updated_at"), node.path("updated"));
        codexThreadToChatThread.putIfAbsent(id, id);
        return Optional.of(new ChatThread(id, name, createdAt, updatedAt));
    }

    private LocalDateTime parseTime(final JsonNode node) {
        return parseTime(node.path("createdAt"), node.path("created_at"), node.path("created"), node.path("time").path("created"));
    }

    private LocalDateTime parseTime(final JsonNode... nodes) {
        for (final JsonNode node : nodes) {
            if (node == null || node.isMissingNode() || node.isNull()) {
                continue;
            }
            if (node.isNumber()) {
                final long raw = node.asLong();
                final long epochMillis = raw > 10_000_000_000L ? raw : raw * 1000;
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
            }
            if (node.isTextual() && !node.asText().isBlank()) {
                try {
                    return LocalDateTime.ofInstant(Instant.parse(node.asText()), ZoneId.systemDefault());
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private String resolveCodexModel(final String modelOverride) {
        final String override = normalizeCodexModel(modelOverride);
        if (override != null && !override.isBlank()) {
            return resolveSupportedCodexModel(override);
        }
        return resolveEffectiveModelIdentifier();
    }

    private String resolveSupportedCodexModel(final String requestedModel) {
        final String normalized = normalizeCodexModel(requestedModel);
        final List<String> models = availableModelsForSelection();
        if (normalized != null && models.contains(normalized)) {
            return normalized;
        }
        if (!models.isEmpty()) {
            return models.getFirst();
        }
        return normalized;
    }

    private List<String> availableModelsForSelection() {
        if (!cachedModels.isEmpty()) {
            return cachedModels;
        }
        return getAvailableModels();
    }

    private String configuredCodexModel() {
        return normalizeCodexModel(assistantSettingsManager.getSettings().chatModel());
    }

    private String normalizeCodexModel(final String model) {
        if (model == null || model.isBlank()) {
            return null;
        }
        final String trimmed = model.trim();
        final int slash = trimmed.indexOf('/');
        return slash >= 0 ? trimmed.substring(slash + 1).trim() : trimmed;
    }

    private boolean isCodexThreadId(final String threadId) {
        return threadId != null && (threadId.startsWith("thr_") || threadId.startsWith("thread_"));
    }

    private Path resolveWorkingDirectory(final WorkspaceSettings settings) {
        return Path.of(settings.codexWorkingDirectory()).toAbsolutePath().normalize();
    }

    private String normalizePlanStatus(final String status) {
        if ("inProgress".equalsIgnoreCase(status) || "in_progress".equalsIgnoreCase(status)) {
            return "in_progress";
        }
        if ("completed".equalsIgnoreCase(status)) {
            return "completed";
        }
        return "pending";
    }

    private record Activity(ChatMessageStreamEvent.ActivityType type, String name, String detail) {
    }

    private record PendingPermission(String requestId,
                                     long rpcId,
                                     String method,
                                     String chatThreadId,
                                     String codexThreadId,
                                     JsonNode params) {
    }

    private static final class ActiveStream {
        private final String chatThreadId;
        private final FluxSink<String> sink;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final CountDownLatch completed = new CountDownLatch(1);
        private final Set<String> seenActivity = ConcurrentHashMap.newKeySet();
        private final Set<String> pendingPermissionRequestIds = ConcurrentHashMap.newKeySet();
        private final Map<String, String> itemPhases = new ConcurrentHashMap<>();
        private final Map<String, String> messageParts = new ConcurrentHashMap<>();
        private final Map<String, String> reasoningParts = new ConcurrentHashMap<>();
        private final Map<String, String> commentaryParts = new ConcurrentHashMap<>();
        private volatile String codexThreadId;
        private volatile String turnId;
        private volatile String modelUsed;
        private volatile long tokensTotal;

        private ActiveStream(final String chatThreadId, final FluxSink<String> sink) {
            this.chatThreadId = chatThreadId;
            this.sink = sink;
        }
    }
}
