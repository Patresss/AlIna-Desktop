package com.patres.alina.server.speech;

import com.patres.alina.common.message.SpeechToTextErrorType;
import com.patres.alina.common.message.SpeechToTextResponse;
import com.patres.alina.common.settings.AssistantSettings;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.server.message.exception.CannotConvertSpeechToTextException;
import com.patres.alina.server.openai.OpenAiApiFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.ai.openai.api.common.OpenAiApiClientErrorException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class SpeechService {

    private static final Logger logger = LoggerFactory.getLogger(SpeechService.class);

    private final OpenAiApiFacade openAiApi;
    private final FileManager<AssistantSettings> assistantSettingsManager;
    private final ExecutorService transcriptionExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public SpeechService(final OpenAiApiFacade openAiApi,
                         final FileManager<AssistantSettings> assistantSettingsManager) {
        this.openAiApi = openAiApi;
        this.assistantSettingsManager = assistantSettingsManager;
    }

    public SpeechToTextResponse speechToText(byte[] audio) {
        File tempFile = null;
        try {
            validateAudio(audio);
            tempFile = File.createTempFile("audio", ".wav");
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(audio);
            }
            int timeoutSeconds = Math.max(1, assistantSettingsManager.getSettings().timeoutSeconds());
            File audioFile = tempFile;
            String transcription = callWithTimeout(() -> openAiApi.speechToText(audioFile), timeoutSeconds);
            return new SpeechToTextResponse(transcription);
        } catch (CannotConvertSpeechToTextException e) {
            throw e;
        } catch (UnsupportedAudioFileException e) {
            throw new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.INVALID_FORMAT,
                    "Unsupported audio format.",
                    e
            );
        } catch (TimeoutException e) {
            throw new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.TIMEOUT,
                    "Speech transcription timed out.",
                    e
            );
        } catch (ExecutionException e) {
            throw mapExecutionException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.TIMEOUT,
                    "Speech transcription interrupted.",
                    e
            );
        } catch (IOException e) {
            logger.error("Cannot convert speech to text", e);
            throw new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.UNKNOWN,
                    "Cannot convert speech to text.",
                    e
            );
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
    }

    @PreDestroy
    public void shutdownExecutor() {
        transcriptionExecutor.shutdown();
        try {
            if (!transcriptionExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                transcriptionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            transcriptionExecutor.shutdownNow();
        }
    }

    private void validateAudio(byte[] audio) throws UnsupportedAudioFileException, IOException {
        if (audio == null || audio.length == 0) {
            throw new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.EMPTY_AUDIO,
                    "No audio data provided."
            );
        }
        try (AudioInputStream ignored = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audio))) {
            // Validation only: ensure audio format can be read.
        }
    }

    private String callWithTimeout(Callable<String> task, int timeoutSeconds)
            throws TimeoutException, ExecutionException, InterruptedException {
        Future<String> future = transcriptionExecutor.submit(task);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        }
    }

    private CannotConvertSpeechToTextException mapExecutionException(ExecutionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof CannotConvertSpeechToTextException exception) {
            return exception;
        }
        if (cause instanceof HttpClientErrorException clientError) {
            return mapHttpStatus(clientError.getStatusCode().value(), clientError);
        }
        if (cause instanceof WebClientResponseException responseException) {
            return mapHttpStatus(responseException.getStatusCode().value(), responseException);
        }
        if (cause instanceof OpenAiApiClientErrorException clientErrorException) {
            return mapClientErrorMessage(clientErrorException.getMessage(), clientErrorException);
        }
        if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
            return new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.TIMEOUT,
                    "Speech transcription timed out.",
                    cause
            );
        }
        return new CannotConvertSpeechToTextException(
                SpeechToTextErrorType.UNKNOWN,
                "Cannot convert speech to text.",
                cause
        );
    }

    private CannotConvertSpeechToTextException mapClientErrorMessage(String message, Throwable cause) {
        if (message == null) {
            return new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.UNKNOWN,
                    "Cannot convert speech to text.",
                    cause
            );
        }
        String normalized = message.toLowerCase();
        if (normalized.contains("401") || normalized.contains("unauthorized") || normalized.contains("api key")) {
            return new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.MISSING_API_KEY,
                    "OpenAI API key not configured.",
                    cause
            );
        }
        if (normalized.contains("415") || normalized.contains("unsupported") || normalized.contains("format")) {
            return new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.INVALID_FORMAT,
                    "Unsupported audio format.",
                    cause
            );
        }
        return new CannotConvertSpeechToTextException(
                SpeechToTextErrorType.UNKNOWN,
                "Cannot convert speech to text.",
                cause
        );
    }

    private CannotConvertSpeechToTextException mapHttpStatus(int statusCode, Exception e) {
        if (statusCode == HttpStatus.UNAUTHORIZED.value() || statusCode == HttpStatus.FORBIDDEN.value()) {
            return new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.MISSING_API_KEY,
                    "OpenAI API key not configured.",
                    e
            );
        }
        if (statusCode == HttpStatus.BAD_REQUEST.value() || statusCode == HttpStatus.UNSUPPORTED_MEDIA_TYPE.value()) {
            return new CannotConvertSpeechToTextException(
                    SpeechToTextErrorType.INVALID_FORMAT,
                    "Unsupported audio format.",
                    e
            );
        }
        return new CannotConvertSpeechToTextException(
                SpeechToTextErrorType.UNKNOWN,
                "Cannot convert speech to text.",
                e
        );
    }

}
