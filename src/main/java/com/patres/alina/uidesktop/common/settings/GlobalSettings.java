package com.patres.alina.uidesktop.common.settings;

import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;

import java.util.List;
import java.util.Map;

public class GlobalSettings {

    private List<ChatGptAction> chatGptActions = List.of(
            new ChatGptAction("Ask ChatGPT", "", Map.of(), true),
            new ChatGptAction("Translate to Polish", "Translate to Polish", Map.of(ContextMenuResultType.COPY, List.of(KeyboardKey.ALT, KeyboardKey.Q)), true),
            new ChatGptAction("Translate to English", "Translate to English", Map.of(), true),
            new ChatGptAction("Paraphrase text", "Paraphrase text", Map.of(), true),
            new ChatGptAction("Test", "Test 123", Map.of(), false)
    );

    public List<ChatGptAction> getChatGptActions() {
        return chatGptActions;
    }

    public void setChatGptActions(List<ChatGptAction> chatGptActions) {
        this.chatGptActions = chatGptActions;
    }
}
