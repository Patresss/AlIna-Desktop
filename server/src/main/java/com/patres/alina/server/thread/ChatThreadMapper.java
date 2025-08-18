package com.patres.alina.server.thread;

import com.patres.alina.common.thread.ChatThreadResponse;

import java.util.List;

public final class ChatThreadMapper {

    public static ChatThreadResponse toChatThreadResponse(final ChatThread chatThread) {
        return new ChatThreadResponse(
                chatThread.id(),
                chatThread.name(),
                chatThread.createdAt(),
                chatThread.modifiedAt());
    }

    public static List<ChatThreadResponse> toChatThreadResponses(final List<ChatThread> chatThreads) {
        return chatThreads.stream()
                .map(ChatThreadMapper::toChatThreadResponse)
                .toList();
    }

}
