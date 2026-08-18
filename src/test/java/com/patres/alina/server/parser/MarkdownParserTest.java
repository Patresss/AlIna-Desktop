package com.patres.alina.server.parser;

import com.patres.alina.server.command.Command;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownParserTest {

    private final MarkdownParser parser = new MarkdownParser();

    @Test
    void shouldReadOpenCodeVariantAndLegacyEffortKeys() {
        final MarkdownParser.ParsedCommand variant = parser.parseMarkdownWithFrontmatter("""
                ---
                id: explain
                name: Explain
                model: openai/gpt-5
                variant: xhigh
                ---
                Explain this.
                """, "explain");
        final MarkdownParser.ParsedCommand legacy = parser.parseMarkdownWithFrontmatter("""
                ---
                id: explain
                effort: max
                ---
                Explain this.
                """, "explain");

        assertEquals("xhigh", variant.metadata().effort());
        assertEquals("max", legacy.metadata().effort());
    }

    @Test
    void shouldGeneratePortableVariantMetadata() {
        final Command command = new Command(
                "Explain",
                "",
                "Explain this.",
                "bi-slash",
                "openai/gpt-5",
                "high",
                null,
                null,
                null,
                null
        );

        final String markdown = parser.generateMarkdownWithFrontmatter(command);

        assertTrue(markdown.contains("model: openai/gpt-5"));
        assertTrue(markdown.contains("variant: high"));
        assertTrue(!markdown.contains("effort: high"));
    }
}
