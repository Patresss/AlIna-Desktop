package com.patres.alina.server.openai.function.notion.quicknote;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record NotionQuickNoteRequest(@JsonPropertyDescription("Title of the note")
                                     @JsonProperty(required = true)
                                     String title,
                                     @JsonPropertyDescription("Exact, to the word message from the user, that needs to be added to the Notion as a quick note. Just fix typos")
                                     @JsonProperty(required = true)
                                     String content,
                                     @JsonPropertyDescription("Emoji that describes the note")
                                     String emoji) {
}