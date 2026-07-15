package com.patres.alina.uidesktop.ui.chat;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ChatLoaderContractTest {

    private static final String CSS_RESOURCE =
            "/com/patres/alina/uidesktop/ui/chat/browser-chat.css";
    private static final String JS_RESOURCE =
            "/com/patres/alina/uidesktop/ui/chat/browser-chat.js";

    @Test
    void loaderWaveRunsOnlyWhileLoaderIsActive() throws IOException {
        final String css = readResource(CSS_RESOURCE);
        final Pattern inactiveDots = Pattern.compile(
                "\\.chat-message\\.loader > div\\s*\\{[^}]*animation:\\s*none;",
                Pattern.DOTALL
        );
        final Pattern activeDots = Pattern.compile(
                "\\.chat-message\\.loader\\.active > div\\s*\\{[^}]*animation:\\s*loaderWave",
                Pattern.DOTALL
        );

        assertThat(inactiveDots.matcher(css).find()).isTrue();
        assertThat(activeDots.matcher(css).find()).isTrue();
    }

    @Test
    void showingLoaderResetsAnimationBeforeRestartingIt() throws IOException {
        final String javascript = readResource(JS_RESOURCE);
        final int functionStart = javascript.indexOf("function showLoader()");
        final int functionEnd = javascript.indexOf("function showLoaderForUserMessage()", functionStart);
        final String showLoader = javascript.substring(functionStart, functionEnd);

        final int reset = showLoader.indexOf("loader.classList.remove('active');");
        final int reflow = showLoader.indexOf("void loader.offsetWidth;");
        final int restart = showLoader.indexOf("loader.classList.add('active');");

        assertThat(functionStart).isGreaterThanOrEqualTo(0);
        assertThat(functionEnd).isGreaterThan(functionStart);
        assertThat(reset).isGreaterThanOrEqualTo(0);
        assertThat(reflow).isGreaterThan(reset);
        assertThat(restart).isGreaterThan(reflow);
    }

    private String readResource(String resource) throws IOException {
        try (var stream = getClass().getResourceAsStream(resource)) {
            assertThat(stream).as(resource).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
