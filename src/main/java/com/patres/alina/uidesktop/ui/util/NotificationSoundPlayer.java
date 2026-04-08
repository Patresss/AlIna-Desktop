package com.patres.alina.uidesktop.ui.util;

import com.patres.alina.uidesktop.settings.UiSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import java.util.concurrent.CompletableFuture;

import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;

/**
 * Plays a short notification sound when a new AI message arrives or a notification is triggered.
 * All sounds are generated programmatically so no external audio files are needed.
 * Respects the {@link UiSettings#isSoundNotificationEnabled()} setting and the selected
 * {@link NotificationSound} variant.
 */
public final class NotificationSoundPlayer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationSoundPlayer.class);

    private static final float SAMPLE_RATE = 44100f;
    private static final double VOLUME = 0.15;

    private NotificationSoundPlayer() {
    }

    /**
     * Plays the notification sound asynchronously if sound notifications are enabled in settings.
     */
    public static void playIfEnabled() {
        final NotificationSound sound;
        try {
            UiSettings settings = UI_SETTINGS.getSettings();
            if (!settings.isSoundNotificationEnabled()) {
                return;
            }
            sound = settings.resolveNotificationSound();
        } catch (Exception e) {
            logger.debug("Could not read sound notification setting, skipping sound: {}", e.getMessage());
            return;
        }
        CompletableFuture.runAsync(() -> playSound(sound));
    }

    /**
     * Plays the given sound synchronously. Intended for preview in settings.
     */
    public static void playPreview(NotificationSound sound) {
        CompletableFuture.runAsync(() -> playSound(sound));
    }

    // ---- Dispatcher ----

    private static void playSound(NotificationSound sound) {
        AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
        try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
            line.open(format);
            line.start();

            switch (sound) {
                case CHIME -> playChime(line);
                case BUBBLE -> playBubble(line);
                case DING -> playDing(line);
                case SOFT_BELL -> playSoftBell(line);
                case DROPLET -> playDroplet(line);
                case SPARKLE -> playSparkle(line);
                case WARM_POP -> playWarmPop(line);
                case GENTLE_RISE -> playGentleRise(line);
            }

            line.drain();
        } catch (LineUnavailableException e) {
            logger.debug("Audio line unavailable, cannot play notification sound: {}", e.getMessage());
        } catch (Exception e) {
            logger.debug("Failed to play notification sound: {}", e.getMessage());
        }
    }

    // ========================================================================
    //  Sound variants
    // ========================================================================

    /**
     * CHIME - Gentle ascending two-note chime (E5 -> B5).
     * Warm bell-like timbre with harmonics.
     */
    private static void playChime(SourceDataLine line) {
        writeBellTone(line, 659.25, 140, 4.0);
        writeSilence(line, 60);
        writeBellTone(line, 987.77, 180, 4.0);
    }

    /**
     * BUBBLE - Soft rounded "pop" with a quick pitch sweep downward.
     * Like a soap bubble popping gently.
     */
    private static void playBubble(SourceDataLine line) {
        int durationMs = 120;
        int numSamples = msToSamples(durationMs);
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / numSamples;

            // Frequency sweeps down from 1400 Hz to 400 Hz
            double freq = 1400.0 - 1000.0 * progress;
            double wave = Math.sin(2.0 * Math.PI * freq * t);
            // Add soft sub-harmonic
            wave += 0.3 * Math.sin(2.0 * Math.PI * freq * 0.5 * t);

            // Quick attack, exponential decay
            double envelope = smoothAttack(progress, 0.03) * Math.exp(-5.0 * progress);

            writeSample(buffer, i, wave * VOLUME * 1.2 * envelope);
        }
        line.write(buffer, 0, buffer.length);
    }

    /**
     * DING - Single clean bell strike at C6 with long natural decay.
     * Classic notification ding.
     */
    private static void playDing(SourceDataLine line) {
        int durationMs = 350;
        int numSamples = msToSamples(durationMs);
        byte[] buffer = new byte[numSamples * 2];

        double freq = 1046.50; // C6
        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / numSamples;

            // Bell timbre: fundamental + inharmonic partials
            double wave = Math.sin(2.0 * Math.PI * freq * t);
            wave += 0.5 * Math.sin(2.0 * Math.PI * freq * 2.0 * t);
            wave += 0.25 * Math.sin(2.0 * Math.PI * freq * 2.76 * t); // inharmonic bell partial
            wave += 0.12 * Math.sin(2.0 * Math.PI * freq * 4.07 * t);

            double envelope = smoothAttack(progress, 0.02) * Math.exp(-5.5 * progress);

            writeSample(buffer, i, wave * VOLUME * 0.7 * envelope);
        }
        line.write(buffer, 0, buffer.length);
    }

    /**
     * SOFT_BELL - Muted, warm bell with two layered notes (A4 + E5) playing together.
     * Like a meditation bell at a distance.
     */
    private static void playSoftBell(SourceDataLine line) {
        int durationMs = 400;
        int numSamples = msToSamples(durationMs);
        byte[] buffer = new byte[numSamples * 2];

        double freq1 = 440.0;  // A4
        double freq2 = 659.25; // E5

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / numSamples;

            double wave1 = Math.sin(2.0 * Math.PI * freq1 * t)
                    + 0.3 * Math.sin(2.0 * Math.PI * freq1 * 2.0 * t);
            double wave2 = Math.sin(2.0 * Math.PI * freq2 * t)
                    + 0.2 * Math.sin(2.0 * Math.PI * freq2 * 2.0 * t);

            double wave = 0.6 * wave1 + 0.4 * wave2;
            double envelope = smoothAttack(progress, 0.04) * Math.exp(-3.5 * progress);

            writeSample(buffer, i, wave * VOLUME * 0.8 * envelope);
        }
        line.write(buffer, 0, buffer.length);
    }

    /**
     * DROPLET - Water droplet effect: quick high pitch plop followed by a low resonance.
     */
    private static void playDroplet(SourceDataLine line) {
        // High plop
        int plopMs = 50;
        int plopSamples = msToSamples(plopMs);
        byte[] plopBuf = new byte[plopSamples * 2];
        for (int i = 0; i < plopSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / plopSamples;
            double freq = 2200.0 - 1200.0 * progress;
            double wave = Math.sin(2.0 * Math.PI * freq * t);
            double envelope = smoothAttack(progress, 0.05) * Math.exp(-8.0 * progress);
            writeSample(plopBuf, i, wave * VOLUME * 1.3 * envelope);
        }
        line.write(plopBuf, 0, plopBuf.length);

        // Low resonance tail
        int tailMs = 180;
        int tailSamples = msToSamples(tailMs);
        byte[] tailBuf = new byte[tailSamples * 2];
        for (int i = 0; i < tailSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / tailSamples;
            double freq = 520.0 + 80.0 * Math.sin(2.0 * Math.PI * 6.0 * t); // slight vibrato
            double wave = Math.sin(2.0 * Math.PI * freq * t);
            wave += 0.4 * Math.sin(2.0 * Math.PI * freq * 2.0 * t);
            double envelope = smoothAttack(progress, 0.02) * Math.exp(-4.0 * progress);
            writeSample(tailBuf, i, wave * VOLUME * envelope);
        }
        line.write(tailBuf, 0, tailBuf.length);
    }

    /**
     * SPARKLE - Three quick ascending high notes like a tiny xylophone.
     * Bright and playful.
     */
    private static void playSparkle(SourceDataLine line) {
        double[] freqs = {1318.5, 1568.0, 2093.0}; // E6, G6, C7
        int[] durations = {70, 70, 100};

        for (int n = 0; n < freqs.length; n++) {
            int numSamples = msToSamples(durations[n]);
            byte[] buffer = new byte[numSamples * 2];
            double freq = freqs[n];

            for (int i = 0; i < numSamples; i++) {
                double t = (double) i / SAMPLE_RATE;
                double progress = (double) i / numSamples;

                double wave = Math.sin(2.0 * Math.PI * freq * t);
                wave += 0.2 * Math.sin(2.0 * Math.PI * freq * 3.0 * t);
                double envelope = smoothAttack(progress, 0.06) * Math.exp(-4.5 * progress);

                writeSample(buffer, i, wave * VOLUME * 0.9 * envelope);
            }
            line.write(buffer, 0, buffer.length);

            if (n < freqs.length - 1) {
                writeSilence(line, 30);
            }
        }
    }

    /**
     * WARM_POP - Short and round, like a wood block tap mixed with a soft bass.
     * Unobtrusive, suitable for frequent notifications.
     */
    private static void playWarmPop(SourceDataLine line) {
        int durationMs = 100;
        int numSamples = msToSamples(durationMs);
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / numSamples;

            // Fundamental with quick frequency drop (gives "pop" quality)
            double freq = 600.0 - 200.0 * progress;
            double wave = Math.sin(2.0 * Math.PI * freq * t);
            // Warm low-end body
            wave += 0.5 * Math.sin(2.0 * Math.PI * 220.0 * t);
            // Tiny high click at the very beginning
            if (progress < 0.1) {
                wave += 0.3 * Math.sin(2.0 * Math.PI * 3000.0 * t) * (1.0 - progress / 0.1);
            }

            double envelope = smoothAttack(progress, 0.02) * Math.exp(-7.0 * progress);

            writeSample(buffer, i, wave * VOLUME * 1.1 * envelope);
        }
        line.write(buffer, 0, buffer.length);
    }

    /**
     * GENTLE_RISE - Slow ascending tone that fades in and gently fades out.
     * Calm and non-startling, good for ambient use.
     */
    private static void playGentleRise(SourceDataLine line) {
        int durationMs = 300;
        int numSamples = msToSamples(durationMs);
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / numSamples;

            // Frequency rises from A4 to E5
            double freq = 440.0 + 220.0 * progress;
            double wave = Math.sin(2.0 * Math.PI * freq * t);
            wave += 0.25 * Math.sin(2.0 * Math.PI * freq * 2.0 * t);
            wave += 0.1 * Math.sin(2.0 * Math.PI * freq * 3.0 * t);

            // Slow fade in (first 30%), then gentle fade out
            double envelope;
            if (progress < 0.3) {
                envelope = 0.5 * (1.0 - Math.cos(Math.PI * progress / 0.3));
            } else {
                double decay = (progress - 0.3) / 0.7;
                envelope = Math.exp(-2.5 * decay);
            }

            writeSample(buffer, i, wave * VOLUME * 0.9 * envelope);
        }
        line.write(buffer, 0, buffer.length);
    }

    // ========================================================================
    //  Shared synthesis helpers
    // ========================================================================

    /**
     * Writes a warm bell-like tone with harmonics and exponential decay.
     */
    private static void writeBellTone(SourceDataLine line, double frequency, int durationMs, double decayRate) {
        int numSamples = msToSamples(durationMs);
        byte[] buffer = new byte[numSamples * 2];

        for (int i = 0; i < numSamples; i++) {
            double t = (double) i / SAMPLE_RATE;
            double progress = (double) i / numSamples;

            double wave = Math.sin(2.0 * Math.PI * frequency * t);
            wave += 0.3 * Math.sin(2.0 * Math.PI * frequency * 2 * t);
            wave += 0.1 * Math.sin(2.0 * Math.PI * frequency * 3 * t);

            double envelope = smoothAttack(progress, 0.05) * Math.exp(-decayRate * progress);

            writeSample(buffer, i, wave * VOLUME * envelope);
        }
        line.write(buffer, 0, buffer.length);
    }

    private static void writeSilence(SourceDataLine line, int durationMs) {
        int numSamples = msToSamples(durationMs);
        byte[] silence = new byte[numSamples * 2];
        line.write(silence, 0, silence.length);
    }

    /**
     * Smooth sinusoidal attack ramp to avoid clicks.
     */
    private static double smoothAttack(double progress, double attackEnd) {
        if (progress < attackEnd) {
            return 0.5 * (1.0 - Math.cos(Math.PI * progress / attackEnd));
        }
        return 1.0;
    }

    private static int msToSamples(int durationMs) {
        return (int) (SAMPLE_RATE * durationMs / 1000);
    }

    private static void writeSample(byte[] buffer, int index, double value) {
        short sample = (short) (value * Short.MAX_VALUE);
        buffer[index * 2] = (byte) (sample & 0xFF);
        buffer[index * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
    }
}
