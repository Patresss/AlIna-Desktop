package com.patres.alina.server.integration.exception;

public class IntegrationTypeNotFoundException extends RuntimeException {

    public IntegrationTypeNotFoundException(String integrationType) {
        super("Integration type '" + integrationType + "' not found");
    }
}
