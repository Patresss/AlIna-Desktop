package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.uidesktop.common.event.ThemeEvent;
import com.patres.alina.uidesktop.ui.theme.ThemeManager;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

import static com.patres.alina.uidesktop.util.MarkdownToHtmlConverter.convertMarkdownToHtml;

public class Browser extends StackPane {

    private static final Logger logger = LoggerFactory.getLogger(Browser.class);

    final WebView webView;
    final WebEngine webEngine;
    
    private final StringBuilder streamingContent = new StringBuilder();

    public Browser() {
        super();
        this.webView = new WebView();
        this.webView.setMaxHeight(Double.MAX_VALUE);
        this.webEngine = webView.getEngine();

        webEngine.loadContent(initHtml());
        getChildren().add(webView);

        updateCssColors();
        DefaultEventBus.getInstance().subscribe(ThemeEvent.class, ignored -> updateCssColors());
    }

    /**
     * Safely execute JavaScript function calls with parameters to avoid deprecated JSObject warnings
     */
    @SuppressWarnings("removal")
    private void safeJavaScriptCall(final String functionName, final Object... parameters) {
        try {
            final var window = (netscape.javascript.JSObject) webEngine.executeScript("window");
            window.call(functionName, parameters);
        } catch (final Exception e) {
            logger.warn("Failed to execute JavaScript function: {} with {} parameters", functionName, parameters.length, e);
        }
    }

    /**
     * Safely execute JavaScript code
     */
    private void executeJavaScript(final String script) {
        try {
            webEngine.executeScript(script);
        } catch (final Exception e) {
            logger.warn("Failed to execute JavaScript: {}", script, e);
        }
    }

    /**
     * Adds formatted content to the chat interface.
     * 
     * @param markdownContent The markdown content to be converted to HTML
     * @param chatMessageRole The role of the message sender (USER, ASSISTANT, etc.)
     * @param chatMessageStyleType The styling type for the message
     */
    public void addContent(final String markdownContent,
                           final ChatMessageRole chatMessageRole,
                           final ChatMessageStyleType chatMessageStyleType) {
        final String htmlContent = convertMarkdownToHtml(markdownContent);
        safeJavaScriptCall("addHtmlContent", htmlContent, chatMessageRole.getChatMessageRole(), chatMessageStyleType.getStyleType());
        
        stopOpeningUrlInWebView();
        executeJavaScript("scrollToBottom()");
    }

