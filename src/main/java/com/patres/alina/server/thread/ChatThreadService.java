package com.patres.alina.server.thread;

import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.server.message.ChatMessageStorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.patres.alina.common.thread.ChatThread.newChatThread;

@Service
public class ChatThreadService {

    private static final Logger logger = LoggerFactory.getLogger(ChatThreadService.class);

    private final Path conversationsDir;
    private final ChatMessageStorageRepository chatMessageStorageRepository;

    public ChatThreadService(final Path conversationsStoragePath,
                             final ChatMessageStorageRepository chatMessageStorageRepository) {
        this.conversationsDir = conversationsStoragePath;
        this.chatMessageStorageRepository = chatMessageStorageRepository;
    }

    public ChatThread createNewChatThread() {
        logger.info("Creating new chat thread");
        // Do NOT create any files here. File will be created on first saved message.
        final ChatThread chatThread = newChatThread();
        logger.info("Created new chat thread (no file yet): id=`{}`, name=`{}`", chatThread.id(), chatThread.name());
        return chatThread;
    }

    public List<ChatThread> getChatThreads() {
        try (Stream<Path> files = Files.list(conversationsDir)) {
            final List<ChatThread> threads = files
                    .filter(p -> p.getFileName().toString().endsWith(".jsonl"))
                    .map(this::toChatThread)
                    .sorted(Comparator.comparing(ChatThread::modifiedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .collect(Collectors.toList());
            logger.info("Found {} total chat threads from files", threads.size());
            return threads;
        } catch (IOException e) {
            throw new RuntimeException("Failed to list conversations from: " + conversationsDir, e);
        }
    }

    public void setModifiedAt(final String chatThreadId) {
        final Path file = conversationFile(chatThreadId);
        try {
            if (Files.exists(file)) {
                Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis()));
            }
        } catch (IOException e) {
            logger.warn("Failed to touch conversation file: {}", file, e);
        }
    }

    public void deleteChatThread(final String chatThreadId) {
        try {
            Files.deleteIfExists(conversationFile(chatThreadId));
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete thread files for: " + chatThreadId, e);
        }
        chatMessageStorageRepository.deleteByThreadId(chatThreadId);
    }

    public void renameChatThread(final String chatThreadId, final String newName) {
        final Path oldFile = conversationFile(chatThreadId);
        // Do NOT create new files if conversation does not exist yet
        if (!Files.exists(oldFile)) {
            logger.info("Skipping rename; conversation file does not exist yet for id={}", chatThreadId);
            return;
        }
        final String sanitized = sanitizeFilename(newName);
        if (sanitized.isBlank()) {
            logger.warn("Skipping rename; new name is empty after sanitization");
            return;
        }
        final Path newFile = conversationsDir.resolve(sanitized + ".jsonl");
        if (Files.exists(newFile)) {
            logger.warn("Skipping rename; target file already exists: {}", newFile);
            return;
        }
        try {
            Files.move(oldFile, newFile, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to rename conversation file " + oldFile + " to " + newFile, e);
        }
    }

    private ChatThread toChatThread(final Path file) {
        final String fileName = file.getFileName().toString();
        final String id = fileName.substring(0, fileName.length() - ".jsonl".length());
        try {
            final BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            final LocalDateTime createdAt = toLocalDateTime(attrs.creationTime());
            final LocalDateTime fileModifiedAt = toLocalDateTime(attrs.lastModifiedTime());

            final LocalDateTime modifiedAt = fileModifiedAt;
            final String name = id; // title equals filename (without extension)
            return new ChatThread(id, name, createdAt, modifiedAt);
        } catch (IOException e) {
            logger.warn("Failed to read file attributes for {}", file, e);
            return new ChatThread(id, id, null, null);
        }
    }

    private static LocalDateTime toLocalDateTime(final FileTime fileTime) {
        return fileTime == null ? null : LocalDateTime.ofInstant(fileTime.toInstant(), ZoneId.systemDefault());
    }

    private Path conversationFile(final String id) {
        return conversationsDir.resolve(id + ".jsonl");
    }

    private static String sanitizeFilename(final String input) {
        String s = input == null ? "" : input.trim();
        s = s.replaceAll("[\\\\/]+", "_");
        s = s.replaceAll("[\n\r\t]", " ");
        s = s.replaceAll("[^A-Za-z0-9._ -]", "_");
        s = s.replaceAll(" +", " ");
        return s;
    }
}
