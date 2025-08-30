package com.patres.alina.common.integration;

public record IntegrationSaved(
        String integrationId,
        String additionalStepAuthorizationUrl
) {

    public IntegrationSaved(String integrationId) {
        this(integrationId, null);
    }

    @Override
    public String integrationId() {
        return integrationId;
    }

    @Override
    public String additionalStepAuthorizationUrl() {
        return additionalStepAuthorizationUrl;
    }
}
