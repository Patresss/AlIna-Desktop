package com.patres.alina.common.integration;


import com.patres.alina.common.card.State;
import com.patres.alina.common.integration.exception.IntegrationFieldIsMandatoryException;

import java.util.Map;

public record IntegrationUpdateRequest(
        String id,
        String integrationType,
        String name,
        String description,
        State state,
        Map<String, Object> integrationSettings
) {

    public IntegrationUpdateRequest {
        if (id == null || id.isBlank()) {
            throw new IntegrationFieldIsMandatoryException("id");
        }
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
