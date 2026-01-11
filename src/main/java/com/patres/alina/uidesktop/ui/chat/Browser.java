package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.message.CommandUsageInfo;
import com.patres.alina.uidesktop.common.event.ThemeEvent;
import com.patres.alina.uidesktop.ui.theme.ThemeManager;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.IkonHandler;
import org.kordamp.ikonli.bootstrapicons.BootstrapIconsIkonHandler;
import org.kordamp.ikonli.devicons.DeviconsIkonHandler;
import org.kordamp.ikonli.feather.FeatherIkonHandler;
import org.kordamp.ikonli.material2.Material2ALIkonHandler;
import org.kordamp.ikonli.material2.Material2MZIkonHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import static com.patres.alina.uidesktop.util.MarkdownToHtmlConverter.convertMarkdownToHtml;

public class Browser extends StackPane {

    private static final Logger logger = LoggerFactory.getLogger(Browser.class);

    final WebView webView;
    final WebEngine webEngine;
    
    private final StringBuilder streamingContent = new StringBuilder();
    private static final int COMMAND_ICON_SIZE = 14;
    private static final String DEFAULT_COMMAND_ICON_LITERAL = "bi-slash";
    private static final List<IkonHandler> ICON_HANDLERS = List.of(
            new Material2ALIkonHandler(),
            new Material2MZIkonHandler(),
            new DeviconsIkonHandler(),
            new FeatherIkonHandler(),
            new BootstrapIconsIkonHandler()
    );
    private final Map<String, CommandIconData> commandIconCache = new HashMap<>();

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
        safeJavaScriptCallResult(functionName, parameters);
    }

    @SuppressWarnings("removal")
    private Object safeJavaScriptCallResult(final String functionName, final Object... parameters) {
        try {
            final var window = (netscape.javascript.JSObject) webEngine.executeScript("window");
            return window.call(functionName, parameters);
        } catch (final Exception e) {
            logger.warn("Failed to execute JavaScript function: {} with {} parameters", functionName, parameters.length, e);
            return null;
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
        addContent(markdownContent, chatMessageRole, chatMessageStyleType, null);
    }

    public void addContent(final String markdownContent,
                           final ChatMessageRole chatMessageRole,
                           final ChatMessageStyleType chatMessageStyleType,
                           final CommandUsageInfo commandUsageInfo) {
        final String htmlContent = convertMarkdownToHtml(markdownContent);
        final CommandTooltipData tooltipData = buildCommandTooltipData(commandUsageInfo);
        safeJavaScriptCall(
                "addHtmlContent",
                htmlContent,
                chatMessageRole.getChatMessageRole(),
                chatMessageStyleType.getStyleType(),
                tooltipData.iconFontFamily(),
                tooltipData.iconGlyph(),
                tooltipData.commandName(),
                tooltipData.prompt()
        );
        
        stopOpeningUrlInWebView();
        executeJavaScript("scrollToBottom()");
    }

    private CommandTooltipData buildCommandTooltipData(final CommandUsageInfo commandUsageInfo) {
        if (commandUsageInfo == null) {
            return CommandTooltipData.empty();
        }
        CommandIconData iconData = resolveCommandIconData(commandUsageInfo.commandIcon());
        if (iconData == null) {
            iconData = resolveCommandIconData(DEFAULT_COMMAND_ICON_LITERAL);
        }
        if (iconData == null) {
            return CommandTooltipData.empty();
        }
        return new CommandTooltipData(
                iconData.fontFamily(),
                iconData.glyph(),
                commandUsageInfo.commandName(),
                commandUsageInfo.prompt()
        );
    }

    private CommandIconData resolveCommandIconData(final String iconLiteral) {
        if (iconLiteral == null || iconLiteral.isBlank()) {
            return null;
        }
        return commandIconCache.computeIfAbsent(iconLiteral, this::createCommandIconData);
    }

    private CommandIconData createCommandIconData(final String iconLiteral) {
        if (iconLiteral == null || iconLiteral.isBlank()) {
            return null;
        }
        for (IkonHandler handler : ICON_HANDLERS) {
            if (handler.supports(iconLiteral)) {
                Ikon ikon = handler.resolve(iconLiteral);
                if (ikon == null) {
                    return null;
                }
                String glyph = new String(Character.toChars(ikon.getCode()));
                return new CommandIconData(handler.getFontFamily(), glyph);
            }
        }
        return null;
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
        startStreamingAssistantMessage(false);
    }

    public void startStreamingAssistantMessage(final boolean replaceExistingAssistantMessage) {
        streamingContent.setLength(0);
        safeJavaScriptCall("startStreamingAssistantMessage", replaceExistingAssistantMessage);
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

    public boolean prepareRegenerationTarget() {
        final Object result = safeJavaScriptCallResult("prepareRegenerationTarget");
        return result instanceof Boolean b && b;
    }

    public void restoreRegenerationTarget() {
        safeJavaScriptCall("restoreRegenerationTarget");
    }

    public void discardRegenerationBackup() {
        safeJavaScriptCall("discardRegenerationBackup");
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
        final String iconFontCss = buildIconFontFaceCss();
        return """
                <style>
                """.concat(iconFontCss).concat("""
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
                  border-left: 4px solid var(--color-accent-fg);
                  color: var(--color-fg-default);
                  box-shadow: 0 2px 4px var(--color-neutral-muted);
                  border-radius: 4px;
                  padding: 2px 15px;
                  margin: 10px 5px;
                  max-width: 85%;
                  word-wrap: break-word;
                  align-items: center;
                  overflow-y: auto;
                  position: relative;
                  box-sizing: border-box;
                }
                .chat-message.command-message {
                  padding-right: 36px;
                  padding-left: 15px;
                  padding-top: 2px;
                  padding-bottom: 2px;
                  overflow: visible;
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
                  border-left: 4px solid var(--color-accent-0);
                }
                .command-badge {
                  position: absolute;
                  top: 4px;
                  right: 6px;
                  width: 20px;
                  height: 20px;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  border-radius: 999px;
                  background-color: var(--color-bg-default);
                  border: 1px solid var(--color-border-default);
                  box-shadow: 0 2px 6px var(--color-neutral-muted);
                  cursor: help;
                }
                .command-icon {
                  font-size: 14px;
                  line-height: 1;
                  color: var(--color-fg-muted, var(--color-fg-default));
                }
                .command-tooltip {
                  position: absolute;
                  top: 22px;
                  right: 0;
                  background-color: var(--color-bg-default);
                  color: var(--color-fg-default);
                  border: 1px solid var(--color-border-default);
                  border-radius: 6px;
                  padding: 8px 10px;
                  min-width: 220px;
                  max-width: 420px;
                  max-height: 260px;
                  overflow: auto;
                  box-shadow: 0 8px 24px var(--color-neutral-muted);
                  display: none;
                  z-index: 10;
                }
                .command-badge:hover .command-tooltip {
                  display: block;
                }
                .command-tooltip-title {
                  font-weight: 600;
                  margin-bottom: 6px;
                }
                .command-tooltip-prompt {
                  white-space: pre-wrap;
                  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
                  font-size: 12px;
                  color: var(--color-fg-muted, var(--color-fg-default));
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
                  background-color: var(--color-accent-0);
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
                """);
    }

    private String buildIconFontFaceCss() {
        StringBuilder css = new StringBuilder();
        Set<String> loadedFamilies = new HashSet<>();
        for (IkonHandler handler : ICON_HANDLERS) {
            String family = handler.getFontFamily();
            if (family == null || family.isBlank() || !loadedFamilies.add(family)) {
                continue;
            }
            String dataUrl = readFontAsDataUrl(handler);
            if (dataUrl == null) {
                logger.warn("Cannot load icon font {}", family);
                continue;
            }
            css.append("@font-face{font-family:'")
                    .append(escapeCssValue(family))
                    .append("';src:url('")
                    .append(dataUrl)
                    .append("') format('truetype');font-weight:normal;font-style:normal;}");
        }
        return css.toString();
    }

    private String readFontAsDataUrl(final IkonHandler handler) {
        try (var stream = handler.getFontResourceAsStream()) {
            if (stream == null) {
                return null;
            }
            byte[] data = stream.readAllBytes();
            return "data:font/ttf;base64," + Base64.getEncoder().encodeToString(data);
        } catch (IOException e) {
            logger.warn("Cannot read icon font {}", handler.getFontFamily(), e);
            return null;
        }
    }

    private String escapeCssValue(final String value) {
        return value.replace("'", "\\'");
    }

    private String getJavaScript() {
        return """
                <script>
                    function addHtmlContent(htmlContent, messageType, notificationStyle, commandFontFamily, commandGlyph, commandName, commandPrompt) {
                        var div = document.createElement('div');
                        div.className = 'chat-message ' + messageType + ' ' + notificationStyle;
                        if (commandGlyph && commandFontFamily) {
                            div.classList.add('command-message');
                        }
                        div.innerHTML = htmlContent;

                        if (commandGlyph && commandFontFamily) {
                            var badge = document.createElement('div');
                            badge.className = 'command-badge';

                            var icon = document.createElement('span');
                            icon.className = 'command-icon';
                            icon.textContent = commandGlyph;
                            icon.style.fontFamily = commandFontFamily;
                            badge.appendChild(icon);

                            var tooltip = document.createElement('div');
                            tooltip.className = 'command-tooltip';

                            var title = document.createElement('div');
                            title.className = 'command-tooltip-title';
                            title.textContent = commandName ? commandName : '';
                            tooltip.appendChild(title);

                            var prompt = document.createElement('div');
                            prompt.className = 'command-tooltip-prompt';
                            prompt.textContent = commandPrompt ? commandPrompt : '';
                            tooltip.appendChild(prompt);

                            badge.appendChild(tooltip);
                            div.appendChild(badge);
                        }

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
                        var chatContainer = document.getElementById('chat-container');
                        var streamingDiv = null;

                        if (arguments.length > 0 && arguments[0] === true) {
                            var target = document.getElementById('regenerate-target');
                            if (target) {
                                streamingDiv = target;
                            }
                        }

                        if (!streamingDiv) {
                            streamingDiv = document.createElement('div');
                            chatContainer.appendChild(streamingDiv);
                        }

                        streamingDiv.className = 'chat-message assistant';
                        streamingDiv.id = 'streaming-message';
                        streamingDiv.innerHTML = '';

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

                    function prepareRegenerationTarget() {
                        var chatContainer = document.getElementById('chat-container');
                        if (!chatContainer || !chatContainer.children) {
                            return false;
                        }

                        for (var i = chatContainer.children.length - 1; i >= 0; i--) {
                            var node = chatContainer.children[i];
                            if (!node || !node.classList) {
                                continue;
                            }
                            if (node.classList.contains('chat-message') && node.classList.contains('assistant')) {
                                if (node.dataset) {
                                    node.dataset.prevHtml = node.innerHTML;
                                }
                                node.id = 'regenerate-target';
                                return true;
                            }
                        }
                        return false;
                    }

                    function restoreRegenerationTarget() {
                        var node = document.getElementById('streaming-message');
                        if (!node) {
                            node = document.getElementById('regenerate-target');
                        }
                        if (!node) {
                            return;
                        }

                        if (node.dataset && node.dataset.prevHtml !== undefined) {
                            node.innerHTML = node.dataset.prevHtml;
                            delete node.dataset.prevHtml;
                        }
                        node.removeAttribute('id');
                        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                    }

                    function discardRegenerationBackup() {
                        var node = document.getElementById('streaming-message');
                        if (!node) {
                            node = document.getElementById('regenerate-target');
                        }
                        if (!node) {
                            return;
                        }
                        if (node.dataset && node.dataset.prevHtml !== undefined) {
                            delete node.dataset.prevHtml;
                        }
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

    private record CommandTooltipData(
            String iconFontFamily,
            String iconGlyph,
            String commandName,
            String prompt
    ) {
        private static CommandTooltipData empty() {
            return new CommandTooltipData(null, null, null, null);
        }
    }

    private record CommandIconData(
            String fontFamily,
            String glyph
    ) {
    }

}
