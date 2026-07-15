package com.patres.alina.uidesktop.ui.calendar;

import java.util.regex.Pattern;

/** Plain-text normalization and word-safe previewing for Calendar descriptions. */
public final class CalendarDescriptionText {

    private static final Pattern BREAK_TAG = Pattern.compile("(?i)<br\\s*/?>|</p\\s*>");
    private static final Pattern HTML_TAG = Pattern.compile("<[^>]+>");
    private static final Pattern INLINE_WHITESPACE = Pattern.compile("[\\t\\x0B\\f ]+");
    private static final Pattern PREVIEW_WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern EXTRA_LINE_BREAKS = Pattern.compile("\\n{3,}");

    private CalendarDescriptionText() {
    }

    public static String toPlainText(final String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String text = value.replace("\r\n", "\n").replace('\r', '\n');
        text = BREAK_TAG.matcher(text).replaceAll("\n");
        text = HTML_TAG.matcher(text).replaceAll(" ");
        text = decodeCommonEntities(text);
        final String[] lines = text.split("\n", -1);
        final StringBuilder normalized = new StringBuilder();
        for (final String line : lines) {
            final String cleanLine = INLINE_WHITESPACE.matcher(line).replaceAll(" ").strip();
            if (!normalized.isEmpty()) {
                normalized.append('\n');
            }
            normalized.append(cleanLine);
        }
        return EXTRA_LINE_BREAKS.matcher(normalized.toString().strip()).replaceAll("\n\n");
    }

    public static String preview(final String plainText, final int limit) {
        final String normalized = PREVIEW_WHITESPACE.matcher(
                plainText == null ? "" : plainText
        ).replaceAll(" ").strip();
        if (normalized.length() <= limit) {
            return normalized;
        }
        final String candidate = normalized.substring(0, Math.max(1, limit));
        final int wordBoundary = candidate.lastIndexOf(' ');
        final String shortened = wordBoundary > 0 ? candidate.substring(0, wordBoundary) : candidate;
        return shortened.stripTrailing() + "…";
    }

    private static String decodeCommonEntities(final String value) {
        return value
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }
}
