package com.patres.alina.server.opencode;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenCodeRuntimeServiceTest {

    @Test
    void shouldNotEmitUpdatedTextWhenPartAlreadyStreamedViaDelta() {
        final Map<String, String> textParts = new ConcurrentHashMap<>();
        final Set<String> deltaTextParts = ConcurrentHashMap.newKeySet();
        deltaTextParts.add("part-1");

        final String delta = OpenCodeRuntimeService.resolveUpdatedTextDelta(
                textParts,
                deltaTextParts,
                "part-1",
                "Jestem Alina."
        );

        assertEquals("", delta);
        assertEquals("Jestem Alina.", textParts.get("part-1"));
    }

    @Test
    void shouldEmitSuffixFromUpdatedTextWhenNoDeltaWasSeen() {
        final Map<String, String> textParts = new ConcurrentHashMap<>();
        final Set<String> deltaTextParts = ConcurrentHashMap.newKeySet();
        textParts.put("part-1", "Jestem");

        final String delta = OpenCodeRuntimeService.resolveUpdatedTextDelta(
                textParts,
                deltaTextParts,
                "part-1",
                "Jestem Alina."
        );

        assertEquals(" Alina.", delta);
        assertEquals("Jestem Alina.", textParts.get("part-1"));
    }

    @Test
    void shouldRecognizeReasoningPartFromPartTypeMap() {
        final Map<String, String> partTypes = new ConcurrentHashMap<>();
        partTypes.put("part-1", "reasoning");
        partTypes.put("part-2", "text");

        assertTrue(OpenCodeRuntimeService.isReasoningPart(partTypes, "part-1"));
        assertFalse(OpenCodeRuntimeService.isReasoningPart(partTypes, "part-2"));
        assertFalse(OpenCodeRuntimeService.isReasoningPart(partTypes, "missing"));
    }
}
