package com.patres.alina.server.message.exception;

public class CannotConvertSpeechToTextException extends RuntimeException {

    public CannotConvertSpeechToTextException(Exception cause) {
        super("Cannot convert speech to text", cause);
    }
}
