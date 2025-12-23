package com.patres.alina.uidesktop.microphone;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class AudioRecorder {

    private static final Logger logger = LoggerFactory.getLogger(AudioRecorder.class);


    private volatile boolean recording;
    private ByteArrayOutputStream byteArrayOutputStream;
    private TargetDataLine targetDataLine;
    private AudioFormat audioFormat;

    public AudioRecorder() {
        recording = false;
        byteArrayOutputStream = new ByteArrayOutputStream();
        audioFormat = getDefaultAudioFormat();
    }

    public boolean startRecording() {
        if (recording) {
            return false;
        }
        byteArrayOutputStream = new ByteArrayOutputStream();
        TargetDataLine line = openAudioLine();
        if (line == null) {
            logger.error("No supported audio line found for recording");
            return false;
        }
        targetDataLine = line;

        Thread recordingThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (recording) {
                try {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                } catch (IllegalStateException e) {
                    logger.debug("Audio line closed while recording", e);
                    break;
                }
            }
            try {
                byteArrayOutputStream.close();
            } catch (IOException e) {
                logger.error("Cannot record speech", e);
            }
        });
        recording = true;
        recordingThread.setDaemon(true);
        recordingThread.start();
        return true;
    }

    public Optional<File> stopRecording() {
        if (!recording || targetDataLine == null) {
            return Optional.empty();
        }
        recording = false;
        try {
            targetDataLine.stop();
        } catch (IllegalStateException e) {
            logger.debug("Audio line already stopped", e);
        }
        try {
            targetDataLine.close();
        } catch (Exception e) {
            logger.debug("Cannot close audio line", e);
        }
        targetDataLine = null;

        try {
            byte[] audioData = byteArrayOutputStream.toByteArray();
            if (audioData.length > 0) {
                logger.info("Recorded audio data length: {}", audioData.length);

                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
                File out = File.createTempFile("audio", ".wav");
                try (AudioInputStream audioInputStream = new AudioInputStream(
                        byteArrayInputStream,
                        audioFormat,
                        audioData.length / audioFormat.getFrameSize())) {
                    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);
                    return Optional.of(out);
                }
            } else {
                logger.error("No data to save audio");
            }
        } catch (IOException e) {
            logger.error("Cannot save audio", e);

        }
        return Optional.empty();
    }

    private AudioFormat getDefaultAudioFormat() {
        return new AudioFormat(16000, 16, 1, true, false);
    }

    private List<AudioFormat> getCandidateFormats() {
        return List.of(
                new AudioFormat(16000, 16, 1, true, false),
                new AudioFormat(44100, 16, 1, true, false),
                new AudioFormat(48000, 16, 1, true, false),
                new AudioFormat(44100, 16, 2, true, false),
                new AudioFormat(48000, 16, 2, true, false)
        );
    }

    private TargetDataLine openAudioLine() {
        for (AudioFormat format : getCandidateFormats()) {
            TargetDataLine line = openLineWithFormat(format);
            if (line != null) {
                audioFormat = format;
                return line;
            }
        }
        return null;
    }

    private TargetDataLine openLineWithFormat(AudioFormat format) {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine line = openLineFromSystem(info, format);
        if (line != null) {
            return line;
        }
        for (Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            line = openLineFromMixer(mixer, info, format);
            if (line != null) {
                return line;
            }
        }
        return null;
    }

    private TargetDataLine openLineFromSystem(DataLine.Info info, AudioFormat format) {
        if (!AudioSystem.isLineSupported(info)) {
            return null;
        }
        try {
            TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            return line;
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            logger.debug("Cannot open audio line using system default", e);
            return null;
        }
    }

    private TargetDataLine openLineFromMixer(Mixer mixer, DataLine.Info info, AudioFormat format) {
        if (!mixer.isLineSupported(info)) {
            return null;
        }
        try {
            TargetDataLine line = (TargetDataLine) mixer.getLine(info);
            line.open(format);
            line.start();
            return line;
        } catch (LineUnavailableException | IllegalArgumentException | SecurityException e) {
            logger.debug("Cannot open audio line from mixer {}", mixer.getMixerInfo().getName(), e);
            return null;
        }
    }

}
