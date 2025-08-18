package com.patres.alina.server.speech;

import com.patres.alina.common.message.SpeechToTextResponse;
import com.patres.alina.server.message.exception.CannotConvertSpeechToTextException;
import com.patres.alina.server.openai.OpenAiApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@Service
public class SpeechService {

    private static final Logger logger = LoggerFactory.getLogger(SpeechService.class);

    private final OpenAiApi openAiApi;

    public SpeechService(final OpenAiApi openAiApi) {
        this.openAiApi = openAiApi;
    }

    public SpeechToTextResponse speechToText(byte[] audio) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("audio", ".wav");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(audio);
            }
            return new SpeechToTextResponse(openAiApi.speechToText(tempFile));
        } catch (IOException e) {
            logger.error("Cannot convert speech to text", e);
            throw new CannotConvertSpeechToTextException(e);
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }


}