package com.patres.alina.server.integration.spotify.exception;

public class CannotExecuteSpotifyActionException extends RuntimeException {

    public CannotExecuteSpotifyActionException(Throwable cause) {
        super("Cannot execute a Spotify action", cause);
    }
}
