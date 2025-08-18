package com.patres.alina.server.integration.exception;

public class IntegrationSettingsIsInvalidException extends RuntimeException {

    public IntegrationSettingsIsInvalidException(String key, String reason) {
        super("Integration setting '" + key + "' is invalid. Reason: " + reason);
    }
}
