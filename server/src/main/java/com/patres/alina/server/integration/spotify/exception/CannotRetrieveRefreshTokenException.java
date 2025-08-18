package com.patres.alina.server.integration.spotify.exception;

public class CannotRetrieveRefreshTokenException extends RuntimeException {

    public CannotRetrieveRefreshTokenException(Throwable cause) {
        super("Cannot retrieve refresh token", cause);
    }
}
