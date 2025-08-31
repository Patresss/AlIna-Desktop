package com.patres.alina.server.thread;

import com.patres.alina.common.thread.ChatThreadResponse;
import com.patres.alina.server.message.ChatMessageStorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.patres.alina.server.thread.ChatThread.newChatThread;
import static com.patres.alina.server.thread.ChatThreadMapper.toChatThreadResponse;
import static com.patres.alina.server.thread.ChatThreadMapper.toChatThreadResponses;

@Service
public class ChatThreadService {

    private static final Logger logger = LoggerFactory.getLogger(ChatThreadService.class);

    private final ChatThreadRepository chatThreadRepository;
    private final ChatMessageStorageRepository chatMessageStorageRepository;

    public ChatThreadService(final ChatThreadRepository chatThreadRepository,
                             final ChatMessageStorageRepository chatMessageStorageRepository
    ) {
        this.chatMessageStorageRepository = chatMessageStorageRepository;
        this.chatThreadRepository = chatThreadRepository;
    }

    public ChatThreadResponse createNewChatThread() {
        logger.info("Creating new chat thread");
        final ChatThread chatThread = chatThreadRepository.save(newChatThread());
        logger.info("Created new chat thread: id=`{}`, name=`{}`", chatThread.id(), chatThread.name());
        return toChatThreadResponse(chatThread);
    }

    public List<ChatThreadResponse> getChatThreads() {
        List<ChatThread> allChatThreads = chatThreadRepository.findAvailableChatThreads();
        logger.info("Found {} total chat threads from MongoDB", allChatThreads.size());

        List<ChatThread> threadsWithMessages = allChatThreads.stream()
                .filter(thread -> chatMessageStorageRepository.conversationExists(thread.id()))
                .toList();

        logger.info("Filtered to {} threads with JSONL messages", threadsWithMessages.size());
        return toChatThreadResponses(threadsWithMessages);
    }

    public void setModifiedAt(final String chatThreadId) {
        chatThreadRepository.updateModifiedAtById(chatThreadId, LocalDateTime.now());
    }

    public void deleteChatThread(final String chatThreadId) {
        chatThreadRepository.deleteById(chatThreadId);
        chatMessageStorageRepository.deleteByThreadId(chatThreadId);
    }

    public void renameChatThread(final String chatThreadId, final String newName) {
        chatThreadRepository.updateNameById(chatThreadId, newName);
    }

}
