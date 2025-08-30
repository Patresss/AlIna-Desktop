package com.patres.alina.common.integration;

import com.patres.alina.common.field.UiForm;

public record IntegrationToAdd(
        String integrationType,
        String defaultName,
        String defaultDescription,
        String icon,
        UiForm form
) implements IntegrationToSave {

    @Override
    public String name() {
        return defaultName;
    }

    @Override
    public String description() {
        return defaultDescription;
    }
}
