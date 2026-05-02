package com.patres.alina.uidesktop.settings.ui;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.common.event.ThemeEvent;
import com.patres.alina.uidesktop.ui.chat.Browser;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.theme.ThemeManager;
import com.patres.alina.uidesktop.util.MarkdownToHtmlConverter;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

public class AboutPane extends ApplicationModalPaneContent {

    private static final Logger logger = LoggerFactory.getLogger(AboutPane.class);
    private static final String CHAT_CSS = loadCssResource();

    private WebView webView;
    private WebEngine webEngine;

    public AboutPane(Runnable backFunction) {
        super(backFunction);
        DefaultEventBus.getInstance().subscribe(ThemeEvent.class, ignored -> updateCssColors());
    }

    @Override
    @FXML
    public void initialize() {
        webView = new WebView();
        webView.setMaxHeight(Double.MAX_VALUE);
        webView.setMaxWidth(Double.MAX_VALUE);
        webEngine = webView.getEngine();

        webEngine.getLoadWorker().stateProperty().addListener((_, _, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                updateCssColors();
            }
        });

        webEngine.locationProperty().addListener((_, _, newLocation) -> {
            if (newLocation != null && newLocation.matches("^[a-zA-Z][a-zA-Z0-9+\\-.]*://.*")
                    && !newLocation.startsWith("about:") && !newLocation.startsWith("data:")) {
                Platform.runLater(() -> {
                    loadContent();
                    Browser.openWebpage(newLocation);
                });
            }
        });

        contentPane.getChildren().clear();
        contentPane.getChildren().add(webView);
        buttonBar.setVisible(false);
        buttonBar.setManaged(false);
    }

    @Override
    public void reload() {
        loadContent();
    }

    @Override
    protected List<Node> generateContent() {
        return List.of();
    }

    private void loadContent() {
        final String markdown = loadMarkdownForCurrentLocale();
        final String htmlBody = MarkdownToHtmlConverter.convertMarkdownToHtml(markdown);
        final String fullHtml = buildHtml(htmlBody);
        webEngine.loadContent(fullHtml, "text/html");
    }

    private String loadMarkdownForCurrentLocale() {
        final Locale locale = LanguageManager.localeProperty().get();
        final String lang = locale.getLanguage();
        final String resourcePath = "about/about_" + lang + ".md";

        try (var stream = Resources.getResourceAsStream(resourcePath)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            logger.warn("Cannot load about markdown for locale {}, falling back to English", lang, e);
            try (var stream = Resources.getResourceAsStream("about/about_en.md")) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception ex) {
                logger.error("Cannot load about markdown", ex);
                return "# About AlIna\n\nCannot load content.";
            }
        }
    }

    private static String buildHtml(final String bodyHtml) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset="UTF-8">
                <style>
                %s
                .about-content { padding: 8px 16px; }
                </style>
                </head>
                <body>
                <div class="about-content">
                %s
                </div>
                </body>
                </html>
                """.formatted(CHAT_CSS, bodyHtml);
    }

    private void updateCssColors() {
        Platform.runLater(() -> {
            final var themeManager = ThemeManager.getInstance();
            final var theme = themeManager.getTheme();
            if (theme != null) {
                try {
                    theme.parseColors().forEach((key, value) -> {
                        final String script = String.format(
                                "document.documentElement.style.setProperty('-%s', '%s');",
                                key, value
                        );
                        try {
                            webEngine.executeScript(script);
                        } catch (Exception ignored) {
                            // WebView not ready yet
                        }
                    });
                } catch (IOException e) {
                    logger.error("Failed to parse theme colors", e);
                }
            }
        });
    }

    private static String loadCssResource() {
        try (var stream = Resources.getResourceAsStream("ui/chat/browser-chat.css")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LoggerFactory.getLogger(AboutPane.class).error("Cannot load chat CSS for about page", e);
            return "";
        }
    }
}
