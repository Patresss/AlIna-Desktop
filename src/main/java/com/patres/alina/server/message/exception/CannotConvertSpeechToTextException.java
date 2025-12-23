package com.patres.alina.server.message.exception;

import com.patres.alina.common.message.SpeechToTextErrorType;

public class CannotConvertSpeechToTextException extends RuntimeException {

    private final SpeechToTextErrorType errorType;

    public CannotConvertSpeechToTextException(Exception cause) {
        super("Cannot convert speech to text", cause);
        this.errorType = SpeechToTextErrorType.UNKNOWN;
    }

    public CannotConvertSpeechToTextException(SpeechToTextErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public CannotConvertSpeechToTextException(SpeechToTextErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public SpeechToTextErrorType getErrorType() {
        return errorType;
    }
}
