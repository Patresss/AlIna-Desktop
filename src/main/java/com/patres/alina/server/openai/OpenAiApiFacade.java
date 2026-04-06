package com.patres.alina.server.openai;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.SpeechToTextErrorType;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.event.AssistantSettingsUpdatedEvent;
import com.patres.alina.server.message.exception.CannotConvertSpeechToTextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
public class OpenAiApiFacade {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiApiFacade.class);
    public static final String SPEECH_TO_TEXT_MODEL = "whisper-1";

    private final OpenAiApiCreator openAiApiCreator;
    private OpenAiAudioTranscriptionModel audioTranscriptionModel;

    public OpenAiApiFacade(FileManager<AssistantSettings> assistantSettingsManager,
                           OpenAiApiCreator openAiApiCreator) {
        this.openAiApiCreator = openAiApiCreator;
        updateAiService(assistantSettingsManager.getSettings());
        DefaultEventBus.getInstance().subscribe(AssistantSettingsUpdatedEvent.class, e -> {
            updateAiService(e.getSettings());
        });
    }

    public void updateAiService(final AssistantSettings settings) {
        try {
            audioTranscriptionModel = openAiApiCreator.createOpenAiAudioTranscriptionModel(settings.openAiApiKey(), SPEECH_TO_TEXT_MODEL);
            if (audioTranscriptionModel == null) {
                logger.warn("OpenAI audio transcription model is null - OpenAI API key not properly configured.");
            } else {
                logger.info("OpenAI audio transcription model updated successfully");
            }
        } catch (Exception e) {
            logger.error("Failed to update OpenAI audio transcription model", e);
            audioTranscriptionModel = null;
        }
    }

    public String speechToText(final File audio) {
        if (audioTranscriptionModel == null) {
            throw new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.MISSING_API_KEY,
                    "OpenAI API key not configured. Please set a valid API key in assistant settings."
            );
        }
        return audioTranscriptionModel.call(new FileSystemResource(audio));
    }
}
