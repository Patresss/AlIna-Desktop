package com.patres.alina.uidesktop.util;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownToHtmlConverter {

    private static final String HTML_NEW_LINE = "<br>";

    private static final List<Extension> EXTENSIONS = List.of(
            TablesExtension.create(),
            AutolinkExtension.create(),
            StrikethroughExtension.create(),
            TaskListItemsExtension.create()
    );

    private static final Parser PARSER = Parser.builder()
            .extensions(EXTENSIONS)
            .build();

    private static final HtmlRenderer RENDER = HtmlRenderer.builder()
            .extensions(EXTENSIONS)
            .softbreak(HTML_NEW_LINE)
            .build();

    /**
     * Regex matching emoji sequences: supplementary plane emoji,
     * common BMP emoji symbols (miscellaneous symbols, dingbats, transport, etc.),
     * zero-width joiners and skin tone modifiers.
     * Variation selectors are stripped before this pattern is applied.
     */
    private static final Pattern EMOJI_PATTERN = Pattern.compile(
            "(" +
            "[\\x{1F600}-\\x{1F64F}" +    // Emoticons
            "\\x{1F300}-\\x{1F5FF}" +      // Misc symbols & pictographs
            "\\x{1F680}-\\x{1F6FF}" +      // Transport & map symbols
            "\\x{1F900}-\\x{1F9FF}" +      // Supplemental symbols
            "\\x{1FA00}-\\x{1FA6F}" +      // Chess symbols
            "\\x{1FA70}-\\x{1FAFF}" +      // Symbols extended-A
            "\\x{2600}-\\x{26FF}" +        // Misc symbols (sun, coffee, etc.)
            "\\x{2700}-\\x{27BF}" +        // Dingbats
            "\\x{200D}" +                  // Zero-width joiner
            "\\x{20E3}" +                  // Combining enclosing keycap
            "\\x{1F1E0}-\\x{1F1FF}" +     // Regional indicator symbols (flags)
            "]+" +
            ")"
    );

    /**
     * Pattern matching blockquotes containing Obsidian-style callout syntax.
     * Captures the callout type (e.g. success, danger, info, question) and
     * the remaining content up to the closing blockquote tag.
     */
    private static final Pattern CALLOUT_BLOCKQUOTE_PATTERN = Pattern.compile(
            "<blockquote>\\s*<p>\\[!(\\w+)]\\s*(.*?)</blockquote>",
            Pattern.DOTALL
    );

    public static String convertMarkdownToHtml(final String markdown) {
        if (markdown == null) {
            return null;
        }
        org.commonmark.node.Node document = PARSER.parse(markdown);
        String html = RENDER.render(document).trim();
        html = convertCalloutsToHtml(html);
        return sanitizeEmojiForWebView(html);
    }

    /**
     * Converts Obsidian-style callout blockquotes into styled HTML div elements.
     * <p>
     * Transforms blockquotes like:
     * <pre>{@code
     * > [!success] Title text
     * > Body text
     * }</pre>
     * into colored callout cards with title and body sections.
     * Supported types include: success, danger, info, question, note, warning, tip, important, caution.
     */
    static String convertCalloutsToHtml(final String html) {
        if (html == null || !html.contains("[!")) {
            return html;
        }
        return CALLOUT_BLOCKQUOTE_PATTERN.matcher(html).replaceAll(matchResult -> {
            String type = matchResult.group(1).toLowerCase();
            String rawContent = matchResult.group(2).trim()
                    .replaceAll("\\s*</p>\\s*$", "").trim();

            String title;
            String body;

            int brIdx = rawContent.indexOf("<br>");
            int pIdx = rawContent.indexOf("</p>");

            int splitIdx;
            int skipLen;
            if (brIdx >= 0 && (pIdx < 0 || brIdx < pIdx)) {
                splitIdx = brIdx;
                skipLen = "<br>".length();
            } else if (pIdx >= 0) {
                splitIdx = pIdx;
                skipLen = "</p>".length();
            } else {
                splitIdx = -1;
                skipLen = 0;
            }

            if (splitIdx >= 0) {
                title = rawContent.substring(0, splitIdx).trim();
                body = rawContent.substring(splitIdx + skipLen).trim()
                        .replaceAll("</p>\\s*<p>", "<br>")
                        .replaceAll("^\\s*<p>", "")
                        .replaceAll("</p>\\s*$", "")
                        .trim();
            } else {
                title = rawContent;
                body = "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("<div class=\"callout callout-").append(type).append("\">");
            sb.append("<div class=\"callout-title\">").append(title).append("</div>");
            if (!body.isEmpty()) {
                sb.append("<div class=\"callout-body\">").append(body).append("</div>");
            }
            sb.append("</div>");
            return Matcher.quoteReplacement(sb.toString());
        });
    }

    /**
     * Sanitizes emoji characters in HTML for proper rendering in JavaFX WebView.
     * <p>
     * The old WebKit engine used by JavaFX WebView has issues with:
     * <ul>
     *   <li>Variation selectors (U+FE0F / U+FE0E) causing layout glitches
     *       that make surrounding text invisible</li>
     *   <li>Compound emoji sequences breaking text metrics</li>
     * </ul>
     * This method strips variation selectors and wraps emoji sequences in
     * {@code <span>} tags with {@code display:inline-block} to isolate their
     * rendering, followed by a zero-width space to prevent layout overflow.
     */
    static String sanitizeEmojiForWebView(final String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }

        // Strip variation selectors that cause rendering issues in old WebKit
        String cleaned = html.replace("\uFE0F", "").replace("\uFE0E", "");

        // Wrap emoji sequences in inline-block spans to isolate their layout
        // from surrounding text, preventing invisible text bugs
        return EMOJI_PATTERN.matcher(cleaned)
                .replaceAll("<span class=\"emoji\">$1</span>\u200B");
    }

}
