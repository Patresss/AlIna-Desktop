package com.patres.alina.server.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Controls media playback on macOS via AppleScript.
 * Supports Spotify and Apple Music — tries Spotify first, then falls back to Music.
 * All public methods are static and never throw; failures are logged as warnings.
 */
public class MediaControlService {

    private static final Logger logger = LoggerFactory.getLogger(MediaControlService.class);
    private static final int APPLESCRIPT_TIMEOUT_SECONDS = 3;

    private MediaControlService() {
    }

    /**
     * Toggles play/pause on the active media player.
     */
    public static void playPause() {
        String player = getActivePlayer();
        if (player == null) {
            logger.warn("No active media player found for play/pause");
            return;
        }
        String script = "tell application \"" + player + "\" to playpause";
        runAppleScript(script);
    }

    /**
     * Skips to the next track on the active media player.
     */
    public static void nextTrack() {
        String player = getActivePlayer();
        if (player == null) {
            logger.warn("No active media player found for next track");
            return;
        }
        String script = "tell application \"" + player + "\" to next track";
        runAppleScript(script);
    }

    /**
     * Goes back to the previous track on the active media player.
     */
    public static void previousTrack() {
        String player = getActivePlayer();
        if (player == null) {
            logger.warn("No active media player found for previous track");
            return;
        }
        String script = "tell application \"" + player + "\" to previous track";
        runAppleScript(script);
    }

    /**
     * Returns the currently playing track info as "Track Name - Artist",
     * or null if no track is playing or no player is active.
     */
    public static String getNowPlaying() {
        String player = getActivePlayer();
        if (player == null) {
            return null;
        }
        String script = String.join("\n",
                "tell application \"" + player + "\"",
                "  if player state is playing then",
                "    set trackName to name of current track",
                "    set trackArtist to artist of current track",
                "    return trackName & \" - \" & trackArtist",
                "  else",
                "    return \"\"",
                "  end if",
                "end tell"
        );
        String result = runAppleScript(script);
        if (result == null || result.isBlank()) {
            return null;
        }
        return result;
    }

    /**
     * Returns whether the active media player is currently playing.
     */
    public static boolean isPlaying() {
        String player = getActivePlayer();
        if (player == null) {
            return false;
        }
        String script = String.join("\n",
                "tell application \"" + player + "\"",
                "  if player state is playing then",
                "    return \"true\"",
                "  else",
                "    return \"false\"",
                "  end if",
                "end tell"
        );
        String result = runAppleScript(script);
        return "true".equals(result);
    }

    /**
     * Detects the active media player. Tries Spotify first, then Apple Music.
     * Returns "Spotify", "Music", or null if neither is running.
     */
    public static String getActivePlayer() {
        String script = String.join("\n",
                "tell application \"System Events\"",
                "  set processList to name of every process",
                "end tell",
                "if processList contains \"Spotify\" then",
                "  return \"Spotify\"",
                "else if processList contains \"Music\" then",
                "  return \"Music\"",
                "else",
                "  return \"\"",
                "end if"
        );
        String result = runAppleScript(script);
        if (result == null || result.isBlank()) {
            return null;
        }
        return result;
    }

    /**
     * Runs an AppleScript via stdin to avoid quoting issues with -e flag.
     * Uses the same pattern as {@code MacTextAccessor}.
     */
    private static String runAppleScript(String script) {
        Process process = null;
        try {
            process = new ProcessBuilder("osascript", "-")
                    .redirectErrorStream(false)
                    .start();
            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(script.getBytes(StandardCharsets.UTF_8));
            }
            boolean finished = process.waitFor(APPLESCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                logger.warn("AppleScript timed out");
                return null;
            }
            if (process.exitValue() != 0) {
                String err = new String(process.getErrorStream().readAllBytes()).trim();
                logger.warn("AppleScript error (exit {}): {}", process.exitValue(), err);
                return null;
            }
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (Exception e) {
            logger.warn("AppleScript execution failed: {}", e.getMessage());
            return null;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}
