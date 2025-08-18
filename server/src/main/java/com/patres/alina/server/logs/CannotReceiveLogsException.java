package com.patres.alina.server.logs;

public class CannotReceiveLogsException extends RuntimeException {

    public CannotReceiveLogsException(Throwable cause) {
        super("Cannot receive logs", cause);
    }
}
