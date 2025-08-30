package com.patres.alina.uidesktop.settings;

import com.patres.alina.common.settings.FileManager;
import com.patres.alina.uidesktop.common.event.UiSettingsUpdateEvent;

public class SettingsMangers {

    public final static FileManager<UiSettings> UI_SETTINGS = new FileManager<>("UI Settings", UiSettings.class, UiSettings::new, UiSettingsUpdateEvent::new);
    public final static FileManager<ServerSettings> SERVER_SETTINGS = new FileManager<>("Server Settings", ServerSettings.class, ServerSettings::new);

}