package com.patres.alina.uidesktop.quickaction;

public enum QuickActionType {

    NEW_CHAT("new", "quickaction.new.title", "quickaction.new.description", "mdal-add_comment"),
    MODELS("models", "quickaction.models.title", "quickaction.models.description", "mdmz-model_training"),
    HISTORY("history", "quickaction.history.title", "quickaction.history.description", "mdal-history"),
    COMMANDS("commands", "quickaction.commands.title", "quickaction.commands.description", "fth-terminal"),
    UI_SETTINGS("ui-settings", "quickaction.uiSettings.title", "quickaction.uiSettings.description", "mdmz-palette"),
    ASSISTANT_SETTINGS("assistant-settings", "quickaction.assistantSettings.title", "quickaction.assistantSettings.description", "mdmz-psychology"),
    DASHBOARD_SETTINGS("dashboard-settings", "quickaction.dashboardSettings.title", "quickaction.dashboardSettings.description", "mdal-dashboard"),
    OPENCODE_SETTINGS("opencode-settings", "quickaction.opencodeSettings.title", "quickaction.opencodeSettings.description", "mdal-code");

    private final String id;
    private final String titleKey;
    private final String descriptionKey;
    private final String icon;

    QuickActionType(String id, String titleKey, String descriptionKey, String icon) {
        this.id = id;
        this.titleKey = titleKey;
        this.descriptionKey = descriptionKey;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public String getTitleKey() {
        return titleKey;
    }

    public String getDescriptionKey() {
        return descriptionKey;
    }

    public String getIcon() {
        return icon;
    }

    public static QuickActionType fromId(String id) {
        for (QuickActionType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
