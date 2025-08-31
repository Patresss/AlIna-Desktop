package com.patres.alina.server.speech;

import com.patres.alina.common.message.SpeechToTextResponse;
import org.springframework.stereotype.Component;

@Component
public class SpeechToTextController {

    private final SpeechService speechService;

    public SpeechToTextController(SpeechService speechService) {
        this.speechService = speechService;
    }

    public SpeechToTextResponse speechToText(byte[] bytes) {
        return speechService.speechToText(bytes);
    }

}
