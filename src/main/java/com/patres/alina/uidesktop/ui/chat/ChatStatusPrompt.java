package com.patres.alina.uidesktop.ui.chat;

import javafx.scene.control.TextArea;

public class ChatStatusPrompt {

    private final TextArea chatTextArea;
    private String basePromptText = "";
    private boolean showingStatusPrompt;

    public ChatStatusPrompt(TextArea chatTextArea) {
        this.chatTextArea = chatTextArea;
    }

    public void setBasePromptText(String text) {
        basePromptText = text == null ? "" : text;
        if (!showingStatusPrompt) {
            chatTextArea.setPromptText(basePromptText);
        }
    }

    public void showStatusPrompt(String text) {
        showingStatusPrompt = true;
        chatTextArea.clear();
        chatTextArea.setPromptText(text == null ? "" : text);
    }

    public void clearStatusPrompt() {
        showingStatusPrompt = false;
        chatTextArea.setPromptText(basePromptText);
    }
}
