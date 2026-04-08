package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.message.CommandUsageInfo;
import com.patres.alina.uidesktop.common.event.ThemeEvent;
import com.patres.alina.uidesktop.ui.theme.ThemeManager;
import javafx.application.Platform;
import javafx.concurrent.Worker;
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
    private PermissionActionHandler permissionActionHandler;
    private volatile boolean webViewReady = false;
    private final java.util.List<Runnable> pendingActions = new java.util.ArrayList<>();

    public Browser() {
        super();
        this.webView = new WebView();
        this.webView.setMaxHeight(Double.MAX_VALUE);
        this.webEngine = webView.getEngine();
        this.webEngine.setOnAlert(event -> handleBrowserAlert(event.getData()));
        this.webEngine.getLoadWorker().stateProperty().addListener((_, _, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                attachJavaBridge();
                webViewReady = true;
                pendingActions.forEach(Runnable::run);
                pendingActions.clear();
            }
        });

        webEngine.loadContent(initHtml(), "text/html");
        getChildren().add(webView);

        updateCssColors();
        DefaultEventBus.getInstance().subscribe(ThemeEvent.class, ignored -> updateCssColors());
    }

    public void setPermissionActionHandler(final PermissionActionHandler permissionActionHandler) {
        this.permissionActionHandler = permissionActionHandler;
    }

    /**
     * Executes the given action immediately if the WebView has finished loading,
     * otherwise queues it to run once the initial HTML/JS is ready.
     */
    public void whenReady(final Runnable action) {
        if (webViewReady) {
            action.run();
        } else {
            pendingActions.add(action);
        }
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
        if (webEngine.getDocument() == null) {
            return;
        }
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

    public void showAssistantActivity(final String label) {
        safeJavaScriptCall("showAssistantActivity", label);
    }

    public void showAssistantReasoning(final String title, final String markdownContent) {
        final String htmlContent = convertMarkdownToHtml(markdownContent);
        safeJavaScriptCall("showAssistantReasoning", title, htmlContent);
    }

    public void showAssistantCommentary(final String title, final String markdownContent) {
        final String htmlContent = convertMarkdownToHtml(markdownContent);
        safeJavaScriptCall("showAssistantCommentary", title, htmlContent);
    }

    public void finalizeAssistantActivity() {
        executeJavaScript("finalizeAssistantActivity()");
    }

    public void finalizeAssistantReasoning() {
        executeJavaScript("finalizeAssistantReasoning()");
    }

    public void finalizeAssistantCommentary() {
        executeJavaScript("finalizeAssistantCommentary()");
    }

    public void attachMessageFooter(final String footerText) {
        safeJavaScriptCall("attachMessageFooter", footerText);
    }

    public void clearAssistantActivity() {
        executeJavaScript("clearAssistantActivity()");
    }

    public void clearAssistantReasoning() {
        executeJavaScript("clearAssistantReasoning()");
    }

    public void clearAssistantCommentary() {
        executeJavaScript("clearAssistantCommentary()");
    }

    public void attachProcessPanelToLastAssistantMessage(final String summaryText,
                                                         final String reasoningTitle,
                                                         final String reasoningMarkdownContent,
                                                         final String commentaryTitle,
                                                         final String commentaryMarkdownContent,
                                                         final String toolsHtml) {
        final String reasoningHtml = reasoningMarkdownContent == null || reasoningMarkdownContent.isBlank()
                ? ""
                : convertMarkdownToHtml(reasoningMarkdownContent);
        final String commentaryHtml = commentaryMarkdownContent == null || commentaryMarkdownContent.isBlank()
                ? ""
                : convertMarkdownToHtml(commentaryMarkdownContent);
        safeJavaScriptCall(
                "attachProcessPanelToLastAssistantMessage",
                summaryText == null ? "" : summaryText,
                reasoningTitle == null ? "" : reasoningTitle,
                reasoningHtml,
                commentaryTitle == null ? "" : commentaryTitle,
                commentaryHtml,
                toolsHtml == null ? "" : toolsHtml
        );
    }

    public void showAssistantPermissionRequest(final String requestId,
                                               final String title,
                                               final String message,
                                               final String approveLabel,
                                               final String approveAlwaysLabel,
                                               final String denyLabel) {
        safeJavaScriptCall(
                "showAssistantPermissionRequest",
                requestId,
                title,
                message,
                approveLabel,
                approveAlwaysLabel,
                denyLabel
        );
    }

    public void markAssistantPermissionRequestPending(final String requestId, final String statusLabel) {
        safeJavaScriptCall("markAssistantPermissionRequestPending", requestId, statusLabel);
    }

    public void resolveAssistantPermissionRequest(final String requestId, final String statusLabel) {
        safeJavaScriptCall("resolveAssistantPermissionRequest", requestId, statusLabel);
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
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
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

    @SuppressWarnings("removal")
    private void attachJavaBridge() {
        try {
            final var window = (netscape.javascript.JSObject) webEngine.executeScript("window");
            window.setMember("alinaBrowserBridge", new BrowserBridge());
        } catch (final Exception e) {
            logger.warn("Failed to attach JavaScript bridge", e);
        }
    }

    private void handleBrowserAlert(final String data) {
        if (data == null || !data.startsWith("__ALINA_PERMISSION__|")) {
            return;
        }
        final String payload = data.substring("__ALINA_PERMISSION__|".length());
        final String[] parts = payload.split("\\|", 2);
        if (parts.length != 2) {
            return;
        }
        if (permissionActionHandler == null) {
            return;
        }
        permissionActionHandler.onPermissionAction(parts[0], parts[1]);
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
                    margin: 0;
                    padding: 0;
                }
                a:link,
                a:hover,
                a:visited,
                a:active {
                  color: var(--color-accent-fg);
                }
                .emoji {
                  display: inline-block;
                  font-family: "Apple Color Emoji", "Segoe UI Emoji", "Segoe UI Symbol", "Noto Color Emoji", sans-serif;
                  font-style: normal;
                  font-weight: normal;
                  line-height: 1;
                  vertical-align: middle;
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
                  color: var(--color-fg-default);
                  word-break: break-word;
                  overflow-wrap: break-word;
                }
                .chat-message {
                  background-color: var(--color-bg-subtle);
                  border-left: 4px solid var(--color-accent-fg);
                  color: var(--color-fg-default);
                  box-shadow: 0 2px 4px var(--color-neutral-muted);
                  border-radius: 4px;
                  padding: 2px 12px;
                  margin: 6px 0;
                  max-width: 95%;
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
                .chat-message.user {
                  margin-left: auto;
                }
                .chat-message.assistant {
                  border-left: 4px solid var(--color-accent-0);
                }
                .chat-message.accent {
                  box-shadow: 0 0 10px var(--color-accent-fg);
                }
                .chat-message.info {
                  border-left: 4px solid var(--color-info-fg);
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
                .chat-message.activity-message {
                  border-left: 4px solid var(--color-warning-fg);
                  background-color: var(--color-bg-default);
                  color: var(--color-fg-muted);
                  box-shadow: none;
                  padding-top: 6px;
                  padding-bottom: 6px;
                }
                .activity-shell {
                  width: 100%;
                }
                .activity-summary {
                  font-size: 12px;
                  font-weight: 600;
                  color: var(--color-fg-muted);
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  gap: 10px;
                }
                .activity-summary-main {
                  min-width: 0;
                  display: flex;
                  align-items: center;
                  gap: 8px;
                  flex: 1;
                }
                .activity-summary-text {
                  overflow: hidden;
                  text-overflow: ellipsis;
                  white-space: nowrap;
                  min-width: 0;
                }
                .activity-summary-badge {
                  border: 1px solid var(--color-border-default);
                  border-radius: 999px;
                  padding: 1px 7px;
                  font-size: 11px;
                  color: var(--color-fg-muted);
                  background-color: var(--color-bg-subtle);
                  flex-shrink: 0;
                }
                .activity-toggle {
                  border: 1px solid var(--color-border-default);
                  border-radius: 999px;
                  background-color: transparent;
                  color: var(--color-fg-muted);
                  font-size: 14px;
                  line-height: 1;
                  width: 26px;
                  height: 26px;
                  padding: 0;
                  cursor: pointer;
                  flex-shrink: 0;
                  display: inline-flex;
                  align-items: center;
                  justify-content: center;
                }
                .activity-body {
                  margin-top: 8px;
                  display: none;
                  padding-left: 2px;
                }
                .activity-body.open {
                  display: block;
                }
                .activity-entry {
                  font-size: 11px;
                  line-height: 1.45;
                  color: var(--color-fg-muted);
                  margin-top: 3px;
                  white-space: nowrap;
                  overflow: hidden;
                  text-overflow: ellipsis;
                }
                .chat-message.permission-message {
                  border-left: none;
                  background-color: transparent;
                  color: var(--color-fg-default);
                  box-shadow: none;
                  padding: 0;
                  max-width: 100%;
                }
                .chat-message.reasoning-message {
                  border-left: 4px solid var(--color-attention-fg);
                  background-color: var(--color-bg-default);
                  color: var(--color-fg-muted);
                  box-shadow: none;
                  padding-top: 8px;
                  padding-bottom: 8px;
                }
                .chat-message.commentary-message {
                  border-left: 4px solid var(--color-accent-0);
                  background-color: var(--color-bg-default);
                  color: var(--color-fg-muted);
                  box-shadow: none;
                  padding-top: 8px;
                  padding-bottom: 8px;
                }
                .reasoning-details {
                  width: 100%;
                }
                .commentary-details {
                  width: 100%;
                }
                .reasoning-summary {
                  cursor: pointer;
                  font-size: 12px;
                  font-weight: 600;
                  color: var(--color-fg-muted);
                  list-style: none;
                  user-select: none;
                }
                .reasoning-summary::-webkit-details-marker {
                  display: none;
                }
                .commentary-summary {
                  cursor: pointer;
                  font-size: 12px;
                  font-weight: 600;
                  color: var(--color-fg-muted);
                  list-style: none;
                  user-select: none;
                }
                .commentary-summary::-webkit-details-marker {
                  display: none;
                }
                .reasoning-body {
                  margin-top: 8px;
                  color: var(--color-fg-muted);
                }
                .commentary-body {
                  margin-top: 8px;
                  color: var(--color-fg-muted);
                }
                .assistant-process {
                  margin-top: 12px;
                  border-top: 1px solid var(--color-border-default);
                  padding-top: 10px;
                }
                .assistant-process-toggle {
                  width: 100%;
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  gap: 10px;
                  border: none;
                  background: transparent;
                  color: var(--color-fg-muted);
                  padding: 0;
                  cursor: pointer;
                  font: inherit;
                  text-align: left;
                }
                .assistant-process-summary {
                  font-size: 12px;
                  font-weight: 600;
                  color: var(--color-fg-muted);
                }
                .assistant-process-chevron {
                  width: 24px;
                  height: 24px;
                  display: inline-flex;
                  align-items: center;
                  justify-content: center;
                  border: 1px solid var(--color-border-default);
                  border-radius: 999px;
                  font-size: 13px;
                  flex-shrink: 0;
                }
                .assistant-process-body {
                  display: none;
                  margin-top: 10px;
                }
                .assistant-process-body.open {
                  display: block;
                }
                .assistant-process-section + .assistant-process-section {
                  margin-top: 12px;
                }
                .assistant-process-title {
                  font-size: 12px;
                  font-weight: 600;
                  color: var(--color-fg-muted);
                  margin-bottom: 6px;
                }
                .assistant-process-content {
                  font-size: 13px;
                  color: var(--color-fg-muted);
                }
                .assistant-process-content ul {
                  margin: 0;
                  padding-left: 18px;
                }
                .activity-entry {
                  font-size: 12px;
                  line-height: 1.45;
                }
                .activity-entry + .activity-entry {
                  margin-top: 4px;
                }
                .permission-shell {
                  width: 100%;
                  border: 1px solid var(--color-accent-fg);
                  border-radius: 12px;
                  background-color: var(--color-bg-default);
                  padding: 12px 14px;
                  box-sizing: border-box;
                }
                .permission-header {
                  display: flex;
                  align-items: center;
                  justify-content: space-between;
                  gap: 12px;
                  margin-bottom: 8px;
                }
                .permission-title {
                  font-size: 13px;
                  font-weight: 600;
                  margin-bottom: 0;
                }
                .permission-badge {
                  border: 1px solid var(--color-border-default);
                  border-radius: 999px;
                  padding: 2px 8px;
                  font-size: 11px;
                  color: var(--color-fg-muted);
                  background-color: var(--color-bg-subtle);
                  white-space: nowrap;
                }
                .permission-message-body {
                  white-space: pre-wrap;
                  line-height: 1.45;
                  color: var(--color-fg-muted);
                  font-size: 12px;
                }
                .permission-actions {
                  display: grid;
                  grid-template-columns: repeat(3, minmax(0, 1fr));
                  gap: 8px;
                  margin-top: 12px;
                }
                .permission-actions button {
                  min-height: 38px;
                  border: 1px solid var(--color-accent-fg);
                  background-color: transparent;
                  color: var(--color-fg-default);
                  border-radius: 10px;
                  padding: 8px 12px;
                  cursor: pointer;
                  font: inherit;
                  font-weight: 600;
                  text-align: center;
                }
                .permission-actions button.primary {
                  background-color: var(--color-bg-subtle);
                }
                .permission-actions button.always {
                  border-color: var(--color-success-fg);
                  background-color: var(--color-bg-subtle);
                }
                .permission-actions button.deny {
                  border-color: var(--color-danger-fg);
                  background-color: var(--color-bg-subtle);
                }
                .permission-actions button:disabled {
                  cursor: default;
                  opacity: 0.55;
                }
                .permission-status {
                  margin-top: 10px;
                  font-size: 12px;
                  color: var(--color-fg-muted);
                }
                @media (max-width: 720px) {
                  .permission-actions {
                    grid-template-columns: 1fr;
                  }
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
                .message-footer {
                  margin-top: 2px;
                  font-size: 11px;
                  color: var(--color-fg-muted);
                  opacity: 0;
                  transition: opacity 0.2s ease;
                }
                .chat-message:hover .message-footer {
                  opacity: 1;
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

                    function showAssistantActivity(label) {
                        var chatContainer = document.getElementById('chat-container');
                        if (!chatContainer) {
                            return;
                        }
                        var activity = document.getElementById('assistant-activity-message');
                        if (!activity) {
                            activity = document.createElement('div');
                            activity.className = 'chat-message assistant activity-message';
                            activity.id = 'assistant-activity-message';
                            activity.dataset.transient = 'true';
                            activity.dataset.count = '0';
                            activity.dataset.expanded = 'false';

                            var shell = document.createElement('div');
                            shell.className = 'activity-shell';

                            var summary = document.createElement('div');
                            summary.className = 'activity-summary';

                            var summaryMain = document.createElement('div');
                            summaryMain.className = 'activity-summary-main';

                            var summaryText = document.createElement('span');
                            summaryText.className = 'activity-summary-text';
                            summaryText.id = 'assistant-activity-summary-text';
                            summaryText.textContent = 'OpenCode: ' + label;

                            var summaryBadge = document.createElement('span');
                            summaryBadge.className = 'activity-summary-badge';
                            summaryBadge.id = 'assistant-activity-summary-badge';
                            summaryBadge.textContent = '1';

                            var toggleButton = document.createElement('button');
                            toggleButton.className = 'activity-toggle';
                            toggleButton.id = 'assistant-activity-toggle';
                            toggleButton.type = 'button';
                            toggleButton.textContent = '▸';
                            toggleButton.setAttribute('aria-label', 'Pokaż szczegóły');
                            toggleButton.onclick = function() {
                                var shell = this.parentElement ? this.parentElement.parentElement : null;
                                var activityEl = shell ? shell.parentElement : null;
                                var bodyEl = shell ? shell.querySelector('.activity-body') : null;
                                if (!activityEl || !bodyEl) return;
                                var expanded = activityEl.dataset.expanded === 'true';
                                if (expanded) {
                                    bodyEl.classList.remove('open');
                                    activityEl.dataset.expanded = 'false';
                                    this.textContent = '\u25b8';
                                    this.setAttribute('aria-label', 'Show details');
                                } else {
                                    bodyEl.classList.add('open');
                                    activityEl.dataset.expanded = 'true';
                                    this.textContent = '\u25be';
                                    this.setAttribute('aria-label', 'Hide details');
                                }
                            };

                            summaryMain.appendChild(summaryText);
                            summaryMain.appendChild(summaryBadge);
                            summary.appendChild(summaryMain);
                            summary.appendChild(toggleButton);

                            var body = document.createElement('div');
                            body.className = 'activity-body';
                            body.id = 'assistant-activity-body';

                            shell.appendChild(summary);
                            shell.appendChild(body);
                            activity.appendChild(shell);
                            chatContainer.appendChild(activity);
                        }

                        var bodyNode = document.getElementById('assistant-activity-body');
                        var lastEntry = bodyNode ? bodyNode.lastElementChild : null;
                        if (lastEntry && lastEntry.dataset && lastEntry.dataset.label === label) {
                            var count = parseInt(lastEntry.dataset.count ? lastEntry.dataset.count : '1', 10) + 1;
                            lastEntry.dataset.count = String(count);
                            lastEntry.textContent = label + ' ×' + count;
                        } else if (bodyNode) {
                            var entry = document.createElement('div');
                            entry.className = 'activity-entry';
                            entry.dataset.label = label;
                            entry.dataset.count = '1';
                            entry.textContent = label;
                            bodyNode.appendChild(entry);
                        }

                        if (activity.dataset) {
                            var totalCount = parseInt(activity.dataset.count ? activity.dataset.count : '0', 10) + 1;
                            activity.dataset.count = String(totalCount);
                            activity.dataset.lastEntry = label;
                        }

                        var summaryTextNode = document.getElementById('assistant-activity-summary-text');
                        if (summaryTextNode) {
                            summaryTextNode.textContent = buildAssistantActivitySummary(activity.dataset.count, label);
                        }
                        var summaryBadgeNode = document.getElementById('assistant-activity-summary-badge');
                        if (summaryBadgeNode && activity.dataset) {
                            summaryBadgeNode.textContent = activity.dataset.count;
                        }
                        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                    }

                    function finalizeAssistantActivity() {
                        var activity = document.getElementById('assistant-activity-message');
                        if (!activity) {
                            return;
                        }
                        activity.removeAttribute('id');
                        var childIds = ['assistant-activity-body', 'assistant-activity-toggle',
                                        'assistant-activity-summary-text', 'assistant-activity-summary-badge'];
                        childIds.forEach(function(cid) {
                            var el = document.getElementById(cid);
                            if (el) el.removeAttribute('id');
                        });
                        if (activity.dataset) {
                            delete activity.dataset.lastEntry;
                        }
                    }

                    function clearAssistantActivity() {
                        var activity = document.getElementById('assistant-activity-message');
                        if (!activity) {
                            return;
                        }
                        activity.remove();
                    }

                    function buildAssistantActivitySummary(count, lastLabel) {
                        var parsedCount = parseInt(count ? count : '0', 10);
                        var safeCount = Number.isNaN(parsedCount) ? 0 : parsedCount;
                        if (safeCount <= 1) {
                            return 'OpenCode: ' + lastLabel;
                        }
                        return 'OpenCode tools: ' + safeCount + ' · ostatnio: ' + lastLabel;
                    }

                    function toggleAssistantActivity() {
                        var activity = document.getElementById('assistant-activity-message');
                        if (!activity || !activity.dataset) {
                            return;
                        }
                        var body = document.getElementById('assistant-activity-body');
                        var toggle = document.getElementById('assistant-activity-toggle');
                        if (!body || !toggle) {
                            return;
                        }
                        var expanded = activity.dataset.expanded === 'true';
                        if (expanded) {
                            body.classList.remove('open');
                            activity.dataset.expanded = 'false';
                            toggle.textContent = '▸';
                            toggle.setAttribute('aria-label', 'Pokaż szczegóły');
                        } else {
                            body.classList.add('open');
                            activity.dataset.expanded = 'true';
                            toggle.textContent = '▾';
                            toggle.setAttribute('aria-label', 'Ukryj szczegóły');
                        }
                    }

                    function showAssistantReasoning(title, htmlContent) {
                        var chatContainer = document.getElementById('chat-container');
                        if (!chatContainer) {
                            return;
                        }
                        var card = document.getElementById('assistant-reasoning-message');
                        if (!card) {
                            card = document.createElement('div');
                            card.className = 'chat-message assistant reasoning-message';
                            card.id = 'assistant-reasoning-message';
                            card.dataset.transient = 'true';

                            var details = document.createElement('details');
                            details.className = 'reasoning-details';
                            details.open = true;

                            var summary = document.createElement('summary');
                            summary.className = 'reasoning-summary';
                            summary.textContent = title;

                            var body = document.createElement('div');
                            body.className = 'reasoning-body';
                            body.id = 'assistant-reasoning-body';

                            details.appendChild(summary);
                            details.appendChild(body);
                            card.appendChild(details);
                            chatContainer.appendChild(card);
                        }
                        var bodyNode = document.getElementById('assistant-reasoning-body');
                        if (bodyNode) {
                            bodyNode.innerHTML = htmlContent;
                        }
                        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                    }

                    function showAssistantCommentary(title, htmlContent) {
                        var chatContainer = document.getElementById('chat-container');
                        if (!chatContainer) {
                            return;
                        }
                        var card = document.getElementById('assistant-commentary-message');
                        if (!card) {
                            card = document.createElement('div');
                            card.className = 'chat-message assistant commentary-message';
                            card.id = 'assistant-commentary-message';
                            card.dataset.transient = 'true';

                            var details = document.createElement('details');
                            details.className = 'commentary-details';
                            details.open = true;

                            var summary = document.createElement('summary');
                            summary.className = 'commentary-summary';
                            summary.textContent = title;

                            var body = document.createElement('div');
                            body.className = 'commentary-body';
                            body.id = 'assistant-commentary-body';

                            details.appendChild(summary);
                            details.appendChild(body);
                            card.appendChild(details);
                            chatContainer.appendChild(card);
                        }
                        var bodyNode = document.getElementById('assistant-commentary-body');
                        if (bodyNode) {
                            bodyNode.innerHTML = htmlContent;
                        }
                        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                    }

                    function finalizeAssistantReasoning() {
                        var card = document.getElementById('assistant-reasoning-message');
                        if (!card) {
                            return;
                        }
                        card.removeAttribute('id');
                        var body = document.getElementById('assistant-reasoning-body');
                        if (body) {
                            body.removeAttribute('id');
                        }
                    }

                    function finalizeAssistantCommentary() {
                        var card = document.getElementById('assistant-commentary-message');
                        if (!card) {
                            return;
                        }
                        card.removeAttribute('id');
                        var body = document.getElementById('assistant-commentary-body');
                        if (body) {
                            body.removeAttribute('id');
                        }
                    }

                    function clearAssistantReasoning() {
                        var card = document.getElementById('assistant-reasoning-message');
                        if (!card) {
                            return;
                        }
                        card.remove();
                    }

                    function clearAssistantCommentary() {
                        var card = document.getElementById('assistant-commentary-message');
                        if (!card) {
                            return;
                        }
                        card.remove();
                    }

                    function attachProcessPanelToLastAssistantMessage(summaryText, reasoningTitle, reasoningHtml, commentaryTitle, commentaryHtml, toolsHtml) {
                        var chatContainer = document.getElementById('chat-container');
                        if (!chatContainer || !chatContainer.children) {
                            return;
                        }

                        var target = null;
                        for (var i = chatContainer.children.length - 1; i >= 0; i--) {
                            var node = chatContainer.children[i];
                            if (!node || !node.classList) {
                                continue;
                            }
                            if (node.id === 'streaming-message') {
                                target = node;
                                break;
                            }
                            if (node.classList.contains('chat-message')
                                && node.classList.contains('assistant')
                                && (!node.dataset || node.dataset.transient !== 'true')) {
                                target = node;
                                break;
                            }
                        }

                        if (!target) {
                            return;
                        }

                        var existing = target.querySelector('.assistant-process');
                        if (existing) {
                            existing.remove();
                        }

                        var hasReasoning = reasoningHtml && reasoningHtml.trim() !== '';
                        var hasCommentary = commentaryHtml && commentaryHtml.trim() !== '';
                        var hasTools = toolsHtml && toolsHtml.trim() !== '';
                        if (!hasReasoning && !hasCommentary && !hasTools) {
                            return;
                        }

                        var shell = document.createElement('div');
                        shell.className = 'assistant-process';

                        var toggle = document.createElement('button');
                        toggle.type = 'button';
                        toggle.className = 'assistant-process-toggle';

                        var summary = document.createElement('span');
                        summary.className = 'assistant-process-summary';
                        summary.textContent = summaryText ? summaryText : 'Process';

                        var chevron = document.createElement('span');
                        chevron.className = 'assistant-process-chevron';
                        chevron.textContent = '▸';

                        toggle.appendChild(summary);
                        toggle.appendChild(chevron);

                        var body = document.createElement('div');
                        body.className = 'assistant-process-body';

                        function appendSection(title, html) {
                            if (!html || html.trim() === '') {
                                return;
                            }
                            var section = document.createElement('div');
                            section.className = 'assistant-process-section';

                            var titleNode = document.createElement('div');
                            titleNode.className = 'assistant-process-title';
                            titleNode.textContent = title;

                            var contentNode = document.createElement('div');
                            contentNode.className = 'assistant-process-content';
                            contentNode.innerHTML = html;

                            section.appendChild(titleNode);
                            section.appendChild(contentNode);
                            body.appendChild(section);
                        }

                        appendSection(reasoningTitle, reasoningHtml);
                        appendSection(commentaryTitle, commentaryHtml);
                        appendSection('Tools', toolsHtml);

                        toggle.onclick = function() {
                            var expanded = body.classList.contains('open');
                            if (expanded) {
                                body.classList.remove('open');
                                chevron.textContent = '▸';
                            } else {
                                body.classList.add('open');
                                chevron.textContent = '▾';
                            }
                        };

                        shell.appendChild(toggle);
                        shell.appendChild(body);
                        target.appendChild(shell);
                    }

                    function showAssistantPermissionRequest(requestId, title, message, approveLabel, approveAlwaysLabel, denyLabel) {
                        var chatContainer = document.getElementById('chat-container');
                        if (!chatContainer || !requestId) {
                            return;
                        }

                        var messageId = 'assistant-permission-' + requestId;
                        var card = document.getElementById(messageId);
                        if (!card) {
                            card = document.createElement('div');
                            card.className = 'chat-message assistant permission-message';
                            card.id = messageId;
                            card.dataset.transient = 'true';

                            var shell = document.createElement('div');
                            shell.className = 'permission-shell';

                            var header = document.createElement('div');
                            header.className = 'permission-header';

                            var titleNode = document.createElement('div');
                            titleNode.className = 'permission-title';
                            titleNode.textContent = title;
                            header.appendChild(titleNode);

                            var badge = document.createElement('div');
                            badge.className = 'permission-badge';
                            badge.textContent = 'Approval';
                            header.appendChild(badge);
                            shell.appendChild(header);

                            var body = document.createElement('div');
                            body.className = 'permission-message-body';
                            body.textContent = message;
                            shell.appendChild(body);

                            var actions = document.createElement('div');
                            actions.className = 'permission-actions';

                            function createButton(label, cssClass, actionName) {
                                var button = document.createElement('button');
                                button.type = 'button';
                                button.className = cssClass;
                                button.textContent = label;
                                button.onclick = function() {
                                    markAssistantPermissionRequestPending(requestId, label + '...');
                                    if (window.alinaBrowserBridge && window.alinaBrowserBridge.handlePermissionAction) {
                                        window.alinaBrowserBridge.handlePermissionAction(requestId, actionName);
                                    } else if (window.alert) {
                                        window.alert('__ALINA_PERMISSION__|' + requestId + '|' + actionName);
                                    }
                                };
                                return button;
                            }

                            actions.appendChild(createButton(approveLabel, 'primary', 'APPROVE_ONCE'));
                            actions.appendChild(createButton(approveAlwaysLabel, 'always', 'APPROVE_ALWAYS'));
                            actions.appendChild(createButton(denyLabel, 'deny', 'DENY'));
                            shell.appendChild(actions);

                            var status = document.createElement('div');
                            status.className = 'permission-status';
                            status.id = messageId + '-status';
                            shell.appendChild(status);

                            card.appendChild(shell);

                            chatContainer.appendChild(card);
                        }

                        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                    }

                    function markAssistantPermissionRequestPending(requestId, statusLabel) {
                        var card = document.getElementById('assistant-permission-' + requestId);
                        if (!card) {
                            return;
                        }
                        var buttons = card.querySelectorAll('button');
                        Array.prototype.forEach.call(buttons, function(button) {
                            button.disabled = true;
                        });
                        var status = document.getElementById('assistant-permission-' + requestId + '-status');
                        if (status) {
                            status.textContent = statusLabel ? statusLabel : '';
                        }
                        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
                    }

                    function resolveAssistantPermissionRequest(requestId, statusLabel) {
                        var card = document.getElementById('assistant-permission-' + requestId);
                        if (!card) {
                            return;
                        }
                        var buttons = card.querySelectorAll('button');
                        Array.prototype.forEach.call(buttons, function(button) {
                            button.disabled = true;
                        });
                        var status = document.getElementById('assistant-permission-' + requestId + '-status');
                        if (status) {
                            status.textContent = statusLabel ? statusLabel : '';
                        }
                        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
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

                    function attachMessageFooter(footerText) {
                        var chatContainer = document.getElementById('chat-container');
                        if (!chatContainer || !chatContainer.children) return;
                        var target = null;
                        for (var i = chatContainer.children.length - 1; i >= 0; i--) {
                            var node = chatContainer.children[i];
                            if (!node || !node.classList) continue;
                            if (node.classList.contains('chat-message')
                                && node.classList.contains('assistant')
                                && (!node.dataset || node.dataset.transient !== 'true')) {
                                target = node;
                                break;
                            }
                        }
                        if (!target) return;
                        var existing = target.querySelector('.message-footer');
                        if (existing) existing.remove();
                        var footer = document.createElement('div');
                        footer.className = 'message-footer';
                        footer.textContent = footerText;
                        target.appendChild(footer);
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
                            if (node.classList.contains('chat-message')
                                && node.classList.contains('assistant')
                                && (!node.dataset || node.dataset.transient !== 'true')) {
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

    public interface PermissionActionHandler {
        void onPermissionAction(String requestId, String actionName);
    }

    public final class BrowserBridge {
        public void handlePermissionAction(final String requestId, final String actionName) {
            if (permissionActionHandler == null) {
                return;
            }
            permissionActionHandler.onPermissionAction(requestId, actionName);
        }
    }

}
