package com.patres.alina.uidesktop.quickaction.settings;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import com.patres.alina.uidesktop.quickaction.QuickActionSettings;
import com.patres.alina.uidesktop.quickaction.QuickActionType;
import com.patres.alina.uidesktop.settings.ui.SettingsModalPaneContent;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import javafx.scene.Node;
import javafx.scene.control.Separator;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.patres.alina.uidesktop.settings.SettingsMangers.QUICK_ACTION_SETTINGS;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTile;

public class QuickActionSettingsPane extends SettingsModalPaneContent {

    private Map<QuickActionType, ToggleSwitch> toggleSwitches;
    private QuickActionSettings quickActionSettings;

    public QuickActionSettingsPane(Runnable backFunction) {
        super(backFunction);
    }

    @Override
    protected void reset() {
        loadDataFromSettings();
        for (QuickActionType type : QuickActionType.values()) {
            ToggleSwitch toggle = toggleSwitches.get(type);
            if (toggle != null) {
                toggle.setSelected(quickActionSettings.isEnabled(type));
            }
        }
    }

    @Override
    protected void save() {
        QuickActionSettings settings = quickActionSettings;
        for (QuickActionType type : QuickActionType.values()) {
            ToggleSwitch toggle = toggleSwitches.get(type);
            if (toggle != null) {
                settings = settings.withEnabled(type, toggle.isSelected());
            }
        }
        QUICK_ACTION_SETTINGS.saveDocument(settings);
    }

    private void loadDataFromSettings() {
        quickActionSettings = QUICK_ACTION_SETTINGS.getSettings();
    }

    @Override
    protected List<Node> generateContent() {
        loadDataFromSettings();
        toggleSwitches = new EnumMap<>(QuickActionType.class);

        var header = createTextSeparator("quickaction.settings.title", Styles.TITLE_3);
        var description = createTextSeparator("quickaction.settings.description", Styles.TEXT_MUTED);

        List<Node> nodes = new ArrayList<>();
        nodes.add(header);
        nodes.add(description);
        nodes.add(new Separator());

        for (QuickActionType type : QuickActionType.values()) {
            var toggle = new ToggleSwitch();
            toggle.setSelected(quickActionSettings.isEnabled(type));
            toggleSwitches.put(type, toggle);

            var tile = createTile(
                    type.getTitleKey(),
                    type.getDescriptionKey()
            );
            tile.setAction(toggle);
            nodes.add(tile);
        }

        return nodes;
    }
}
