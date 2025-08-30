package com.patres.alina.common.integration;


import com.patres.alina.common.integration.exception.IntegrationFieldIsMandatoryException;

import java.util.Map;

public record IntegrationCreateRequest(
        String integrationType,
        String name,
        String description,
        Map<String, Object> integrationSettings
) {

    public IntegrationCreateRequest {
        if (integrationType == null || integrationType.isBlank()) {
            throw new IntegrationFieldIsMandatoryException("Integration Type");
        }
        if (name == null || name.isBlank()) {
            throw new IntegrationFieldIsMandatoryException("name");
        }
        if (description == null || description.isBlank()) {
            throw new IntegrationFieldIsMandatoryException("description");
        }
    }
}
