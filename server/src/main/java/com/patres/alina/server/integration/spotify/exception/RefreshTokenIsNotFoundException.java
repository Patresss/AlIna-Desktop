package com.patres.alina.server.integration.spotify.exception;

public class RefreshTokenIsNotFoundException extends RuntimeException {

    public RefreshTokenIsNotFoundException(String integrationId) {
        super("Refresh token is nto found, integration id: " + integrationId);
    }
}
