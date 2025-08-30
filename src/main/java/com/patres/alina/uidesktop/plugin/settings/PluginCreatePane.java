package com.patres.alina.uidesktop.plugin.settings;

import com.patres.alina.common.plugin.PluginCreateRequest;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.common.event.PluginUpdateEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;

import static com.patres.alina.uidesktop.plugin.settings.IconComboBox.CONVERTER;

public class PluginCreatePane extends PluginSavePane {

    public PluginCreatePane(Runnable backFunction) {
        super(backFunction);
    }

    @Override
    public void reload() {
        pluginNameTextField.clear();
        pluginDescriptionTextArea.clear();
        pluginSystemPromptTextArea.clear();
        iconComboBox.getSelectionModel().select(CONVERTER.fromString(BootstrapIcons.PLUG.getDescription()));
    }

    @Override
    protected void savePlugin() {
        createPlugin();
    }

    private void createPlugin() {
        PluginCreateRequest pluginCreateRequest = new PluginCreateRequest(
                pluginNameTextField.getText(),
                pluginDescriptionTextArea.getText(),
                pluginSystemPromptTextArea.getText(),
                iconComboBox.getValue().getObject().name()
        );

        BackendApi.createPluginDetail(pluginCreateRequest);
        backFunction.run();
        DefaultEventBus.getInstance().publish(new PluginUpdateEvent(PluginUpdateEvent.EventType.PLUGIN_ADDED));
    }

}
