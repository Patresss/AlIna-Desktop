package com.patres.alina.uidesktop.microphone;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Optional;

public class AudioRecorder {

    private static final Logger logger = LoggerFactory.getLogger(AudioRecorder.class);


    private boolean recording;
    private ByteArrayOutputStream byteArrayOutputStream;
    private TargetDataLine targetDataLine;
    private final AudioFormat audioFormat;

    public AudioRecorder() {
        recording = false;
        byteArrayOutputStream = new ByteArrayOutputStream();
        audioFormat = getAudioFormat();
    }

    public void startRecording() {
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

            Thread recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (recording) {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                }
                try {
                    byteArrayOutputStream.close();
                } catch (IOException e) {
                    logger.error("Cannot record speech", e);
                }
            });
            recording = true;
            recordingThread.start();
        } catch (LineUnavailableException e) {
            logger.error("Cannot record speech", e);
        }
    }

    public Optional<File> stopRecording() {
        recording = false;
        targetDataLine.stop();
        targetDataLine.close();

        try {
            byte[] audioData = byteArrayOutputStream.toByteArray();
            if (audioData.length > 0) {
                logger.info("Recorded audio data length: {}", audioData.length);

                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(audioData);
                AudioInputStream audioInputStream = new AudioInputStream(byteArrayInputStream, audioFormat, audioData.length / audioFormat.getFrameSize());

                File out = File.createTempFile("audio", ".wav");
                AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, out);

                audioInputStream.close();
                return Optional.of(out);
            } else {
                logger.error("No data to save audio");
            }
        } catch (IOException e) {
            logger.error("Cannot save audio", e);

        }
        return Optional.empty();
    }

    private AudioFormat getAudioFormat() {
        float sampleRate = 44100;
        int sampleSizeInBits = 16;
        int channels = 2;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

}