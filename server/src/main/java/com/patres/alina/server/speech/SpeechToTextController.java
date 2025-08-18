package com.patres.alina.server.speech;

import com.patres.alina.common.message.SpeechToTextResponse;
import com.patres.alina.server.message.exception.CannotConvertSpeechToTextException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping(path = "/speech")
public class SpeechToTextController {

    private final SpeechService speechService;

    public SpeechToTextController(SpeechService speechService) {
        this.speechService = speechService;
    }

    @PostMapping
    public SpeechToTextResponse speechToText(@RequestParam("file") MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            return speechService.speechToText(bytes);
        } catch (IOException e) {
            throw new CannotConvertSpeechToTextException(e);
        }
    }


}
