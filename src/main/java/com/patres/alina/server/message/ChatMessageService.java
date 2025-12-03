package com.patres.alina.server.message;

import com.patres.alina.common.event.ChatMessageStreamEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageResponseModel;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageSendModel;
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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatMessageService {

    private static final Logger logger = LoggerFactory.getLogger(ChatMessageService.class);

    private final ChatMessageStorageRepository chatMessageRepository;
    private final CommandFileService commandFileService;
    private final OpenAiApiFacade openAiApi;
    private final StoreMessageManager storeMessageManager;
    private final ChatThreadFacade chatThreadFacade;
    private final FileManager<AssistantSettings> assistantSettingsManager;
    private final McpChatIntegrationService mcpChatIntegrationService;

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
                    chatMessageSendModel.styleType()
            );
            sendMessageStreamWithChatThread(withNewThread);
            return;
        }
        sendMessageStreamWithChatThread(chatMessageSendModel);
    }

    public synchronized void sendMessageStreamWithChatThread(final ChatMessageSendModel chatMessageSendModel) {
        final String chatThreadId = chatMessageSendModel.chatThreadId();
        logger.info("Sending streaming '{}' content, threadId={} ...", chatMessageSendModel.content(), chatThreadId);

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

        // Enhance messages with MCP tools if available
        final List<AbstractMessage> enhancedMessages = mcpChatIntegrationService.enhanceMessagesWithMcpTools(contextMessages);
        
        if (mcpChatIntegrationService.hasMcpTools()) {
            logger.debug("Enhanced chat messages with {} MCP tools", mcpChatIntegrationService.getAvailableToolCount());
        }

        final StringBuilder fullResponse = new StringBuilder();

        try {
            final reactor.core.publisher.Flux<String> stream = openAiApi.sendMessageStream(enhancedMessages);

            stream.subscribe(
                    token -> {
                        fullResponse.append(token);
                        DefaultEventBus.getInstance().publish(
                                new ChatMessageStreamEvent(chatMessageSendModel.chatThreadId(), token)
                        );
                    },
                    error -> {
                        logger.error("Error in streaming response", error);
                        DefaultEventBus.getInstance().publish(
                                new ChatMessageStreamEvent(
                                        chatMessageSendModel.chatThreadId(),
                                        ChatMessageStreamEvent.StreamEventType.ERROR,
                                        error.getMessage()
                                )
                        );
                    },
                    () -> {
                        logger.info("Streaming completed for threadId: {}", chatMessageSendModel.chatThreadId());

                        final AbstractMessage assistantMessage = new AssistantMessage(fullResponse.toString());
                        storeMessageManager.storeMessage(assistantMessage, chatMessageSendModel, fullResponse.toString());

                        DefaultEventBus.getInstance().publish(
                                new ChatMessageStreamEvent(
                                        chatMessageSendModel.chatThreadId(),
                                        ChatMessageStreamEvent.StreamEventType.COMPLETE
                                )
                        );
                    }
            );

        } catch (Exception e) {
            logger.error("Error starting streaming", e);
            DefaultEventBus.getInstance().publish(
                    new ChatMessageStreamEvent(
                            chatMessageSendModel.chatThreadId(),
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
        final List<ChatMessage> messages = chatMessageRepository.findLastMessagesForContext(chatThreadId, numberOfMessagesInContext);
        return messages.stream().map(this::toAbstractMessage).collect(Collectors.toList());
    }

    private AbstractMessage toAbstractMessage(final ChatMessage message) {
        return switch (message.role()) {
            case USER -> new UserMessage(message.contentWithContext());
            case ASSISTANT -> new AssistantMessage(message.contentWithContext());
            case SYSTEM -> new SystemMessage(message.contentWithContext());
            default -> throw new IllegalArgumentException("Unsupported message role: " + message.role());
        };
    }
}