    private void stopOpeningUrlInWebView() {
        final NodeList nodeList = webEngine.getDocument().getElementsByTagName("a");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node node = nodeList.item(i);
            EventTarget eventTarget = (EventTarget) node;
            eventTarget.addEventListener("click", evt -> {
                EventTarget target = evt.getCurrentTarget();
                HTMLAnchorElement anchorElement = (HTMLAnchorElement) target;
                String href = anchorElement.getHref();
                //handle opening URL outside JavaFX WebView
                openWebpage(href);
                evt.preventDefault();
            }, false);
        }
    }

    /**
     * Opens a URL in the system's default web browser.
     * 
     * @param url The URL to open in the browser
     */
    public static void openWebpage(final String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (final Exception e) {
            logger.error("Cannot open a link: {}", url, e);
        }
    }


    /**
     * Initiates a new streaming message from the assistant.
     * Clears the content buffer and sets up the UI for streaming.
     */
    public void startStreamingAssistantMessage() {
        streamingContent.setLength(0);
        executeJavaScript("startStreamingAssistantMessage()");
    }

    /**
     * Appends a token to the streaming message with real-time markdown processing.
     * Each token is added to the buffer, processed through markdown converter,
     * and immediately displayed with proper HTML formatting.
     * 
     * @param token The text token to append to the streaming message
     */
    public void appendToStreamingMessage(final String token) {
        try {
            appendTokenAndUpdateHtml(token);
        } catch (final Exception e) {
            logger.info("Error updating streaming message with markdown, using fallback", e);
            appendTokenWithFallback(token);
        }
    }

    private void appendTokenAndUpdateHtml(final String token) {
        streamingContent.append(token);
        
        final String currentMarkdown = streamingContent.toString();
        final String htmlContent = convertMarkdownToHtml(currentMarkdown);
        
        safeJavaScriptCall("updateStreamingMessageWithHtml", htmlContent);
    }

    private void appendTokenWithFallback(final String token) {
        final String escapedToken = escapeForJavaScript(token);
        executeJavaScript("appendToStreamingMessage('" + escapedToken + "')");
    }

    private static String escapeForJavaScript(final String input) {
        return input
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    /**
     * Finishes the current streaming message session.
     * Delegates to finishStreamingMessageWithMarkdown for consistency.
     */
    public void finishStreamingMessage() {
        finishStreamingMessageWithMarkdown();
    }

    /**
     * Completes the streaming message session and performs cleanup.
     * Since markdown is processed in real-time, this method only finalizes
     * the message display, clears the buffer, and updates link behaviors.
     */
    public void finishStreamingMessageWithMarkdown() {
        try {
            logger.info("Finishing streaming message, content was processed in real-time");
            
            executeJavaScript("finishStreamingMessage()");
            
            // Clear the content buffer for next streaming session
            streamingContent.setLength(0);
            
            stopOpeningUrlInWebView();
        } catch (final Exception e) {
            logger.error("Error finishing streaming message, using fallback", e);
            executeJavaScript("finishStreamingMessage()");
            streamingContent.setLength(0);
            stopOpeningUrlInWebView();
        }
    }

    /**
     * Shows the loader to indicate processing is in progress.
     * The loader will appear as an assistant message.
     */
    public void showLoader() {
        executeJavaScript("showLoader()");
    }

    /**
     * Shows the loader positioned as a user message.
     * Useful for indicating processing related to user input.
     */
    public void showLoaderForUserMessage() {
        executeJavaScript("showLoaderForUserMessage()");
    }

    /**
     * Hides the loader element.
     * Called when the first AI response token arrives or when an error occurs.
     */
    public void hideLoader() {
        executeJavaScript("hideLoader()");
    }

    private String initHtml() {
        return """
                <html>
                <head>
                %s
                %s
                </head>
                    <body>
                        <div id="chat-container"></div>
                        <div id="loader" class="chat-message assistant loader">
                            <div></div>
                            <div></div>
                            <div></div>
                        </div>
                    </body>
                </html>
                """.formatted(getCssStyles(), getJavaScript());
    }

    private String getCssStyles() {
        return """
                <style>
                body {
                    background-color: var(--color-bg-default);
                    color: var(--color-fg-default);
                    font-family: Söhne, ui-sans-serif, system-ui, -apple-system, "Segoe UI", Roboto, Ubuntu, Cantarell, "Noto Sans", sans-serif, "Helvetica Neue", Arial, "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji";
                    font-size: 14px;
                }
                a:link,
                a:hover,
                a:visited,
                a:active {
                  color: var(--color-accent-fg);
                }
                table {
                  width: 100%;
                  border-collapse: collapse;
                  margin: 20px 0;
                }
                th, td {
                  padding: 8px;
                  text-align: left;
                  border-bottom: 1px solid var(--color-border-default);
                }
                .chat-message {
                  background-color: var(--color-bg-subtle);
                  color: var(--color-fg-default);
                  box-shadow: 0 2px 4px var(--color-neutral-muted);
                  border-radius: 15px;
                  padding: 2px 15px;
                  margin: 10px 5px;
                  max-width: 85%;
                  word-wrap: break-word;
                  align-items: center;
                  overflow-y: auto;
                }
                .chat-message.accent {
                  box-shadow: 0 0 10px var(--color-accent-fg);
                }
                .chat-message.warning {
                  box-shadow: 0 0 10px var(--color-warning-fg);
                }
                .chat-message.success {
                  box-shadow: 0 0 10px var(--color-success-fg);
                }
                .chat-message.danger {
                  box-shadow: 0 0 10px var(--color-danger-fg);
                }
                .chat-message.user {
                  margin-left: auto;
                }
                .chat-message.assistant {
                }
                .loader {
                  display: none;
                  justify-content: space-around;
                  width: 50px;
                  height: 40px;
                }
                .loader.active {
                  display: flex;
                }
                .loader.user-message {
                  margin-left: auto;
                }
                .loader div {
                  width: 8px;
                  height: 8px;
                  background-color: var(--color-accent-fg);
                  border-radius: 50%;
                  animation: pulse 0.6s infinite alternate;
                }
                .loader div:nth-child(2) {
                  animation-delay: 0.2s;
                }
                .loader div:nth-child(3) {
                  animation-delay: 0.4s;
                }
                @keyframes pulse {
                  from { transform: scale(1); }
                  to { transform: scale(0.5); }
                }
                </style>
                """;
    }

    private String getJavaScript() {
        return """
                <script>
                    function addHtmlContent(htmlContent, messageType, notificationStyle) {
                        var div = document.createElement('div');
                        div.innerHTML = htmlContent;
                        div.className = 'chat-message ' + messageType + ' ' + notificationStyle;

                        var chatContainer = document.getElementById('chat-container');
                        chatContainer.appendChild(div);
                    }

                    function showLoader() {
                        document.getElementById('loader').classList.add('active');
                        document.getElementById('loader').classList.remove('user-message');
                        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                    }

                    function showLoaderForUserMessage() {
                        showLoader();
                        document.getElementById('loader').classList.add('user-message');
                    }

                    function hideLoader() {
                        document.getElementById('loader').classList.remove('active');
                        document.getElementById('loader').classList.remove('user-message');
                    }

                    function startStreamingAssistantMessage() {
                        var streamingDiv = document.createElement('div');
                        streamingDiv.className = 'chat-message assistant';
                        streamingDiv.id = 'streaming-message';
                        streamingDiv.innerHTML = '';

                        var chatContainer = document.getElementById('chat-container');
                        chatContainer.appendChild(streamingDiv);

                        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                    }

                    function appendToStreamingMessage(escapedToken) {
                        var streamingDiv = document.getElementById('streaming-message');
                        if (streamingDiv) {
                            // Use textContent to preserve markdown syntax during streaming
                            if (streamingDiv.textContent === undefined) {
                                streamingDiv.innerText += escapedToken;
                            } else {
                                streamingDiv.textContent += escapedToken;
                            }
                            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                        }
                    }

                    function updateStreamingMessageWithHtml(htmlContent) {
                        var streamingDiv = document.getElementById('streaming-message');
                        if (streamingDiv) {
                            // Update with processed HTML content in real-time
                            streamingDiv.innerHTML = htmlContent;
                            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                        }
                    }

                    function finishStreamingMessage() {
                        var streamingDiv = document.getElementById('streaming-message');
                        if (streamingDiv) {
                            // Remove the streaming ID so it becomes a regular message
                            streamingDiv.removeAttribute('id');
                            // Scroll to bottom one final time
                            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                        }
                    }

                    function finishStreamingMessageWithMarkdown(processedHtml) {
                        var streamingDiv = document.getElementById('streaming-message');
                        if (streamingDiv) {
                            // Replace raw content with processed markdown HTML
                            streamingDiv.innerHTML = processedHtml;
                            // Remove the streaming ID so it becomes a regular message
                            streamingDiv.removeAttribute('id');
                            // Scroll to bottom one final time
                            window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                        }
                    }

                    function scrollToBottom() {
                        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                    }
                </script>
                """;
    }

    private void updateCssColors() {
        Platform.runLater(() -> {
            final var themeManager = ThemeManager.getInstance();
            final var theme = themeManager.getTheme();
            
            if (theme != null) {
                try {
                    theme.parseColors().forEach(this::setCssProperty);
                } catch (final IOException e) {
                    logger.error("Failed to parse theme colors", e);
                }
            }
        });
    }
    
    private void setCssProperty(final String keyColor, final String valueColor) {
        final String script = String.format(
            "document.documentElement.style.setProperty('-%s', '%s');", 
            keyColor, 
            valueColor
        );
        executeJavaScript(script);
    }


}