package com.patres.alina.uidesktop.util;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import java.util.List;

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

    public static String convertMarkdownToHtml(final String markdown) {
        if (markdown == null) {
            return null;
        }
        org.commonmark.node.Node document = PARSER.parse(markdown);
        return RENDER.render(document).trim();
    }

}
