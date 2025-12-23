package com.patres.alina.common.message;

public enum SpeechToTextErrorType {
    MISSING_API_KEY("chat.speech.error.missingKey"),
    TIMEOUT("chat.speech.error.timeout"),
    INVALID_FORMAT("chat.speech.error.format"),
    EMPTY_AUDIO("chat.speech.error.empty"),
    UNKNOWN("chat.speech.error.unknown");

    private final String i18nKey;

    SpeechToTextErrorType(String i18nKey) {
        this.i18nKey = i18nKey;
    }

    public String getI18nKey() {
        return i18nKey;
    }
}
