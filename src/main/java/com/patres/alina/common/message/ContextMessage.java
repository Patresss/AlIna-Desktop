package com.patres.alina.common.message;

/**
 * Simple message container used for building conversation context
 * sent to the AI backend (OpenCode). Replaces Spring AI AbstractMessage hierarchy.
 */
public record ContextMessage(
        String text,
        ChatMessageRole role
) {

    public static ContextMessage user(final String text) {
        return new ContextMessage(text, ChatMessageRole.USER);
    }

    public static ContextMessage assistant(final String text) {
        return new ContextMessage(text, ChatMessageRole.ASSISTANT);
    }

    public static ContextMessage system(final String text) {
        return new ContextMessage(text, ChatMessageRole.SYSTEM);
    }
}
