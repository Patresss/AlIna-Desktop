package com.patres.alina.server.codex;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.patres.alina.common.event.ChatThreadTitleUpdatedEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.FileManager;
import com.patres.alina.common.thread.ChatThread;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class CodexAgentRuntimeThreadTitleTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldPreferCodexNameOverPreview() throws Exception {
        final ChatThread thread = loadThread(codexThread("thread-1")
                .put("name", "Codex title")
                .put("preview", "First user message"));

        assertEquals("Codex title", thread.name());
    }

    @Test
    void shouldNormalizePreviewWhenCodexNameIsMissing() throws Exception {
        final ChatThread thread = loadThread(codexThread("thread-1")
                .putNull("name")
                .put("preview", "  First\n\tuser   message  "));

        assertEquals("First user message", thread.name());
    }

    @Test
    void shouldTruncatePreviewWithoutSplittingUnicodeCodePoints() throws Exception {
        final String preview = "🙂".repeat(81);

        final ChatThread thread = loadThread(codexThread("thread-1")
                .putNull("name")
                .put("preview", preview));

        assertEquals("🙂".repeat(79) + "…", thread.name());
        assertEquals(80, thread.name().codePointCount(0, thread.name().length()));
    }

    @Test
    void shouldUseUuidWhenNameAndPreviewAreUnavailable() throws Exception {
        final ChatThread blankPreview = loadThread(codexThread("thread-blank")
                .put("name", "  ")
                .put("preview", "\n\t"));
        final ChatThread malformedPreview = loadThread(codexThread("thread-malformed")
                .putNull("name")
                .put("preview", 42));

        assertEquals("thread-blank", blankPreview.name());
        assertEquals("thread-malformed", malformedPreview.name());
    }

    @Test
    void shouldPublishCurrentAndCompatibleThreadNameNotifications() {
        final CodexAppServerClient client = mock(CodexAppServerClient.class);
        runtime(client);
        final Consumer<JsonNode> serverListener = serverListener(client);
        final List<ChatThreadTitleUpdatedEvent> events = new ArrayList<>();
        final Consumer<ChatThreadTitleUpdatedEvent> titleListener = events::add;
        DefaultEventBus.getInstance().subscribe(ChatThreadTitleUpdatedEvent.class, titleListener);

        try {
            serverListener.accept(threadStarted("codex-1"));
            serverListener.accept(notification("thread/name/updated",
                    objectMapper.createObjectNode()
                            .put("threadId", "codex-1")
                            .put("threadName", "Current title")));
            serverListener.accept(notification("thread/name/updated",
                    objectMapper.createObjectNode()
                            .put("threadId", "codex-1")
                            .put("name", "Compatible title")));
            final ObjectNode nestedParams = objectMapper.createObjectNode();
            nestedParams.putObject("thread")
                    .put("id", "codex-1")
                    .put("name", "Nested title");
            serverListener.accept(notification("thread/name/updated", nestedParams));
        } finally {
            DefaultEventBus.getInstance().unsubscribe(ChatThreadTitleUpdatedEvent.class, titleListener);
        }

        assertEquals(List.of("Current title", "Compatible title", "Nested title"),
                events.stream().map(ChatThreadTitleUpdatedEvent::getNewTitle).toList());
        assertTrue(events.stream().allMatch(event -> event.getThreadId().equals("codex-1")));
    }

    @Test
    void shouldIgnoreTitleNotificationForUnknownThread() {
        final CodexAppServerClient client = mock(CodexAppServerClient.class);
        runtime(client);
        final Consumer<JsonNode> serverListener = serverListener(client);
        final List<ChatThreadTitleUpdatedEvent> events = new ArrayList<>();
        final Consumer<ChatThreadTitleUpdatedEvent> titleListener = events::add;
        DefaultEventBus.getInstance().subscribe(ChatThreadTitleUpdatedEvent.class, titleListener);

        try {
            serverListener.accept(notification("thread/name/updated",
                    objectMapper.createObjectNode()
                            .put("threadId", "unknown")
                            .put("threadName", "Unroutable title")));
        } finally {
            DefaultEventBus.getInstance().unsubscribe(ChatThreadTitleUpdatedEvent.class, titleListener);
        }

        assertTrue(events.isEmpty());
    }

    private ChatThread loadThread(final ObjectNode threadNode) throws Exception {
        final CodexAppServerClient client = mock(CodexAppServerClient.class);
        final ObjectNode response = objectMapper.createObjectNode();
        response.putArray("data").add(threadNode);
        when(client.request(eq("thread/list"), any(JsonNode.class))).thenReturn(response);

        final List<ChatThread> threads = runtime(client).getChatThreads();

        assertEquals(1, threads.size());
        return threads.getFirst();
    }

    private ObjectNode codexThread(final String id) {
        return objectMapper.createObjectNode()
                .put("id", id)
                .put("createdAt", 1_784_204_818L)
                .put("updatedAt", 1_784_207_674L);
    }

    private Consumer<JsonNode> serverListener(final CodexAppServerClient client) {
        final ArgumentCaptor<Consumer<JsonNode>> listenerCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(client).addMessageListener(listenerCaptor.capture());
        return listenerCaptor.getValue();
    }

    private ObjectNode threadStarted(final String threadId) {
        final ObjectNode params = objectMapper.createObjectNode();
        params.putObject("thread").put("id", threadId);
        return notification("thread/started", params);
    }

    private ObjectNode notification(final String method, final ObjectNode params) {
        return objectMapper.createObjectNode()
                .put("method", method)
                .set("params", params);
    }

    private CodexAgentRuntime runtime(final CodexAppServerClient client) {
        return new CodexAgentRuntime(
                client,
                mock(FileManager.class),
                mock(FileManager.class),
                objectMapper
        );
    }
}
