package com.patres.alina.uidesktop.common.settings;

import com.patres.alina.uidesktop.shortcuts.key.KeyboardKey;

import java.util.List;
import java.util.Map;

public class ChatGptAction {

    private String name;
    private String prompt;
    private Map<ContextMenuResultType, List<KeyboardKey>> shortcuts;
    private boolean enabled;

    public ChatGptAction() {
    }

    public ChatGptAction(String name, String prompt, Map<ContextMenuResultType, List<KeyboardKey>> shortcuts, boolean enabled) {
        this.name = name;
        this.prompt = prompt;
        this.shortcuts = shortcuts;
        this.enabled = enabled;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public Map<ContextMenuResultType, List<KeyboardKey>> getShortcuts() {
        return shortcuts;
    }

    public void setShortcuts(Map<ContextMenuResultType, List<KeyboardKey>> shortcuts) {
        this.shortcuts = shortcuts;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
