package com.patres.alina.common.integration;


import com.patres.alina.common.card.State;
import com.patres.alina.common.field.UiForm;

public record IntegrationDetails(
        String id,
        String integrationType,
        String name,
        String defaultName,
        String description,
        String defaultDescription,
        State state,
        UiForm form
) implements IntegrationToSave {
}
