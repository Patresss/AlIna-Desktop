package com.patres.alina.uidesktop.settings;

import com.patres.alina.common.settings.FileManager;
import com.patres.alina.uidesktop.common.event.QuickActionSettingsUpdateEvent;
import com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent;
import com.patres.alina.uidesktop.quickaction.QuickActionSettings;

public class SettingsMangers {

    public final static FileManager<UiSettings> UI_SETTINGS = new FileManager<>("ui", UiSettings.class, UiSettings::new, UiSettingsUpdateEvent::new);
    public final static FileManager<QuickActionSettings> QUICK_ACTION_SETTINGS = new FileManager<>("quick-actions", QuickActionSettings.class, QuickActionSettings::new, QuickActionSettingsUpdateEvent::new);

}
