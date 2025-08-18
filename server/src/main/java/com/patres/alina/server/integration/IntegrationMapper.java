package com.patres.alina.server.integration;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.State;
import com.patres.alina.common.field.FormField;
import com.patres.alina.common.field.UiForm;
import com.patres.alina.common.integration.*;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationType;

import java.util.List;

public class IntegrationMapper {

    public static IntegrationToAdd toIntegrationToAdd(final AlinaIntegrationType<?, ?> integration) {
        return new IntegrationToAdd(
                integration.getTypeName(),
                integration.getDefaultNameToDisplay(),
                integration.getDefaultDescription(),
                integration.getIcon(),
                integration.getUiForm()
        );
    }

    public static CardListItem toIntegrationListItem(final Integration integration, final String icon) {
        return new CardListItem(
                integration.id(),
                integration.name(),
                integration.description(),
                icon,
                integration.state()
        );
    }

    public static IntegrationDetails toIntegrationDetails(final Integration integration, final AlinaIntegrationType<?, ?> integrationType) {
        return new IntegrationDetails(
                integration.id(),
                integration.integrationType(),
                integration.name(),
                integrationType.getDefaultNameToDisplay(),
                integration.description(),
                integrationType.getDefaultDescription(),
                integration.state(),
                toUiForm(integration, integrationType)
        );
    }

    public static IntegrationDetails toIntegrationDetails(final AlinaIntegrationType<?, ?> integrationType) {
        return new IntegrationDetails(
                null,
                integrationType.getTypeName(),
                integrationType.getDefaultNameToDisplay(),
                integrationType.getDefaultNameToDisplay(),
                integrationType.getDefaultDescription(),
                integrationType.getDefaultDescription(),
                State.ENABLED,
                integrationType.getUiForm()
        );
    }

    public static Integration toIntegration(final IntegrationCreateRequest integrationCreateRequest) {
        return new Integration(
                null,
                integrationCreateRequest.integrationType(),
                integrationCreateRequest.name(),
                integrationCreateRequest.description(),
                State.ENABLED,
                integrationCreateRequest.integrationSettings()
        );
    }

    public static Integration toIntegration(final IntegrationUpdateRequest integrationUpdateRequest) {
        return new Integration(
                integrationUpdateRequest.id(),
                integrationUpdateRequest.integrationType(),
                integrationUpdateRequest.name(),
                integrationUpdateRequest.description(),
                integrationUpdateRequest.state(),
                integrationUpdateRequest.integrationSettings()
        );
    }

    public static IntegrationUpdateRequest toIntegrationUpdateRequest(final Integration integration) {
        return new IntegrationUpdateRequest(
                integration.id(),
                integration.integrationType(),
                integration.name(),
                integration.description(),
                integration.state(),
                integration.integrationSettings()
        );
    }


    private static UiForm toUiForm(final Integration integration, final AlinaIntegrationType<?, ?> integrationType) {
        List<FormField> formFields = integrationType.getUiForm().formFields().stream()
                .map(form -> new FormField(form, integration.integrationSettings().get(form.id())))
                .toList();
        return new UiForm(formFields);
    }

}
