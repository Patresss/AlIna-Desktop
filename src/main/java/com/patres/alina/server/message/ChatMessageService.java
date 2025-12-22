package com.patres.alina.server.message;

import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.command.Command;
import com.patres.alina.server.command.CommandConstants;
import com.patres.alina.server.command.CommandFileService;
import com.patres.alina.server.mcp.McpChatIntegrationService;
import com.patres.alina.server.openai.OpenAiApiFacade;
import com.patres.alina.server.thread.ChatThreadFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.List;
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

    private static final class StreamSession {
        private final ChatMessageSendModel chatMessageSendModel;
        private final StreamPurpose purpose;
        private final List<String> messageIdsToDeleteOnComplete;
        private final StringBuilder fullResponse = new StringBuilder();
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
    private final OpenAiApiFacade openAiApi;
    private final StoreMessageManager storeMessageManager;
    private final ChatThreadFacade chatThreadFacade;
    private final FileManager<AssistantSettings> assistantSettingsManager;
    private final McpChatIntegrationService mcpChatIntegrationService;

    private final ConcurrentHashMap<String, StreamSession> activeStreams = new ConcurrentHashMap<>();

    public ChatMessageService(final ChatMessageStorageRepository chatMessageRepository,
                              final CommandFileService commandFileService,
                              final OpenAiApiFacade openAiApi,
                              final StoreMessageManager storeMessageManager,
                              final ChatThreadFacade chatThreadFacade,
                              final FileManager<AssistantSettings> assistantSettingsManager,
                              final McpChatIntegrationService mcpChatIntegrationService) {
        this.chatMessageRepository = chatMessageRepository;
        this.openAiApi = openAiApi;
        this.commandFileService = commandFileService;
        this.storeMessageManager = storeMessageManager;
        this.chatThreadFacade = chatThreadFacade;
        this.assistantSettingsManager = assistantSettingsManager;
        this.mcpChatIntegrationService = mcpChatIntegrationService;
    }

    public synchronized void sendMessageStream(final ChatMessageSendModel chatMessageSendModel) {
        if (chatMessageSendModel.chatThreadId() == null) {
            final ChatThread newChatThread = chatThreadFacade.createNewChatThread();
            final ChatMessageSendModel withNewThread = new ChatMessageSendModel(
                    chatMessageSendModel.content(),
                    newChatThread.id(),
                    chatMessageSendModel.commandId(),
                    chatMessageSendModel.styleType(),
                    chatMessageSendModel.onComplete()
            );
            sendMessageStreamWithChatThread(withNewThread);
            return;
        }
        sendMessageStreamWithChatThread(chatMessageSendModel);
    }

    public synchronized void cancelStreaming(final String chatThreadId) {
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
                storeMessageManager.storeMessage(new AssistantMessage(partial), warnModel, partial);
            }
        }

        DefaultEventBus.getInstance().publish(
                new ChatMessageStreamEvent(chatThreadId, ChatMessageStreamEvent.StreamEventType.CANCELLED)
        );
    }

    public synchronized void regenerateLastAssistantResponse(final String chatThreadId) {
        cancelStreamingSilently(chatThreadId);

        final List<ChatMessage> allMessages = chatMessageRepository.findAllByThreadId(chatThreadId);
        final int lastUserIndex = findLastIndexByRole(allMessages, MessageType.USER);
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
                .filter(msg -> msg.role() == MessageType.ASSISTANT)
                .map(ChatMessage::id)
                .toList();

        final List<AbstractMessage> contextMessages = loadMessagesForRegeneration(allMessages, lastUserIndex);

        final ChatMessage lastUser = allMessages.get(lastUserIndex);
        final ChatMessageSendModel regenerateModel = new ChatMessageSendModel(
                lastUser.content(),
                chatThreadId,
                lastUser.commandId()
        );

        sendStreamingAssistantResponse(contextMessages, regenerateModel, StreamPurpose.REGENERATE, messageIdsToDeleteOnComplete);
    }

    public synchronized void sendMessageStreamWithChatThread(final ChatMessageSendModel chatMessageSendModel) {
        final String chatThreadId = chatMessageSendModel.chatThreadId();
        logger.info("Sending streaming '{}' content, threadId={} ...", chatMessageSendModel.content(), chatThreadId);

        cancelStreamingSilently(chatThreadId);

        final List<AbstractMessage> contextMessages = loadMessages(chatThreadId);
        final String chatContent = calculateContentWithCommandPrompt(chatMessageSendModel.content(), chatMessageSendModel.commandId());
        final AbstractMessage userChatMessage = new UserMessage(chatContent);

        // Store the user message first
        storeMessageManager.storeMessage(userChatMessage, chatMessageSendModel, chatMessageSendModel.content());

        sendStreamingMessage(contextMessages, userChatMessage, chatMessageSendModel);
    }

    private void sendStreamingMessage(final List<AbstractMessage> contextMessages,
                                      final AbstractMessage message,
                                      final ChatMessageSendModel chatMessageSendModel) {
        contextMessages.add(message);
        sendStreamingAssistantResponse(contextMessages, chatMessageSendModel, StreamPurpose.NORMAL, List.of());
    }

    private void sendStreamingAssistantResponse(final List<AbstractMessage> contextMessages,
                                                final ChatMessageSendModel chatMessageSendModel,
                                                final StreamPurpose purpose,
                                                final List<String> messageIdsToDeleteOnComplete) {
        final String chatThreadId = chatMessageSendModel.chatThreadId();

        // Enhance messages with MCP tools if available
        final List<AbstractMessage> enhancedMessages = mcpChatIntegrationService.enhanceMessagesWithMcpTools(contextMessages);
        
        if (mcpChatIntegrationService.hasMcpTools()) {
            logger.debug("Enhanced chat messages with {} MCP tools", mcpChatIntegrationService.getAvailableToolCount());
        }

        final StreamSession session = new StreamSession(chatMessageSendModel, purpose, messageIdsToDeleteOnComplete);
        activeStreams.put(chatThreadId, session);

        try {
            final Flux<String> stream = openAiApi.sendMessageStream(enhancedMessages);

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
                        activeStreams.remove(chatThreadId, session);
                        if (session.cancelled) {
                            return;
                        }
                        logger.info("Streaming completed for threadId: {}", chatMessageSendModel.chatThreadId());

                        final String aiResponse = session.fullResponse.toString();
                        final AbstractMessage assistantMessage = new AssistantMessage(aiResponse);
                        if (session.purpose == StreamPurpose.REGENERATE) {
                            for (final String messageId : session.messageIdsToDeleteOnComplete) {
                                chatMessageRepository.deleteMessage(chatThreadId, messageId);
                            }
                        }
                        storeMessageManager.storeMessage(assistantMessage, chatMessageSendModel, aiResponse);

                        DefaultEventBus.getInstance().publish(
                                new ChatMessageStreamEvent(
                                        chatThreadId,
                                        ChatMessageStreamEvent.StreamEventType.COMPLETE
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
        return messages.stream()
                .map(msg -> new ChatMessageResponseModel(
                        msg.content(),
                        ChatMessageRole.findChatMessageRoleByValue(msg.role().getValue()),
                        msg.createdAt(),
                        msg.styleType(),
                        chatThreadId
                ))
                .toList();
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

    private List<AbstractMessage> loadMessages(final String chatThreadId) {
        final int numberOfMessagesInContext = assistantSettingsManager.getSettings().numberOfMessagesInContext();
        final List<ChatMessage> messages = chatMessageRepository.findLastMessagesForContext(
                chatThreadId,
                Set.of(ChatMessageRole.USER, ChatMessageRole.ASSISTANT, ChatMessageRole.SYSTEM),
                numberOfMessagesInContext
        );
        return messages.stream()
                .map(this::toAbstractMessage)
                .collect(Collectors.toList());
    }

    private List<AbstractMessage> loadMessagesForRegeneration(final List<ChatMessage> allMessages,
                                                             final int lastUserIndex) {
        final int numberOfMessagesInContext = Math.max(0, assistantSettingsManager.getSettings().numberOfMessagesInContext());
        final List<ChatMessage> prefix = allMessages.subList(0, lastUserIndex + 1).stream()
                .filter(msg -> msg.role() == MessageType.USER || msg.role() == MessageType.ASSISTANT || msg.role() == MessageType.SYSTEM)
                .toList();

        final int from = Math.max(0, prefix.size() - numberOfMessagesInContext);
        final List<ChatMessage> window = prefix.subList(from, prefix.size());
        final List<AbstractMessage> context = window.stream().map(this::toAbstractMessage).collect(Collectors.toList());

        if (!context.isEmpty() && context.getLast().getMessageType() == MessageType.USER) {
            return context;
        }

        // Safety net: ensure the last user prompt is included and last.
        final ChatMessage lastUser = allMessages.get(lastUserIndex);
        context.add(new UserMessage(lastUser.contentWithContext()));
        return context;
    }

    private AbstractMessage toAbstractMessage(final ChatMessage message) {
        return switch (message.role()) {
            case USER -> new UserMessage(message.contentWithContext());
            case ASSISTANT -> new AssistantMessage(message.contentWithContext());
            case SYSTEM -> new SystemMessage(message.contentWithContext());
            default -> throw new IllegalArgumentException("Unsupported message role: " + message.role());
        };
    }

    private void cancelStreamingSilently(final String chatThreadId) {
        final StreamSession session = activeStreams.remove(chatThreadId);
        if (session == null) {
            return;
        }
        session.cancelled = true;
        disposeQuietly(session.disposable);
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

    private static int findLastIndexByRole(final List<ChatMessage> messages, final MessageType role) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).role() == role) {
                return i;
            }
        }
        return -1;
    }
}
