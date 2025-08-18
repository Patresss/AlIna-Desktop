package com.patres.alina.common.integration;

import com.patres.alina.common.field.UiForm;

public interface IntegrationToSave {
    String integrationType();
    String name();
    String description();
    String defaultDescription();
    UiForm form();
}

