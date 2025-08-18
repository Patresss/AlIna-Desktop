package com.patres.alina.common.message;

import java.util.Arrays;

public enum ChatMessageRole {

    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant"),
    FUNCTION("function");

    private final String chatMessageRole;

    ChatMessageRole(String chatMessageRole) {
        this.chatMessageRole = chatMessageRole;
    }

    public String getChatMessageRole() {
        return chatMessageRole;
    }

    public static ChatMessageRole findChatMessageRoleByValue(String chatMessageRole) {
        return Arrays.stream(values())
                .filter(type -> type.getChatMessageRole().equals(chatMessageRole))
                .findFirst()
                .orElse(null);
    }
}
