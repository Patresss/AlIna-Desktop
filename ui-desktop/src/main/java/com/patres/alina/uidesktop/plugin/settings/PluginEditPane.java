package com.patres.alina.uidesktop.plugin.settings;

import atlantafx.base.controls.ToggleSwitch;
import com.patres.alina.common.plugin.PluginDetail;
import com.patres.alina.common.card.State;
import com.patres.alina.uidesktop.backend.AlinaRestApi;
import com.patres.alina.uidesktop.common.event.PluginUpdateEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.ui.atlantafx.CustomTile;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.stream.Stream;

import static com.patres.alina.uidesktop.plugin.settings.IconComboBox.CONVERTER;
import static com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableTextField;

public class PluginEditPane extends PluginSavePane {

    private final PluginDetail pluginDetail;

    private TextField pluginIdTextField;
    private ToggleSwitch stateToggleSwitch;

    public PluginEditPane(Runnable backFunction, PluginDetail pluginDetail) {
        super(backFunction);
        this.pluginDetail = pluginDetail;
        reload();
    }

    @Override
    protected void savePlugin() {
        editPlugin();
    }

    private void editPlugin() {
        final PluginDetail pluginEditRequest = new PluginDetail(
                pluginDetail.id(),
                pluginNameTextField.getText(),
                pluginDescriptionTextArea.getText(),
                pluginSystemPromptTextArea.getText(),
                iconComboBox.getValue().getObject().name(),
                stateToggleSwitch.isSelected() ? State.ENABLED : State.DISABLED
        );
        AlinaRestApi.getAlinaRestClient().updatePluginDetail(pluginEditRequest);
        backFunction.run();

        DefaultEventBus.getInstance().publish(new PluginUpdateEvent(PluginUpdateEvent.EventType.PLUGIN_UPDATED));
    }

    @Override
    protected List<Node> generateContent() {
        var pluginIdTile = new CustomTile(
                getLanguageString("plugin.id.title"),
                null
        );
        pluginIdTextField = createResizableTextField(settingsBox);
        pluginIdTile.setAction(pluginIdTextField);
        pluginIdTextField.setEditable(false);

        var stateTile = new CustomTile(
                getLanguageString("plugin.state.title"),
                null
        );
        stateToggleSwitch = new ToggleSwitch();
        stateTile.setAction(stateToggleSwitch);
        stateTile.setActionHandler(stateToggleSwitch::fire);


        final List<Node> pluginContent = super.generateContent();
        final List<Node> editPluginContent = List.of(pluginIdTile, stateTile, new Separator());
        return Stream.concat(editPluginContent.stream(), pluginContent.stream()).toList();
    }

    @Override
    public void reload() {
        pluginIdTextField.setText(pluginDetail.id());
        stateToggleSwitch.setSelected(pluginDetail.state() == State.ENABLED);
        pluginNameTextField.setText(pluginDetail.name());
        pluginDescriptionTextArea.setText(pluginDetail.description());
        pluginSystemPromptTextArea.setText(pluginDetail.systemPrompt());
        iconComboBox.getSelectionModel().select(CONVERTER.fromString(pluginDetail.icon()));
    }

}
