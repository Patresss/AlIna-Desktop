package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.message.SpeechToTextErrorType;
import com.patres.alina.server.message.exception.CannotConvertSpeechToTextException;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.microphone.AudioRecorder;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.util.FxThreadRunner;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class ChatRecordingController {

    private static final Logger logger = LoggerFactory.getLogger(ChatRecordingController.class);
    private static final String RECORDING_STYLE_CLASS = "recording-active";

    private final Button recordButton;
    private final TextArea chatTextArea;
    private final List<Node> actionNodes;
    private final List<Node> nonRecordingActionNodes;
    private final AudioRecorder audioRecorder;
    private final ChatStatusPrompt statusPrompt;
    private final Consumer<String> onTranscriptionReady;
    private final Consumer<String> onErrorMessage;

    private RecordMode recordMode = RecordMode.PREPARE_TO_START_RECORDING;

    public ChatRecordingController(Button recordButton,
                                   TextArea chatTextArea,
                                   List<Node> actionNodes,
                                   List<Node> nonRecordingActionNodes,
                                   AudioRecorder audioRecorder,
                                   ChatStatusPrompt statusPrompt,
                                   Consumer<String> onTranscriptionReady,
                                   Consumer<String> onErrorMessage) {
        this.recordButton = recordButton;
        this.chatTextArea = chatTextArea;
        this.actionNodes = actionNodes;
        this.nonRecordingActionNodes = nonRecordingActionNodes;
        this.audioRecorder = audioRecorder;
        this.statusPrompt = statusPrompt;
        this.onTranscriptionReady = onTranscriptionReady;
        this.onErrorMessage = onErrorMessage;
    }

    public void bind() {
        recordButton.setOnAction(event -> toggleRecording());
        updateRecordingState(false);
    }

    public void toggleRecording() {
        FxThreadRunner.run(() -> {
            if (recordButton != null && recordButton.isDisable()) {
                return;
            }
            if (recordMode == RecordMode.PREPARE_TO_START_RECORDING) {
                startRecording();
            } else {
                stopAndSendRecording();
            }
        });
    }

    private void startRecording() {
        if (recordMode != RecordMode.PREPARE_TO_START_RECORDING) {
            return;
        }
        if (!audioRecorder.startRecording()) {
            handleSpeechError(LanguageManager.getLanguageString("chat.speech.error.microphone"));
            return;
        }
        recordMode = RecordMode.PREPARE_TO_STOP_RECORDING;
        updateRecordingState(true);
        nonRecordingActionNodes.forEach(node -> node.setDisable(true));
        statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.speech.recording"));
    }

    private void stopAndSendRecording() {
        if (recordMode != RecordMode.PREPARE_TO_STOP_RECORDING) {
            return;
        }
        recordMode = RecordMode.PREPARE_TO_START_RECORDING;
        updateRecordingState(false);
        actionNodes.forEach(node -> node.setDisable(true));
        statusPrompt.showStatusPrompt(LanguageManager.getLanguageString("chat.speech.processing"));
        Thread.startVirtualThread(() -> {
            audioRecorder.stopRecording()
                    .ifPresentOrElse(
                            this::processRecording,
                            () -> handleSpeechError(LanguageManager.getLanguageString("chat.speech.error.empty")));
        });
    }

    private void processRecording(File audio) {
        try {
            String message = BackendApi.sendChatMessagesAsAudio(audio).content();
            if (message == null || message.isBlank()) {
                handleSpeechError(LanguageManager.getLanguageString("chat.speech.error.empty"));
                return;
            }
            onTranscriptionReady.accept(message);
        } catch (CannotConvertSpeechToTextException e) {
            handleSpeechError(resolveSpeechErrorMessage(e));
        } catch (Exception e) {
            logger.error("Cannot send speech message", e);
            handleSpeechError(LanguageManager.getLanguageString("chat.speech.error.unknown"));
        } finally {
            if (audio != null) {
                audio.delete();
            }
        }
    }

    private void handleSpeechError(String message) {
        logger.error(message);
        onErrorMessage.accept(message);
        FxThreadRunner.run(() -> {
            actionNodes.forEach(node -> node.setDisable(false));
            statusPrompt.clearStatusPrompt();
            recordMode = RecordMode.PREPARE_TO_START_RECORDING;
            updateRecordingState(false);
            chatTextArea.clear();
            chatTextArea.requestFocus();
        });
    }

    private void updateRecordingState(boolean recording) {
        if (recordButton == null) {
            return;
        }
        if (recording) {
            if (!recordButton.getStyleClass().contains(RECORDING_STYLE_CLASS)) {
                recordButton.getStyleClass().add(RECORDING_STYLE_CLASS);
            }
        } else {
            recordButton.getStyleClass().remove(RECORDING_STYLE_CLASS);
        }
        recordButton.setGraphic(recordMode.getFontIcon());
    }

    private String resolveSpeechErrorMessage(CannotConvertSpeechToTextException exception) {
        SpeechToTextErrorType errorType = exception.getErrorType() == null
                ? SpeechToTextErrorType.UNKNOWN
                : exception.getErrorType();
        return LanguageManager.getLanguageString(errorType.getI18nKey());
    }
}
