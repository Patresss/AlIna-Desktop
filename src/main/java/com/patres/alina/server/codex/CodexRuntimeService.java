package com.patres.alina.server.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patres.alina.common.ai.AiProvider;
import com.patres.alina.common.ai.AiRuntimeStatus;
import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.Event;
import com.patres.alina.common.message.ImageAttachment;
import com.patres.alina.common.message.TodoItem;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.server.ai.AiRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class CodexRuntimeService implements AiRuntime {

    private static final Logger logger = LoggerFactory.getLogger(CodexRuntimeService.class);
    private static final int CACHE_TIME_SEC = 60 * 60 * 24;
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(10);

    private final CodexConfigurationService configurationService;
    private final CodexSessionService sessionService;
    private final ObjectMapper objectMapper;
    private final Map<String, ActiveStream> activeStreams = new ConcurrentHashMap<>();

    private volatile List<String> cachedModels = List.of();
    private volatile Instant cachedModelsAt = Instant.EPOCH;

    public CodexRuntimeService(final CodexConfigurationService configurationService,
                               final CodexSessionService sessionService,
                               final ObjectMapper objectMapper) {
        this.configurationService = configurationService;
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiProvider provider() {
        return AiProvider.CODEX;
    }

    @Override
    public Flux<String> sendMessageStream(final String chatThreadId,
                                          final String chatThreadTitle,
                                          final String userMessage,
                                          final String systemPrompt,
                                          final String historySummary,
                                          final String modelOverride,
                                          final boolean forceNewSession,
                                          final List<ImageAttachment> imageAttachments) {
        return Flux.create(sink -> {
            final ActiveStream stream = new ActiveStream(chatThreadId);
            activeStreams.put(chatThreadId, stream);
            sink.onDispose(() -> closeStream(stream));

            Thread.startVirtualThread(() -> {
                try {
                    sessionService.ensureConversation(chatThreadId, chatThreadTitle, userMessage);
                    if (forceNewSession) {
                        sessionService.clearCodexSessionId(chatThreadId);
                    }
                    sessionService.addUserMessage(chatThreadId, userMessage);

                    final String existingSessionId = forceNewSession ? null : sessionService.resolveSessionId(chatThreadId);
                    stream.sessionId = existingSessionId;
                    stream.modelUsed = resolveModel(modelOverride);
                    stream.outputFile = Files.createTempFile("alina-codex-response-", ".txt");
                    stream.imageFiles.addAll(writeImageAttachments(imageAttachments));

                    final List<String> command = buildCodexCommand(existingSessionId, stream.modelUsed, stream.outputFile, stream.imageFiles);
                    final ProcessBuilder processBuilder = new ProcessBuilder(command);
                    processBuilder.directory(configurationService.resolveWorkingDirectory().toFile());
                    processBuilder.environment().putAll(configurationService.buildProcessEnvironment());
                    processBuilder.redirectErrorStream(true);

                    final Process process = processBuilder.start();
                    stream.process = process;
                    writePrompt(process, composePrompt(userMessage, systemPrompt, historySummary));
                    consumeOutput(stream, process, sink);
                } catch (Exception e) {
                    if (!stream.cancelled.get()) {
                        sink.error(e);
                    }
                } finally {
                    activeStreams.remove(chatThreadId, stream);
                    closeStream(stream);
                    deleteTempFiles(stream);
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
        closeStream(stream);
    }

    @Override
    public List<String> getAvailableModels() {
        if (Instant.now().isBefore(cachedModelsAt.plusSeconds(CACHE_TIME_SEC)) && !cachedModels.isEmpty()) {
            return cachedModels;
        }
        final TreeSet<String> models = new TreeSet<>();
        try {
            final CommandResult result = runCommand(List.of(configurationService.command(), "debug", "models", "--bundled"), COMMAND_TIMEOUT);
            if (result.exitCode() == 0) {
                final String json = firstJsonObject(result.output());
                if (json != null) {
                    final JsonNode root = objectMapper.readTree(json);
                    final JsonNode modelNodes = root.path("models");
                    if (modelNodes.isArray()) {
                        for (final JsonNode node : modelNodes) {
                            final String slug = node.path("slug").asText("");
                            if (!slug.isBlank()) {
                                models.add(slug);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Cannot fetch available models from Codex", e);
        }

        models.add(configurationService.resolveEffectiveModelIdentifier());
        if (models.isEmpty()) {
            models.addAll(List.of("gpt-5.5", "gpt-5-codex", "gpt-5", "gpt-5-mini"));
        }
        cachedModels = List.copyOf(models);
        cachedModelsAt = Instant.now();
        return cachedModels;
    }

    @Override
    public AiRuntimeStatus getRuntimeStatus() {
        final WorkspaceSettings workspace = configurationService.workspaceSettings();
        final Path workingDirectory = configurationService.resolveWorkingDirectory();
        boolean healthy = false;
        String version = null;
        String message;
        try {
            final CommandResult result = runCommand(List.of(configurationService.command(), "--version"), COMMAND_TIMEOUT);
            healthy = result.exitCode() == 0;
            version = lastNonBlankLine(result.output());
            message = healthy ? "Codex CLI is available." : result.output();
        } catch (Exception e) {
            message = e.getMessage();
        }

        return new AiRuntimeStatus(
                AiProvider.CODEX,
                "Codex",
                workspace.codexCommand(),
                null,
                0,
                null,
                workingDirectory.toString(),
                Files.isDirectory(workingDirectory),
                false,
                healthy,
                version,
                message
        );
    }

    @Override
    public void prepareForFreshChat() {
        activeStreams.keySet().forEach(this::cancelStreaming);
    }

    @Override
    public String getModelUsedForThread(final String threadId) {
        final ActiveStream stream = activeStreams.get(threadId);
        return stream != null ? stream.modelUsed : null;
    }

    @Override
    public String getAgentUsedForThread(final String threadId) {
        return "codex";
    }

    @Override
    public long getTokensTotalForThread(final String threadId) {
        final ActiveStream stream = activeStreams.get(threadId);
        return stream != null ? stream.tokensTotal : 0;
    }

    private List<String> buildCodexCommand(final String existingSessionId,
                                           final String model,
                                           final Path outputFile,
                                           final List<Path> imageFiles) {
        final List<String> command = new ArrayList<>();
        command.add(configurationService.command());
        command.add("exec");

        if (existingSessionId != null && !existingSessionId.isBlank()) {
            command.add("resume");
        }

        command.add("--json");
        command.add("--skip-git-repo-check");
        command.add("-c");
        command.add("approval_policy=\"never\"");
        if (existingSessionId == null || existingSessionId.isBlank()) {
            command.add("-s");
            command.add(configurationService.sandbox());
        }
        if (model != null && !model.isBlank()) {
            command.add("-m");
            command.add(model);
        }
        for (final Path imageFile : imageFiles) {
            command.add("-i");
            command.add(imageFile.toString());
        }
        command.add("-o");
        command.add(outputFile.toString());
        if (existingSessionId != null && !existingSessionId.isBlank()) {
            command.add(existingSessionId);
        }
        command.add("-");
        return command;
    }

    private String resolveModel(final String modelOverride) {
        if (modelOverride != null && !modelOverride.isBlank()) {
            return configurationService.toCodexModel(modelOverride);
        }
        return configurationService.resolveEffectiveModelIdentifier();
    }

    private String composePrompt(final String userMessage,
                                 final String systemPrompt,
                                 final String historySummary) {
        final List<String> sections = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            sections.add("System instructions:\n" + systemPrompt.trim());
        }
        if (historySummary != null && !historySummary.isBlank()) {
            sections.add("Conversation summary:\n" + historySummary.trim());
        }
        sections.add(userMessage == null ? "" : userMessage);
        return String.join(System.lineSeparator() + System.lineSeparator(), sections);
    }

    private void writePrompt(final Process process, final String prompt) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
            writer.write(prompt);
        }
    }

    private void consumeOutput(final ActiveStream stream,
                               final Process process,
                               final FluxSink<String> sink) throws Exception {
        final StringBuilder rawOutput = new StringBuilder();
        try (BufferedReader reader = process.inputReader(StandardCharsets.UTF_8)) {
            String line;
            while (!stream.cancelled.get() && (line = reader.readLine()) != null) {
                rawOutput.append(line).append(System.lineSeparator());
                processOutputLine(stream, line, sink);
            }
        }

        final int exitCode = process.waitFor();
        if (stream.cancelled.get()) {
            return;
        }

        emitOutputFileMessage(stream, sink);
        if (stream.errorMessage != null && stream.response.isEmpty()) {
            sessionService.clearCodexSessionId(stream.threadId);
            throw new IllegalStateException("Codex failed: " + stream.errorMessage);
        }
        if (exitCode != 0 && stream.response.isEmpty()) {
            sessionService.clearCodexSessionId(stream.threadId);
            throw new IllegalStateException("Codex failed: " + summarize(rawOutput.toString()));
        }
        if (exitCode != 0) {
            logger.warn("Codex exited with code {} after streaming a response: {}", exitCode, summarize(rawOutput.toString()));
        }
        if (!stream.response.isEmpty()) {
            sessionService.addAssistantMessage(stream.threadId, stream.response.toString());
        }
        sink.complete();
    }

    private void processOutputLine(final ActiveStream stream,
                                   final String line,
                                   final FluxSink<String> sink) {
        final String trimmed = line == null ? "" : line.trim();
        if (trimmed.isBlank() || !trimmed.startsWith("{")) {
            return;
        }
        try {
            final JsonNode event = objectMapper.readTree(trimmed);
            handleEvent(stream, event, sink);
        } catch (Exception e) {
            logger.debug("Cannot parse Codex JSONL event: {}", trimmed, e);
        }
    }

    private void handleEvent(final ActiveStream stream,
                             final JsonNode event,
                             final FluxSink<String> sink) {
        final JsonNode msg = event.path("msg").isObject() ? event.path("msg") : event;
        final String type = text(msg, "type", text(event, "type", ""));
        if (isErrorEvent(type)) {
            stream.errorMessage = firstNonBlank(extractErrorSummary(event), "Codex stream error");
            return;
        }
        switch (type) {
            case "session_configured", "thread.started" -> handleSessionConfigured(stream, msg);
            case "agent_message_content_delta" -> emitDelta(stream, text(msg, "delta", ""), sink);
            case "agent_message" -> emitFinalMessage(stream, extractText(msg), sink);
            case "task_complete", "turn_complete", "turn.completed" -> {
                parseTokenUsage(stream, msg);
                emitFinalMessage(stream, text(msg, "last_agent_message", ""), sink);
            }
            case "token_count" -> parseTokenUsage(stream, msg);
            case "agent_reasoning", "reasoning_content_delta", "reasoning_raw_content_delta" -> {
                final String content = firstNonBlank(
                        text(msg, "delta", ""),
                        text(msg, "text", ""),
                        text(msg, "reasoning_text", "")
                );
                if (!content.isBlank()) {
                    Event.publish(new ChatMessageStreamEvent(stream.threadId, content, true));
                }
            }
            case "exec_command_begin" -> publishCommandActivity(stream, msg);
            case "mcp_tool_call_begin" -> publishMcpActivity(stream, msg);
            case "patch_apply_begin", "apply_patch_approval_request" -> publishActivity(
                    stream.threadId,
                    ChatMessageStreamEvent.ActivityType.TOOL,
                    "apply_patch",
                    "Codex"
            );
            case "item.started", "item_started", "item.updated", "item_updated" -> {
                publishItemActivity(stream, msg);
                publishTodoList(stream, msg);
            }
            case "stream_error", "error" -> stream.errorMessage = firstNonBlank(
                    extractErrorSummary(event),
                    "Codex stream error"
            );
            case "item.completed", "item_completed" -> {
                publishItemActivity(stream, msg);
                publishTodoList(stream, msg);
                emitFinalMessage(stream, extractText(msg.path("item")), sink);
            }
            default -> {
            }
        }
    }

    private void handleSessionConfigured(final ActiveStream stream, final JsonNode msg) {
        final String sessionId = firstNonBlank(
                text(msg, "session_id", ""),
                text(msg, "thread_id", ""),
                text(msg, "conversation_id", "")
        );
        if (!sessionId.isBlank()) {
            stream.sessionId = sessionId;
            sessionService.setCodexSessionId(stream.threadId, sessionId);
        }
    }

    private void emitDelta(final ActiveStream stream,
                           final String delta,
                           final FluxSink<String> sink) {
        if (delta == null || delta.isEmpty()) {
            return;
        }
        stream.response.append(delta);
        sink.next(delta);
    }

    private void emitFinalMessage(final ActiveStream stream,
                                  final String finalMessage,
                                  final FluxSink<String> sink) {
        if (finalMessage == null || finalMessage.isBlank()) {
            return;
        }
        final String normalized = finalMessage.trim();
        final String current = stream.response.toString();
        if (normalized.equals(current.trim())) {
            return;
        }
        if (!stream.seenAssistantMessages.add(normalized)) {
            return;
        }
        if (!current.isBlank() && finalMessage.startsWith(current)) {
            emitDelta(stream, finalMessage.substring(current.length()), sink);
            return;
        }
        if (current.isBlank()) {
            emitDelta(stream, finalMessage, sink);
            return;
        }
        emitDelta(stream, System.lineSeparator() + System.lineSeparator() + finalMessage, sink);
    }

    private void emitOutputFileMessage(final ActiveStream stream,
                                       final FluxSink<String> sink) {
        if (stream.outputFile == null || !Files.exists(stream.outputFile)) {
            return;
        }
        try {
            final String finalMessage = Files.readString(stream.outputFile, StandardCharsets.UTF_8).trim();
            emitFinalMessage(stream, finalMessage, sink);
        } catch (IOException e) {
            logger.debug("Cannot read Codex output file {}", stream.outputFile, e);
        }
    }

    private String extractText(final JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        final String direct = firstNonBlank(
                text(node, "message", ""),
                text(node, "text", ""),
                text(node, "content", ""),
                text(node, "last_agent_message", "")
        );
        if (!direct.isBlank()) {
            return direct;
        }
        final JsonNode content = node.path("content");
        if (content.isArray()) {
            return collectText(content);
        }
        final JsonNode item = node.path("item");
        if (item.isObject()) {
            return extractText(item);
        }
        return "";
    }

    private String collectText(final JsonNode array) {
        final StringBuilder builder = new StringBuilder();
        for (final JsonNode item : array) {
            final String text = firstNonBlank(
                    text(item, "text", ""),
                    text(item, "output_text", ""),
                    text(item, "message", "")
            );
            if (!text.isBlank()) {
                builder.append(text);
            }
        }
        return builder.toString();
    }

    private void parseTokenUsage(final ActiveStream stream, final JsonNode msg) {
        final List<JsonNode> candidates = List.of(
                msg.path("total_token_usage"),
                msg.path("token_usage"),
                msg.path("usage"),
                msg
        );
        for (final JsonNode candidate : candidates) {
            final long total = firstPositive(
                    candidate.path("total_tokens").asLong(0),
                    candidate.path("total").asLong(0)
            );
            if (total > 0) {
                stream.tokensTotal = total;
                return;
            }
            final long input = candidate.path("input_tokens").asLong(0);
            final long output = candidate.path("output_tokens").asLong(0);
            final long reasoning = candidate.path("reasoning_output_tokens").asLong(0);
            final long summed = input + output + reasoning;
            if (summed > 0) {
                stream.tokensTotal = summed;
                return;
            }
        }
    }

    private String describeCommand(final JsonNode msg) {
        return firstNonBlank(
                commandText(msg.path("parsed_cmd")),
                commandText(msg.path("cmd")),
                commandText(msg.path("command")),
                commandText(msg.path("shell_command")),
                commandText(msg.path("display_command")),
                commandText(msg.path("interaction_input")),
                commandText(msg.path("context").path("parsed_cmd")),
                commandText(msg.path("context").path("cmd")),
                commandText(msg.path("context").path("command")),
                text(msg, "program", ""),
                "command"
        );
    }

    private void publishCommandActivity(final ActiveStream stream, final JsonNode msg) {
        publishActivity(
                stream.threadId,
                ChatMessageStreamEvent.ActivityType.TOOL,
                describeCommand(msg),
                "Codex terminal"
        );
    }

    private void publishMcpActivity(final ActiveStream stream, final JsonNode msg) {
        final JsonNode invocation = msg.path("invocation");
        final String server = firstNonBlank(
                text(msg, "server_name", ""),
                text(msg, "server", ""),
                text(invocation, "server", ""),
                text(invocation, "connector_name", ""),
                "MCP"
        );
        final String tool = firstNonBlank(
                text(msg, "tool_title", ""),
                text(msg, "tool_name", ""),
                text(invocation, "tool_title", ""),
                text(invocation, "tool_name", ""),
                text(invocation, "name", ""),
                "tool"
        );
        publishActivity(stream.threadId, ChatMessageStreamEvent.ActivityType.MCP, tool, server);
    }

    private void publishItemActivity(final ActiveStream stream, final JsonNode msg) {
        final JsonNode item = msg.path("item");
        final String itemType = firstNonBlank(
                text(item, "type", ""),
                text(item, "kind", ""),
                text(msg, "item_type", "")
        );
        if (itemType.equalsIgnoreCase("local_shell_call")
                || itemType.equalsIgnoreCase("execution")
                || itemType.equalsIgnoreCase("exec_command")
                || itemType.equalsIgnoreCase("command_execution")) {
            publishActivity(
                    stream.threadId,
                    ChatMessageStreamEvent.ActivityType.TOOL,
                    describeCommand(item),
                    "Codex terminal"
            );
            return;
        }
        if (itemType.equalsIgnoreCase("function_call")
                || itemType.equalsIgnoreCase("custom_tool_call")
                || itemType.equalsIgnoreCase("mcp_tool_call")) {
            final String name = firstNonBlank(
                    text(item, "name", ""),
                    text(item, "tool_name", ""),
                    text(item, "tool_title", ""),
                    text(item, "call_id", ""),
                    "tool"
            );
            publishActivity(stream.threadId, ChatMessageStreamEvent.ActivityType.TOOL, name, "Codex");
        }
    }

    private void publishTodoList(final ActiveStream stream, final JsonNode msg) {
        final JsonNode item = msg.path("item");
        final String itemType = text(item, "type", "");
        if (!itemType.equalsIgnoreCase("todo_list")) {
            return;
        }
        final JsonNode items = item.path("items");
        if (!items.isArray()) {
            return;
        }
        final List<TodoItem> todoItems = new ArrayList<>();
        for (final JsonNode todo : items) {
            final String content = firstNonBlank(
                    text(todo, "text", ""),
                    text(todo, "content", ""),
                    text(todo, "title", "")
            );
            if (content.isBlank()) {
                continue;
            }
            final String status = firstNonBlank(
                    text(todo, "status", ""),
                    todo.path("completed").asBoolean(false) ? "completed" : "pending"
            );
            final String priority = firstNonBlank(text(todo, "priority", ""), "medium");
            todoItems.add(new TodoItem(content, status, priority));
        }
        if (!todoItems.isEmpty()) {
            Event.publish(ChatMessageStreamEvent.todoUpdate(stream.threadId, todoItems));
        }
    }

    private void publishActivity(final String threadId,
                                 final ChatMessageStreamEvent.ActivityType activityType,
                                 final String name,
                                 final String detail) {
        final String activityName = firstNonBlank(name, "tool");
        final String dedupeKey = activityType + ":" + activityName + ":" + firstNonBlank(detail, "");
        final ActiveStream stream = activeStreams.get(threadId);
        if (stream != null && !stream.seenActivities.add(dedupeKey)) {
            return;
        }
        Event.publish(new ChatMessageStreamEvent(
                threadId,
                activityType,
                activityName,
                detail
        ));
    }

    private String commandText(final JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            final List<String> parts = new ArrayList<>();
            for (final JsonNode item : node) {
                final String value = commandText(item);
                if (!value.isBlank()) {
                    parts.add(value);
                }
            }
            return String.join(" ", parts);
        }
        if (node.isObject()) {
            final String direct = firstNonBlank(
                    commandText(node.path("cmd")),
                    commandText(node.path("command")),
                    commandText(node.path("shell_command")),
                    commandText(node.path("display_command")),
                    commandText(node.path("input")),
                    commandText(node.path("query")),
                    text(node, "program", "")
            );
            if (!direct.isBlank()) {
                return direct;
            }
            final String type = text(node, "type", "");
            return type.isBlank() ? "" : type;
        }
        return "";
    }

    private List<Path> writeImageAttachments(final List<ImageAttachment> imageAttachments) throws IOException {
        if (imageAttachments == null || imageAttachments.isEmpty()) {
            return List.of();
        }
        final List<Path> files = new ArrayList<>();
        for (final ImageAttachment image : imageAttachments) {
            if (image == null || image.base64Data() == null || image.base64Data().isBlank()) {
                continue;
            }
            final String suffix = switch (image.mimeType() == null ? "" : image.mimeType().toLowerCase()) {
                case "image/jpeg", "image/jpg" -> ".jpg";
                case "image/webp" -> ".webp";
                default -> ".png";
            };
            final Path file = Files.createTempFile("alina-codex-image-", suffix);
            Files.write(file, java.util.Base64.getDecoder().decode(image.base64Data()));
            files.add(file);
        }
        return files;
    }

    private CommandResult runCommand(final List<String> command,
                                     final Duration timeout) throws IOException, InterruptedException {
        final ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(configurationService.resolveWorkingDirectory().toFile());
        processBuilder.redirectErrorStream(true);
        final Process process = processBuilder.start();
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        final boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new CommandResult(124, output);
        }
        return new CommandResult(process.exitValue(), output);
    }

    private void closeStream(final ActiveStream stream) {
        if (stream.process != null && stream.process.isAlive()) {
            stream.process.destroyForcibly();
        }
    }

    private void deleteTempFiles(final ActiveStream stream) {
        final Set<Path> files = new LinkedHashSet<>(stream.imageFiles);
        if (stream.outputFile != null) {
            files.add(stream.outputFile);
        }
        for (final Path file : files) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                logger.debug("Cannot delete temporary Codex file {}", file, e);
            }
        }
    }

    private String firstJsonObject(final String output) {
        if (output == null) {
            return null;
        }
        final int start = output.indexOf('{');
        final int end = output.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        return output.substring(start, end + 1);
    }

    private String lastNonBlankLine(final String output) {
        if (output == null || output.isBlank()) {
            return null;
        }
        final List<String> lines = output.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("WARNING:"))
                .toList();
        return lines.isEmpty() ? null : lines.getLast();
    }

    private String summarize(final String output) {
        if (output == null || output.isBlank()) {
            return "Unknown Codex error";
        }
        final List<String> messages = new ArrayList<>();
        final List<String> eventTypes = new ArrayList<>();
        for (final String line : output.lines().map(String::trim).filter(line -> !line.isBlank()).toList()) {
            if (line.startsWith("{")) {
                try {
                    final JsonNode event = objectMapper.readTree(line);
                    final JsonNode msg = event.path("msg").isObject() ? event.path("msg") : event;
                    final String type = text(msg, "type", text(event, "type", ""));
                    if (!type.isBlank()) {
                        eventTypes.add(type);
                    }
                    final String error = extractErrorSummary(event);
                    if (!error.isBlank()) {
                        messages.add(error);
                    }
                } catch (Exception ignored) {
                    messages.add(line);
                }
            } else if (!line.startsWith("WARNING:")) {
                messages.add(line);
            }
        }
        final String text = messages.stream()
                .map(String::trim)
                .filter(message -> !message.isBlank())
                .distinct()
                .collect(Collectors.joining(System.lineSeparator()));
        if (!text.isBlank()) {
            return limit(text);
        }
        if (!eventTypes.isEmpty()) {
            final int from = Math.max(0, eventTypes.size() - 5);
            return "Codex exited without an assistant response. Last events: "
                    + String.join(", ", eventTypes.subList(from, eventTypes.size()));
        }
        return "Codex exited without an assistant response.";
    }

    private boolean isErrorEvent(final String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        final String normalized = type.toLowerCase();
        return normalized.equals("error")
                || normalized.equals("stream_error")
                || normalized.endsWith(".failed")
                || normalized.endsWith("_failed");
    }

    private String extractErrorSummary(final JsonNode event) {
        if (event == null || event.isMissingNode() || event.isNull()) {
            return "";
        }
        final JsonNode msg = event.path("msg").isObject() ? event.path("msg") : event;
        final String type = text(msg, "type", text(event, "type", ""));
        final JsonNode item = msg.path("item");
        final String failedToolOutput = "failed".equalsIgnoreCase(text(item, "status", ""))
                ? firstNonBlank(text(item, "aggregated_output", ""), text(item, "output", ""))
                : "";
        final String message = firstNonBlank(
                text(msg, "message", ""),
                text(event, "message", ""),
                text(msg.path("error"), "message", ""),
                text(event.path("error"), "message", ""),
                text(msg, "error", ""),
                text(event, "error", ""),
                failedToolOutput
        );
        if (!message.isBlank()) {
            final String command = commandText(item.path("command"));
            if (!command.isBlank() && !failedToolOutput.isBlank()) {
                return command + ": " + limit(message.trim());
            }
            return limit(message.trim());
        }
        return isErrorEvent(type) ? type : "";
    }

    private String limit(final String text) {
        return text.length() > 1_000 ? text.substring(0, 1_000) + "..." : text;
    }

    private String text(final JsonNode node, final String field, final String fallback) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return fallback;
        }
        final JsonNode value = node.path(field);
        if (value.isTextual()) {
            return value.asText();
        }
        return fallback;
    }

    private String firstNonBlank(final String... values) {
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private long firstPositive(final long... values) {
        for (final long value : values) {
            if (value > 0) {
                return value;
            }
        }
        return 0;
    }

    private record CommandResult(int exitCode, String output) {
    }

    private static final class ActiveStream {
        private final String threadId;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final StringBuilder response = new StringBuilder();
        private final List<Path> imageFiles = new ArrayList<>();
        private volatile Process process;
        private volatile Path outputFile;
        private volatile String sessionId;
        private volatile String modelUsed;
        private volatile long tokensTotal;
        private volatile String errorMessage;
        private final Set<String> seenAssistantMessages = ConcurrentHashMap.newKeySet();
        private final Set<String> seenActivities = ConcurrentHashMap.newKeySet();

        private ActiveStream(final String threadId) {
            this.threadId = threadId;
        }
    }
}
