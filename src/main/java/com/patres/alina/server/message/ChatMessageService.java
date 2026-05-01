package com.patres.alina.server.message;

import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ImageAttachment;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.command.Command;
import com.patres.alina.server.command.CommandConstants;
import com.patres.alina.server.command.CommandFileService;
import com.patres.alina.server.assistant.AssistantPromptService;
import com.patres.alina.server.opencode.OpenCodeRuntimeService;
import com.patres.alina.server.opencode.OpenCodeSessionService;
import com.patres.alina.server.thread.ChatThreadFacade;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageService.class);

    private enum StreamPurpose {
        NORMAL,
        REGENERATE
    }

    private static final class StreamSession {
        private final ChatMessageSendModel chatMessageSendModel;
        private final StreamPurpose purpose;
        private final StringBuilder fullResponse = new StringBuilder();
        private Disposable disposable;
        private volatile boolean cancelled = false;

        private StreamSession(final ChatMessageSendModel chatMessageSendModel,
                              final StreamPurpose purpose) {
            this.chatMessageSendModel = chatMessageSendModel;
            this.purpose = purpose;
        }
    }

    private final CommandFileService commandFileService;
    private final ChatThreadFacade chatThreadFacade;
    private final FileManager<AssistantSettings> assistantSettingsManager;
    private final AssistantPromptService assistantPromptService;
    private final OpenCodeRuntimeService openCodeRuntimeService;
    private final OpenCodeSessionService openCodeSessionService;

    private final ConcurrentHashMap<String, StreamSession> activeStreams = new ConcurrentHashMap<>();

    public ChatMessageService(final CommandFileService commandFileService,
                              final ChatThreadFacade chatThreadFacade,
                              final FileManager<AssistantSettings> assistantSettingsManager,
                              final AssistantPromptService assistantPromptService,
                              final OpenCodeRuntimeService openCodeRuntimeService,
                              final OpenCodeSessionService openCodeSessionService) {
        this.commandFileService = commandFileService;
        this.chatThreadFacade = chatThreadFacade;
        this.assistantSettingsManager = assistantSettingsManager;
        this.assistantPromptService = assistantPromptService;
        this.openCodeRuntimeService = openCodeRuntimeService;
        this.openCodeSessionService = openCodeSessionService;
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
        DefaultEventBus.getInstance().publish(
                new ChatMessageStreamEvent(chatThreadId, ChatMessageStreamEvent.StreamEventType.CANCELLED)
        );
        if (session != null) {
            session.cancelled = true;
            disposeQuietly(session.disposable);
        }
    }

    public synchronized void sendMessageStreamWithChatThread(final ChatMessageSendModel chatMessageSendModel) {
        final String chatThreadId = chatMessageSendModel.chatThreadId();
        logger.info("Sending streaming '{}' content, threadId={} ...", chatMessageSendModel.content(), chatThreadId);

        cancelStreamingSilently(chatThreadId);

        final String chatContent = calculateContentWithCommandPrompt(chatMessageSendModel.content(), chatMessageSendModel.commandId());
        final String systemPrompt = assistantPromptService.buildSystemPrompt(assistantSettingsManager.getSettings());
        final String commandModelOverride = resolveCommandModelOverride(chatMessageSendModel.commandId());
        final String perTabModel = chatMessageSendModel.model();
        final String modelOverride;
        if (commandModelOverride != null && !commandModelOverride.isBlank()) {
            modelOverride = commandModelOverride.trim();
        } else if (perTabModel != null && !perTabModel.isBlank()) {
            modelOverride = perTabModel.trim();
        } else {
            modelOverride = null;
        }

        sendStreamingAssistantResponse(chatContent, systemPrompt, modelOverride, chatMessageSendModel, StreamPurpose.NORMAL,
                chatMessageSendModel.imageAttachments());
    }

    private void sendStreamingAssistantResponse(final String userMessage,
                                                final String systemPrompt,
                                                final String modelOverride,
                                                final ChatMessageSendModel chatMessageSendModel,
                                                final StreamPurpose purpose,
                                                final List<ImageAttachment> imageAttachments) {
        final String chatThreadId = chatMessageSendModel.chatThreadId();
        final StreamSession session = new StreamSession(chatMessageSendModel, purpose);
        activeStreams.put(chatThreadId, session);

        try {
            if (!openCodeRuntimeService.isEnabled()) {
                throw new IllegalStateException(LanguageManager.getLanguageString("error.opencode.disabled"));
            }

            final Flux<String> stream = openCodeRuntimeService.sendMessageStream(
                    chatThreadId,
                    null,
                    userMessage,
                    systemPrompt,
                    "",
                    modelOverride,
                    purpose == StreamPurpose.REGENERATE,
                    imageAttachments != null ? imageAttachments : List.of()
            );

            final Disposable disposable = stream.subscribe(
                    token -> {
                        if (session.cancelled) return;
                        session.fullResponse.append(token);
                        DefaultEventBus.getInstance().publish(new ChatMessageStreamEvent(chatThreadId, token));
                    },
                    error -> {
                        activeStreams.remove(chatThreadId, session);
                        if (session.cancelled) return;
                        logger.error("Error in streaming response", error);
                        DefaultEventBus.getInstance().publish(
                                new ChatMessageStreamEvent(chatThreadId, ChatMessageStreamEvent.StreamEventType.ERROR, error.getMessage())
                        );
                    },
                    () -> {
                        final String openCodeModel = openCodeRuntimeService.getModelUsedForThread(chatThreadId);
                        final String openCodeAgent = openCodeRuntimeService.getAgentUsedForThread(chatThreadId);
                        final long openCodeTokensTotal = openCodeRuntimeService.getTokensTotalForThread(chatThreadId);
                        final double openCodeCost = openCodeRuntimeService.getCostForThread(chatThreadId);
                        activeStreams.remove(chatThreadId, session);
                        if (session.cancelled) return;
                        logger.info("Streaming completed for threadId: {}", chatThreadId);

                        DefaultEventBus.getInstance().publish(
                                ChatMessageStreamEvent.complete(chatThreadId, openCodeModel, openCodeAgent, openCodeTokensTotal, openCodeCost)
                        );

                        if (chatMessageSendModel.onComplete() != null) {
                            logger.info("Executing onComplete callback for threadId: {}", chatThreadId);
                            chatMessageSendModel.onComplete().onComplete(session.fullResponse.toString());
                        }
                    }
            );
            session.disposable = disposable;

        } catch (Exception e) {
            activeStreams.remove(chatThreadId, session);
            if (session.cancelled) return;
            logger.error("Error starting streaming", e);
            DefaultEventBus.getInstance().publish(
                    new ChatMessageStreamEvent(chatThreadId, ChatMessageStreamEvent.StreamEventType.ERROR, e.getMessage())
            );
        }
    }

    public synchronized void regenerateLastAssistantResponse(final String chatThreadId) {
        final String lastUserMessage = findLastUserMessage(chatThreadId);
        if (lastUserMessage == null) {
            DefaultEventBus.getInstance().publish(
                    new ChatMessageStreamEvent(chatThreadId, ChatMessageStreamEvent.StreamEventType.ERROR, "No user message to regenerate")
            );
            return;
        }
        cancelStreamingSilently(chatThreadId);
        final String systemPrompt = assistantPromptService.buildSystemPrompt(assistantSettingsManager.getSettings());
        final ChatMessageSendModel model = new ChatMessageSendModel(lastUserMessage, chatThreadId, null, null, null, null);
        sendStreamingAssistantResponse(lastUserMessage, systemPrompt, null, model, StreamPurpose.REGENERATE, List.of());
    }

    public synchronized void retryLastUserMessage(final String chatThreadId) {
        final String lastUserMessage = findLastUserMessage(chatThreadId);
        if (lastUserMessage == null) {
            DefaultEventBus.getInstance().publish(
                    new ChatMessageStreamEvent(chatThreadId, ChatMessageStreamEvent.StreamEventType.ERROR, "No user message to retry")
            );
            return;
        }
        cancelStreamingSilently(chatThreadId);
        final String systemPrompt = assistantPromptService.buildSystemPrompt(assistantSettingsManager.getSettings());
        final ChatMessageSendModel model = new ChatMessageSendModel(lastUserMessage, chatThreadId, null, null, null, null);
        sendStreamingAssistantResponse(lastUserMessage, systemPrompt, null, model, StreamPurpose.NORMAL, List.of());
    }

    private String findLastUserMessage(final String chatThreadId) {
        final List<ChatMessageResponseModel> messages = getMessagesByThreadId(chatThreadId);
        for (int i = messages.size() - 1; i >= 0; i--) {
            final ChatMessageResponseModel msg = messages.get(i);
            if (msg.sender() == ChatMessageRole.USER) {
                return msg.content();
            }
        }
        return null;
    }

    /**
     * Returns messages for history display, sourced from OpenCode server.
     */
    public List<ChatMessageResponseModel> getMessagesByThreadId(final String chatThreadId) {
        // First try registry lookup (AlIna threadId → OpenCode sessionId)
        String sessionId = openCodeSessionService.resolveSessionId(chatThreadId);
        // Fallback: chatThreadId may already be an OpenCode sessionId (e.g. sessions loaded from history)
        if (sessionId == null) {
            sessionId = chatThreadId;
        }
        return openCodeSessionService.getMessages(sessionId);
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

    private void cancelStreamingSilently(final String chatThreadId) {
        if (openCodeRuntimeService.isEnabled()) {
            openCodeRuntimeService.cancelStreaming(chatThreadId);
        }
        final StreamSession session = activeStreams.remove(chatThreadId);
        if (session == null) return;
        session.cancelled = true;
        disposeQuietly(session.disposable);
    }

    private static void disposeQuietly(final Disposable disposable) {
        if (disposable == null) return;
        try {
            disposable.dispose();
        } catch (Exception ignored) {
        }
    }
}
