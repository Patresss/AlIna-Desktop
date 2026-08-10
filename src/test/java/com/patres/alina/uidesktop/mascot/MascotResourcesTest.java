package com.patres.alina.uidesktop.mascot;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;

class MascotResourcesTest {

    private static final String RESOURCE_ROOT = "/com/patres/alina/uidesktop/";

    @Test
    void mascotImagesAreCompactPngFilesWithTransparentBackgrounds() throws IOException {
        assertTransparentImage("assets/mascot/alinka-permission.png");
        assertTransparentImage("assets/mascot/alinka-complete.png");
    }

    @Test
    void englishAndPolishBundlesContainMascotCopy() {
        for (Locale locale : new Locale[]{Locale.ENGLISH, Locale.forLanguageTag("pl")}) {
            final ResourceBundle bundle = ResourceBundle.getBundle("language.Bundle", locale);
            assertThat(bundle.getString("settings.mascot.notification.title")).isNotBlank();
            assertThat(bundle.getString("mascot.approval.allowOnce")).isNotBlank();
            assertThat(bundle.getString("mascot.approval.allowSession")).isNotBlank();
            assertThat(bundle.getString("mascot.approval.allowAlways")).isNotBlank();
            assertThat(bundle.getString("mascot.complete.title")).isNotBlank();
        }
    }

    private void assertTransparentImage(final String resource) throws IOException {
        final BufferedImage image;
        try (var stream = getClass().getResourceAsStream(RESOURCE_ROOT + resource)) {
            assertThat(stream).isNotNull();
            image = ImageIO.read(stream);
        }

        assertThat(image).isNotNull();
        assertThat(image.getWidth()).isBetween(128, 512);
        assertThat(image.getHeight()).isBetween(128, 512);
        assertThat(image.getColorModel().hasAlpha()).isTrue();
        assertThat((image.getRGB(0, 0) >>> 24) & 0xff).isZero();
        assertThat((image.getRGB(image.getWidth() / 2, image.getHeight() / 2) >>> 24) & 0xff).isGreaterThan(0);
    }
}
