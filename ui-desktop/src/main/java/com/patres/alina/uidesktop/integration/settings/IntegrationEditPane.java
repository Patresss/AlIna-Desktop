package com.patres.alina.uidesktop.integration.settings;

import atlantafx.base.controls.ToggleSwitch;
import com.patres.alina.common.card.State;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.integration.IntegrationCreateRequest;
import com.patres.alina.common.integration.IntegrationDetails;
import com.patres.alina.common.integration.IntegrationSaved;
import com.patres.alina.common.integration.IntegrationUpdateRequest;
import com.patres.alina.common.plugin.PluginDetail;
import com.patres.alina.uidesktop.backend.AlinaRestApi;
import com.patres.alina.uidesktop.common.event.IntegrationUpdateEvent;
import com.patres.alina.uidesktop.common.event.PluginUpdateEvent;
import com.patres.alina.uidesktop.ui.atlantafx.CustomTile;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.stream.Stream;

import static com.patres.alina.uidesktop.plugin.settings.IconComboBox.CONVERTER;
import static com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableTextField;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class IntegrationEditPane extends IntegrationSavePane<IntegrationDetails> {

    private TextField integrationIdTextField;
    private ToggleSwitch stateToggleSwitch;

    public IntegrationEditPane(Runnable backFunction, IntegrationDetails integration) {
        super(backFunction, integration);
        reload();
    }

    @Override
    protected List<Node> generateContent() {
        var idTile = new CustomTile(
                getLanguageString("integration.id.title"),
                null
        );
        integrationIdTextField = createResizableTextField(settingsBox);
        idTile.setAction(integrationIdTextField);
        integrationIdTextField.setEditable(false);

        var stateTile = new CustomTile(
                getLanguageString("integration.state.title"),
                null
        );
        stateToggleSwitch = new ToggleSwitch();
        stateTile.setAction(stateToggleSwitch);
        stateTile.setActionHandler(stateToggleSwitch::fire);


        final List<Node> integrationContent = super.generateContent();
        final List<Node> editIntegrationContent = List.of(idTile, stateTile, new Separator());
        return Stream.concat(editIntegrationContent.stream(), integrationContent.stream())
                .toList();
    }

    @Override
    public void reload() {
        super.reload();
        integrationIdTextField.setText(integrationToSave.id());
        stateToggleSwitch.setSelected(integrationToSave.state() == State.ENABLED);
    }

    @Override
    protected void saveIntegration() {
        validateMandatoryFields();
        createIntegration();
    }

    private void validateMandatoryFields() {

    }

    private void createIntegration() {
        IntegrationUpdateRequest integrationUpdateRequest = new IntegrationUpdateRequest(
                integrationToSave.id(),
                integrationToSave.integrationType(),
                nameTextField.getText(),
                descriptionTextArea.getText(),
                stateToggleSwitch.isSelected() ? State.ENABLED : State.DISABLED,
                getIntegrationSettings()
        );
        final IntegrationSaved integration = AlinaRestApi.getAlinaRestClient().updateIntegration(integrationUpdateRequest);
        if (isNotBlank(integration.additionalStepAuthorizationUrl())) {
            openWebpage(integration.additionalStepAuthorizationUrl());
        }

        backFunction.run();
        DefaultEventBus.getInstance().publish(new IntegrationUpdateEvent(IntegrationUpdateEvent.EventType.INTEGRATION_ADDED));
    }


}
