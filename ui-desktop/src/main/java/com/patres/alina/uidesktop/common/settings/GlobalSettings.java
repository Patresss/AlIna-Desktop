package com.patres.alina.uidesktop.common.settings;

import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;

import java.util.List;
import java.util.Map;

public class GlobalSettings {

    private List<KeyboardKey> contextMenuKeys = List.of(KeyboardKey.CONTROL, KeyboardKey.BACK_QUOTE);
    private List<ChatGptAction> chatGptActions = List.of(
            new ChatGptAction("Zapytaj ChatPGT", "", Map.of(), true),
            new ChatGptAction("Przetłumacz na język polski", "Przetłumacz na język polski", Map.of(ContextMenuResultType.COPY, List.of(KeyboardKey.ALT, KeyboardKey.Q)), true),
            new ChatGptAction("Przetłumacz na język angielski", "Przetłumacz na język angielski", Map.of(), true),
            new ChatGptAction("Sparafrazuj tekst", "Sparafrazuj tekst", Map.of(), true),
            new ChatGptAction("Test", "Test 123", Map.of(), false)
    );

    public List<KeyboardKey> getContextMenuKeys() {
        return contextMenuKeys;
    }

    public void setContextMenuKeys(List<KeyboardKey> contextMenuKeys) {
        this.contextMenuKeys = contextMenuKeys;
    }

    public List<ChatGptAction> getChatGptActions() {
        return chatGptActions;
    }

    public void setChatGptActions(List<ChatGptAction> chatGptActions) {
        this.chatGptActions = chatGptActions;
    }
}
