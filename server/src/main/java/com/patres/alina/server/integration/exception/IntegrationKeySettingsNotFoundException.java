package com.patres.alina.server.integration.exception;

public class IntegrationKeySettingsNotFoundException extends RuntimeException {

    public IntegrationKeySettingsNotFoundException(String key) {
        super("Integration key settings '" + key + "' not found");
    }
}
