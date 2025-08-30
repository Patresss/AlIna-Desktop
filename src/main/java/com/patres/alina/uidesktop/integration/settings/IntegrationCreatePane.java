package com.patres.alina.uidesktop.integration.settings;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.integration.IntegrationCreateRequest;
import com.patres.alina.common.integration.IntegrationSaved;
import com.patres.alina.common.integration.IntegrationToAdd;
import com.patres.alina.uidesktop.common.event.IntegrationUpdateEvent;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class IntegrationCreatePane extends IntegrationSavePane<IntegrationToAdd> {


    public IntegrationCreatePane(Runnable backFunction, IntegrationToAdd integrationToAdd) {
        super(backFunction, integrationToAdd);
    }

//    @Override
//    public void reload() {
//        nameTextField.clear();
//        descriptionTextArea.clear();
//        iconComboBox.getSelectionModel().select(CONVERTER.fromString(BootstrapIcons.PLUG.getDescription()));
//    }

    @Override
    protected void saveIntegration() {
        createIntegration();
    }

    private void createIntegration() {
        IntegrationCreateRequest createRequest = new IntegrationCreateRequest(
                integrationToSave.integrationType(),
                nameTextField.getText(),
                descriptionTextArea.getText(),
                getIntegrationSettings()
        );

        final IntegrationSaved integration = null;//AlinaRestApi.getAlinaRestClient().createIntegration(createRequest);
        if (isNotBlank(integration.additionalStepAuthorizationUrl())) {
            openWebpage(integration.additionalStepAuthorizationUrl());
        }
        backFunction.run();
        DefaultEventBus.getInstance().publish(new IntegrationUpdateEvent(IntegrationUpdateEvent.EventType.INTEGRATION_ADDED));
    }






}
