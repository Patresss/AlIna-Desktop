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
import netscape.javascript.JSObject;
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

    public Browser() {
        super();
        this.webView = new WebView();
        this.webView.setMaxHeight(Double.MAX_VALUE);
        this.webEngine = webView.getEngine();

        webEngine.loadContent(initHtml());
        getChildren().add(webView);

        updateCssColors();
        DefaultEventBus.getInstance().subscribe(ThemeEvent.class, e -> {
            updateCssColors();
        });
    }


    public void addContent(final String markdownContent,
                           final ChatMessageRole chatMessageRole,
                           final ChatMessageStyleType chatMessageStyleType) {
        final String htmlContent = convertMarkdownToHtml(markdownContent);
        final JSObject window = (JSObject) webEngine.executeScript("window");
        window.call("addHtmlContent", htmlContent, chatMessageRole.getChatMessageRole(), chatMessageStyleType.getStyleType());

        stopOpeningUrlInWebView();
        webEngine.executeScript("window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' })");
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

    public static void openWebpage(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (Exception e) {
            logger.error("Cannot open a link: {}", url, e);
        }
    }

    public void showLoader() {
        webEngine.executeScript("document.getElementById('loader').classList.add('active')");
        webEngine.executeScript("document.getElementById('loader').classList.remove('user-message')");
        webEngine.executeScript("window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' })");
    }

    public void showLoaderForUserMessage() {
        showLoader();
        webEngine.executeScript("document.getElementById('loader').classList.add('user-message')");
    }

    public void hideLoader() {
        webEngine.executeScript("document.getElementById('loader').classList.remove('active')");
        webEngine.executeScript("document.getElementById('loader').classList.remove('user-message')");
    }

    private String initHtml() {
        return """
                <html>
                <head>
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
                  from {
                    transform: scale(1);
                  }
                  to {
                    transform: scale(0.5);
                  }
                }

                </style>
                <script>
                    function addHtmlContent(htmlContent, messageType, notificationStyle) {
                        var div = document.createElement('div');
                        div.innerHTML = htmlContent;
                        div.className = 'chat-message ' + messageType + ' ' + notificationStyle;

                        var chatContainer = document.getElementById('chat-container');
                        chatContainer.appendChild(div);
                    }
                </script>
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
                """;
    }

    private void updateCssColors() {
        Platform.runLater(() -> {
                    if (ThemeManager.getInstance().getTheme() != null) {
                        try {
                            ThemeManager.getInstance().getTheme().parseColors().forEach((keyColor, valueColor) ->
                                    webEngine.executeScript("document.documentElement.style.setProperty('-" + keyColor + "', '" + valueColor + "');"));
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
        );
    }


}