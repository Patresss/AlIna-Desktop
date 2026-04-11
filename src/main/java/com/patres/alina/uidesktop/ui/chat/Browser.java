package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.message.ChatMessageRole;
import com.patres.alina.common.message.ChatMessageStyleType;
import com.patres.alina.common.message.CommandUsageInfo;
import com.patres.alina.common.message.TodoItem;
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
import java.util.function.Consumer;

import static com.patres.alina.uidesktop.util.MarkdownToHtmlConverter.convertMarkdownToHtml;

public class Browser extends StackPane {

    private static final Logger logger = LoggerFactory.getLogger(Browser.class);

    private static final String CHAT_CSS = loadResource("browser-chat.css");
    private static final String CHAT_JS = loadResource("browser-chat.js");
    private static final String CHAT_HTML_TEMPLATE = loadResource("browser-chat.html");

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
    private final Consumer<ThemeEvent> themeEventConsumer = ignored -> updateCssColors();

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
        DefaultEventBus.getInstance().subscribe(ThemeEvent.class, themeEventConsumer);
    }

    public void setPermissionActionHandler(final PermissionActionHandler permissionActionHandler) {
        this.permissionActionHandler = permissionActionHandler;
    }

    /**
     * Releases resources held by this Browser: unsubscribes from events,
     * clears the WebEngine content, and removes the WebView from the scene graph.
     * Should be called when the owning tab is closed.
     */
    public void dispose() {
        DefaultEventBus.getInstance().unsubscribe(ThemeEvent.class, themeEventConsumer);
        pendingActions.clear();
        webViewReady = false;
        Platform.runLater(() -> {
            webEngine.load(null);
            getChildren().remove(webView);
        });
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

    public void showAssistantActivity(final String label, final String detail) {
        safeJavaScriptCall("showAssistantActivity", label, detail == null ? "" : detail);
    }

    /**
     * Displays or updates the todo list panel in the chat.
     * Serialises the list of {@link TodoItem}s into a JSON array string
     * and passes it to the JavaScript side for rendering.
     *
     * @param items the current todo items
     * @param title the localised title for the panel header
     */
    public void showTodoList(final java.util.List<TodoItem> items, final String title) {
        final StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            final TodoItem item = items.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("{\"content\":\"").append(escapeJsonString(item.content()))
                    .append("\",\"status\":\"").append(escapeJsonString(item.status()))
                    .append("\",\"priority\":\"").append(escapeJsonString(item.priority()))
                    .append("\"}");
        }
        json.append("]");
        safeJavaScriptCall("showTodoList", json.toString(), title == null ? "Todo" : title);
    }

    private static String escapeJsonString(final String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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

    public void finalizeTodoList() {
        executeJavaScript("finalizeTodoList()");
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

    public void clearTodoList() {
        executeJavaScript("clearTodoList()");
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
        final String iconFontCss = buildIconFontFaceCss();
        return CHAT_HTML_TEMPLATE.formatted(iconFontCss, getCssStyles(), getJavaScript());
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
        return CHAT_CSS;
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

    private static String loadResource(final String resourceName) {
        try (var stream = Browser.class.getResourceAsStream(resourceName)) {
            if (stream == null) {
                logger.error("Cannot find resource: {}", resourceName);
                return "";
            }
            return new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (final IOException e) {
            logger.error("Cannot read resource: {}", resourceName, e);
            return "";
        }
    }

    private String getJavaScript() {
        return CHAT_JS;
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
