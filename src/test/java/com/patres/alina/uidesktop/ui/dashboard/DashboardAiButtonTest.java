package com.patres.alina.uidesktop.ui.dashboard;

import com.patres.alina.common.event.CalendarAiPromptEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardAiButtonTest {

    @Test
    void publishesResolvedPromptImmediately() {
        final AtomicReference<String> message = new AtomicReference<>();
        final Consumer<CalendarAiPromptEvent> subscriber = event -> message.set(event.getMessage());
        final DefaultEventBus eventBus = DefaultEventBus.getInstance();
        eventBus.subscribe(CalendarAiPromptEvent.class, subscriber);
        try {
            DashboardAiButton.publishPrompt("Prepare me:\n$ARGUMENTS", "Event: Demo");

            assertThat(message.get()).isEqualTo("Prepare me:\nEvent: Demo");
        } finally {
            eventBus.unsubscribe(CalendarAiPromptEvent.class, subscriber);
        }
    }

    @Test
    void ignoresBlankPrompt() {
        final AtomicReference<String> message = new AtomicReference<>();
        final Consumer<CalendarAiPromptEvent> subscriber = event -> message.set(event.getMessage());
        final DefaultEventBus eventBus = DefaultEventBus.getInstance();
        eventBus.subscribe(CalendarAiPromptEvent.class, subscriber);
        try {
            DashboardAiButton.publishPrompt("  ", "Event: Demo");

            assertThat(message.get()).isNull();
        } finally {
            eventBus.unsubscribe(CalendarAiPromptEvent.class, subscriber);
        }
    }
}
