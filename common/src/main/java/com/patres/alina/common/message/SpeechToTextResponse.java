package com.patres.alina.common.message;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public record SpeechToTextResponse(
        String content
) {
}
