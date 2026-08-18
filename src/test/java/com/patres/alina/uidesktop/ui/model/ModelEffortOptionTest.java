package com.patres.alina.uidesktop.ui.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelEffortOptionTest {

    @Test
    void shouldBuildStableChoicesWhenBackendDoesNotAdvertiseEfforts() {
        final List<ModelEffortOption> options = ModelEffortOption.choices("Backend default", List.of());

        assertEquals(List.of("", "low", "medium", "high", "xhigh", "max"),
                options.stream().map(ModelEffortOption::value).toList());
    }

    @Test
    void shouldSelectCaseInsensitivelyAndKeepUnknownValues() {
        final List<ModelEffortOption> options = ModelEffortOption.choices("Default", List.of("low", "high"));

        assertEquals("high", ModelEffortOption.select(options, " HIGH ").value());
        assertEquals("experimental", ModelEffortOption.select(options, "experimental").value());
        assertEquals("Extra High", ModelEffortOption.humanize("xhigh"));
    }

    @Test
    void shouldUseTheFriendlyLabelWhenRenderedByJavaFxControls() {
        assertEquals("High", new ModelEffortOption("high", "High").toString());
    }
}
