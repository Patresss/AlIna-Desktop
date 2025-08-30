package com.patres.alina.uidesktop.integration.settings.exception;

public class CannotSaveIntegrationDueToAuthLink extends RuntimeException {

    public CannotSaveIntegrationDueToAuthLink(String link) {
        super("Cannot save integration due to auth link: " + link);
    }

    public CannotSaveIntegrationDueToAuthLink(String link, String reason) {
        super("Cannot save integration due to auth link: " + link + ". Reason: " + reason);
    }

    public CannotSaveIntegrationDueToAuthLink(String link, Throwable cause) {
        super("Cannot save integration due to auth link: " + link, cause);
    }
}
