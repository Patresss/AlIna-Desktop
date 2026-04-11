package com.patres.alina.server.message;

import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.message.CommandUsageInfo;
import com.patres.alina.common.message.ContextMessage;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.command.Command;
import com.patres.alina.server.command.CommandConstants;
import com.patres.alina.server.command.CommandFileService;
import com.patres.alina.server.assistant.AssistantPromptService;
import com.patres.alina.server.opencode.OpenCodeRuntimeService;
import com.patres.alina.server.thread.ChatThreadFacade;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageService.class);

    private enum StreamPurpose {
        NORMAL,
        REGENERATE
    }

    private static final int DEFAULT_NUMBER_OF_CONTEXT_MESSAGES = 5;

    private static final class StreamSession {
        private final ChatMessageSendModel chatMessageSendModel;
        private final StreamPurpose purpose;
        private final List<String> messageIdsToDeleteOnComplete;
        private final StringBuilder fullResponse = new StringBuilder();
        private String effectiveModel;
        private Disposable disposable;
        private volatile boolean cancelled = false;

        private StreamSession(final ChatMessageSendModel chatMessageSendModel,
                              final StreamPurpose purpose,
                              final List<String> messageIdsToDeleteOnComplete) {
            this.chatMessageSendModel = chatMessageSendModel;
            this.purpose = purpose;
            this.messageIdsToDeleteOnComplete = messageIdsToDeleteOnComplete;
        }
    }

    private final ChatMessageStorageRepository chatMessageRepository;
    private final CommandFileService commandFileService;
    private final StoreMessageManager storeMessageManager;
    private final ChatThreadFacade chatThreadFacade;
    private final FileManager<AssistantSettings> assistantSettingsManager;
    private final AssistantPromptService assistantPromptService;
    private final OpenCodeRuntimeService openCodeRuntimeService;

    private final ConcurrentHashMap<String, StreamSession> activeStreams = new ConcurrentHashMap<>();

    public ChatMessageService(final ChatMessageStorageRepository chatMessageRepository,
                              final CommandFileService commandFileService,
                              final StoreMessageManager storeMessageManager,
                              final ChatThreadFacade chatThreadFacade,
                              final FileManager<AssistantSettings> assistantSettingsManager,
                              final AssistantPromptService assistantPromptService,
                              final OpenCodeRuntimeService openCodeRuntimeService) {
        this.chatMessageRepository = chatMessageRepository;
        this.commandFileService = commandFileService;
        this.storeMessageManager = storeMessageManager;
        this.chatThreadFacade = chatThreadFacade;
        this.assistantSettingsManager = assistantSettingsManager;
        this.assistantPromptService = assistantPromptService;
        this.openCodeRuntimeService = openCodeRuntimeService;
    }

    public synchronized void sendMessageStream(final ChatMessageSendModel chatMessageSendModel) {
        if (chatMessageSendModel.chatThreadId() == null) {
            final ChatThread newChatThread = chatThreadFacade.createNewChatThread();
            final ChatMessageSendModel withNewThread = new ChatMessageSendModel(
                    chatMessageSendModel.content(),
                    newChatThread.id(),
                    chatMessageSendModel.commandId(),
                    chatMessageSendModel.styleType(),
                    chatMessageSendModel.onComplete(),
                    chatMessageSendModel.model()
            );
            sendMessageStreamWithChatThread(withNewThread);
            return;
        }
        sendMessageStreamWithChatThread(chatMessageSendModel);
    }

    public synchronized void cancelStreaming(final String chatThreadId) {
        if (openCodeRuntimeService.isEnabled()) {
            openCodeRuntimeService.cancelStreaming(chatThreadId);
        }
        final StreamSession session = activeStreams.remove(chatThreadId);
        if (session == null) {
            DefaultEventBus.getInstance().publish(
                    new ChatMessageStreamEvent(chatThreadId, ChatMessageStreamEvent.StreamEventType.CANCELLED)
            );
            return;
        }
        session.cancelled = true;
        disposeQuietly(session.disposable);

        if (session.purpose == StreamPurpose.NORMAL) {
            final String partial = session.fullResponse.toString();
            if (!partial.isBlank()) {
                final ChatMessageSendModel warnModel = new ChatMessageSendModel(
                        session.chatMessageSendModel.content(),
                        chatThreadId,
                        session.chatMessageSendModel.commandId(),
                        ChatMessageStyleType.WARNING,
                        null
                );
                storeMessageManager.storeMessage(ContextMessage.assistant(partial), warnModel, partial);
            }
        }

        DefaultEventBus.getInstance().publish(
                new ChatMessageStreamEvent(chatThreadId, ChatMessageStreamEvent.StreamEventType.CANCELLED)
        );
    }

    public synchronized void regenerateLastAssistantResponse(final String chatThreadId) {
        cancelStreamingSilently(chatThreadId);

        final List<ChatMessage> allMessages = chatMessageRepository.findAllByThreadId(chatThreadId);
        final int lastUserIndex = findLastIndexByRole(allMessages, ChatMessageRole.USER);
        if (lastUserIndex < 0) {
            DefaultEventBus.getInstance().publish(
                    new ChatMessageStreamEvent(
                            chatThreadId,
                            ChatMessageStreamEvent.StreamEventType.ERROR,
                            "No user message to regenerate"
                    )
            );
            return;
        }

        final List<String> messageIdsToDeleteOnComplete = allMessages.subList(lastUserIndex + 1, allMessages.size())
                .stream()
                .filter(msg -> msg.role() == ChatMessageRole.ASSISTANT)
                .map(ChatMessage::id)
                .toList();

        final List<ContextMessage> contextMessages = loadMessagesForRegeneration(allMessages, lastUserIndex);

        final ChatMessage lastUser = allMessages.get(lastUserIndex);
        final ChatMessageSendModel regenerateModel = new ChatMessageSendModel(
                lastUser.content(),
                chatThreadId,
                lastUser.commandId()
        );

        sendStreamingAssistantResponse(contextMessages, regenerateModel, StreamPurpose.REGENERATE, messageIdsToDeleteOnComplete);
    }

    public synchronized void retryLastUserMessage(final String chatThreadId) {
        cancelStreamingSilently(chatThreadId);

        final List<ChatMessage> allMessages = chatMessageRepository.findAllByThreadId(chatThreadId);
        final int lastUserIndex = findLastIndexByRole(allMessages, ChatMessageRole.USER);
        if (lastUserIndex < 0) {
            DefaultEventBus.getInstance().publish(
                    new ChatMessageStreamEvent(
                            chatThreadId,
                            ChatMessageStreamEvent.StreamEventType.ERROR,
                            "No user message to retry"
                    )
            );
            return;
        }

        final List<ContextMessage> contextMessages = loadMessagesForRegeneration(allMessages, lastUserIndex);
        final ChatMessage lastUser = allMessages.get(lastUserIndex);
        final ChatMessageSendModel retryModel = new ChatMessageSendModel(
                lastUser.content(),
                chatThreadId,
                lastUser.commandId()
        );

        sendStreamingAssistantResponse(contextMessages, retryModel, StreamPurpose.NORMAL, List.of());
    }

    public synchronized void sendMessageStreamWithChatThread(final ChatMessageSendModel chatMessageSendModel) {
        final String chatThreadId = chatMessageSendModel.chatThreadId();
        logger.info("Sending streaming '{}' content, threadId={} ...", chatMessageSendModel.content(), chatThreadId);

        cancelStreamingSilently(chatThreadId);

        final List<ContextMessage> contextMessages = loadMessages(chatThreadId);
        final String chatContent = calculateContentWithCommandPrompt(chatMessageSendModel.content(), chatMessageSendModel.commandId());
        final ContextMessage userChatMessage = ContextMessage.user(chatContent);

        // Store the user message first
        storeMessageManager.storeMessage(userChatMessage, chatMessageSendModel, chatMessageSendModel.content());

        sendStreamingMessage(contextMessages, userChatMessage, chatMessageSendModel);
    }

    private void sendStreamingMessage(final List<ContextMessage> contextMessages,
                                      final ContextMessage message,
                                      final ChatMessageSendModel chatMessageSendModel) {
        contextMessages.add(message);
        sendStreamingAssistantResponse(contextMessages, chatMessageSendModel, StreamPurpose.NORMAL, List.of());
    }

    private void sendStreamingAssistantResponse(final List<ContextMessage> contextMessages,
                                                final ChatMessageSendModel chatMessageSendModel,
                                                final StreamPurpose purpose,
                                                final List<String> messageIdsToDeleteOnComplete) {
        final String chatThreadId = chatMessageSendModel.chatThreadId();

        final StreamSession session = new StreamSession(chatMessageSendModel, purpose, messageIdsToDeleteOnComplete);
        activeStreams.put(chatThreadId, session);

        try {
            if (!openCodeRuntimeService.isEnabled()) {
                throw new IllegalStateException(LanguageManager.getLanguageString("error.opencode.disabled"));
            }
            final String systemPrompt = buildOpenCodeSystemPrompt(contextMessages);
            final String historySummary = summarizeHistoryForOpenCode(contextMessages);
            final String commandModelOverride = resolveCommandModelOverride(chatMessageSendModel.commandId());
            // Priority: command model override > per-tab model > global default
            final String perTabModel = chatMessageSendModel.model();
            final String modelOverride;
            if (commandModelOverride != null && !commandModelOverride.isBlank()) {
                modelOverride = commandModelOverride.trim();
            } else if (perTabModel != null && !perTabModel.isBlank()) {
                modelOverride = perTabModel.trim();
            } else {
                modelOverride = null;
            }
            session.effectiveModel = (modelOverride == null)
                    ? assistantSettingsManager.getSettings().resolveModelIdentifier()
                    : modelOverride;
            final String currentUserMessage = contextMessages.isEmpty()
                    ? chatMessageSendModel.content()
                    : contextMessages.getLast().text();
            final Flux<String> stream = openCodeRuntimeService.sendMessageStream(
                    chatThreadId,
                    chatThreadId,
                    currentUserMessage,
                    systemPrompt,
                    historySummary,
                    modelOverride,
                    purpose == StreamPurpose.REGENERATE
            );

            final Disposable disposable = stream.subscribe(
                    token -> {
                        if (session.cancelled) {
                            return;
                        }
                        session.fullResponse.append(token);
                        DefaultEventBus.getInstance().publish(
                                new ChatMessageStreamEvent(chatThreadId, token)
                        );
                    },
                    error -> {
                        activeStreams.remove(chatThreadId, session);
                        if (session.cancelled) {
                            return;
                        }
                        logger.error("Error in streaming response", error);
                        DefaultEventBus.getInstance().publish(
                                new ChatMessageStreamEvent(
                                        chatThreadId,
                                        ChatMessageStreamEvent.StreamEventType.ERROR,
                                        error.getMessage()
                                )
                        );
                    },
                    () -> {
                        final String openCodeModel = openCodeRuntimeService.getModelUsedForThread(chatThreadId);
                        final String openCodeAgent = openCodeRuntimeService.getAgentUsedForThread(chatThreadId);
                        final long openCodeTokensTotal = openCodeRuntimeService.getTokensTotalForThread(chatThreadId);
                        final double openCodeCost = openCodeRuntimeService.getCostForThread(chatThreadId);
                        activeStreams.remove(chatThreadId, session);
                        if (session.cancelled) {
                            return;
                        }
                        logger.info("Streaming completed for threadId: {}", chatMessageSendModel.chatThreadId());

                        final String aiResponse = session.fullResponse.toString();
                        final ContextMessage assistantMessage = ContextMessage.assistant(aiResponse);
                        if (session.purpose == StreamPurpose.REGENERATE) {
                            for (final String messageId : session.messageIdsToDeleteOnComplete) {
                                chatMessageRepository.deleteMessage(chatThreadId, messageId);
                            }
                        }
                        storeMessageManager.storeMessage(assistantMessage, chatMessageSendModel, aiResponse);

                        DefaultEventBus.getInstance().publish(
                                ChatMessageStreamEvent.complete(
                                        chatThreadId,
                                        openCodeModel,
                                        openCodeAgent,
                                        openCodeTokensTotal,
                                        openCodeCost
                                )
                        );

                        // Execute callback if provided
                        if (chatMessageSendModel.onComplete() != null) {
                            logger.info("Executing onComplete callback for threadId: {}", chatMessageSendModel.chatThreadId());
                            chatMessageSendModel.onComplete().onComplete(aiResponse);
                        }
                    }
            );
            session.disposable = disposable;

        } catch (Exception e) {
            activeStreams.remove(chatThreadId, session);
            if (session.cancelled) {
                return;
            }
            logger.error("Error starting streaming", e);
            DefaultEventBus.getInstance().publish(
                    new ChatMessageStreamEvent(
                            chatThreadId,
                            ChatMessageStreamEvent.StreamEventType.ERROR,
                            e.getMessage()
                    )
            );
        }
    }

    public List<ChatMessageResponseModel> getMessagesByThreadId(final String chatThreadId) {
        final List<ChatMessage> messages = chatMessageRepository.findMessagesToDisplay(chatThreadId);
        final Map<String, Command> commandCache = new HashMap<>();
        return messages.stream()
                .map(msg -> new ChatMessageResponseModel(
                        msg.content(),
                        msg.role(),
                        msg.createdAt(),
                        msg.styleType(),
                        chatThreadId,
                        buildCommandUsageInfo(msg, commandCache)
                ))
                .toList();
    }

    private CommandUsageInfo buildCommandUsageInfo(final ChatMessage message, final Map<String, Command> commandCache) {
        if (message.role() != ChatMessageRole.USER) {
            return null;
        }
        final String commandId = message.commandId();
        if (commandId == null || commandId.isBlank()) {
            return null;
        }
        final Command command = commandCache.computeIfAbsent(commandId, this::findCommandById);
        final String commandName = command == null ? commandId : command.name();
        final String commandIcon = command == null ? "bi-slash" : command.icon();
        final String prompt = message.contentWithContext() == null ? message.content() : message.contentWithContext();
        return new CommandUsageInfo(commandId, commandName, commandIcon, prompt);
    }

    private Command findCommandById(final String commandId) {
        return commandFileService.findById(commandId).orElse(null);
    }

    private String calculateContentWithCommandPrompt(final String content, final String commandId) {
        final Optional<Command> commandOpt = Optional.ofNullable(commandId)
                .flatMap(commandFileService::findById);

        final String commandContent = commandOpt
                .map(Command::systemPrompt)
                .orElse("")
                .trim();

        if (commandContent.isEmpty()) {
            return content;
        }

        if (commandContent.contains(CommandConstants.ARGUMENTS_PLACEHOLDER)) {
            return commandContent.replace(CommandConstants.ARGUMENTS_PLACEHOLDER, content);
        }
        return commandContent + System.lineSeparator() + content;
    }

    private String resolveCommandModelOverride(final String commandId) {
        return Optional.ofNullable(commandId)
                .flatMap(commandFileService::findById)
                .map(Command::model)
                .filter(model -> !model.isBlank())
                .orElse(null);
    }

    private List<ContextMessage> loadMessages(final String chatThreadId) {
        final List<ChatMessage> messages = chatMessageRepository.findLastMessagesForContext(
                chatThreadId,
                Set.of(ChatMessageRole.USER, ChatMessageRole.ASSISTANT, ChatMessageRole.SYSTEM),
                DEFAULT_NUMBER_OF_CONTEXT_MESSAGES
        );
        return messages.stream()
                .map(this::toContextMessage)
                .collect(Collectors.toList());
    }

    private List<ContextMessage> loadMessagesForRegeneration(final List<ChatMessage> allMessages,
                                                             final int lastUserIndex) {
        final List<ChatMessage> prefix = allMessages.subList(0, lastUserIndex + 1).stream()
                .filter(msg -> msg.role() == ChatMessageRole.USER || msg.role() == ChatMessageRole.ASSISTANT || msg.role() == ChatMessageRole.SYSTEM)
                .toList();

        final int from = Math.max(0, prefix.size() - DEFAULT_NUMBER_OF_CONTEXT_MESSAGES);
        final List<ChatMessage> window = prefix.subList(from, prefix.size());
        final List<ContextMessage> context = window.stream().map(this::toContextMessage).collect(Collectors.toList());

        if (!context.isEmpty() && context.getLast().role() == ChatMessageRole.USER) {
            return context;
        }

        // Safety net: ensure the last user prompt is included and last.
        final ChatMessage lastUser = allMessages.get(lastUserIndex);
        context.add(ContextMessage.user(lastUser.contentWithContext()));
        return context;
    }

    private ContextMessage toContextMessage(final ChatMessage message) {
        return new ContextMessage(message.contentWithContext(), message.role());
    }

    private void cancelStreamingSilently(final String chatThreadId) {
        if (openCodeRuntimeService.isEnabled()) {
            openCodeRuntimeService.cancelStreaming(chatThreadId);
        }
        final StreamSession session = activeStreams.remove(chatThreadId);
        if (session == null) {
            return;
        }
        session.cancelled = true;
        disposeQuietly(session.disposable);
    }

    private String buildOpenCodeSystemPrompt(final List<ContextMessage> enhancedMessages) {
        final String assistantPrompt = assistantPromptService.buildSystemPrompt(assistantSettingsManager.getSettings());
        final String storedSystemMessages = enhancedMessages.stream()
                .filter(message -> message.role() == ChatMessageRole.SYSTEM)
                .map(ContextMessage::text)
                .filter(text -> text != null && !text.isBlank())
                .collect(Collectors.joining(System.lineSeparator() + System.lineSeparator()));
        if (storedSystemMessages.isBlank()) {
            return assistantPrompt;
        }
        return assistantPrompt + System.lineSeparator() + System.lineSeparator() + storedSystemMessages;
    }

    private String summarizeHistoryForOpenCode(final List<ContextMessage> contextMessages) {
        final String history = contextMessages.stream()
                .filter(message -> message.role() == ChatMessageRole.USER || message.role() == ChatMessageRole.ASSISTANT)
                .map(message -> (message.role() == ChatMessageRole.USER ? "User: " : "Assistant: ") + compact(message.text(), 800))
                .collect(Collectors.joining(System.lineSeparator()));
        if (history.isBlank()) {
            return "";
        }
        return """
                <conversation-history>
                Use this recent local history as background context for this thread. It may come from AlIna storage before the OpenCode session was created.
                %s
                </conversation-history>
                """.formatted(history);
    }

    private String compact(final String text, final int limit) {
        if (text == null) {
            return "";
        }
        final String normalized = text.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.length() <= limit) {
            return normalized;
        }
        return normalized.substring(0, limit) + "...";
    }

    private static void disposeQuietly(final Disposable disposable) {
        if (disposable == null) {
            return;
        }
        try {
            disposable.dispose();
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static int findLastIndexByRole(final List<ChatMessage> messages, final ChatMessageRole role) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).role() == role) {
                return i;
            }
        }
        return -1;
    }
}
