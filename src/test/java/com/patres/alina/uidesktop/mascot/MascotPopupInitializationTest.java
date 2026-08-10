package com.patres.alina.uidesktop.mascot;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class MascotPopupInitializationTest {

    @Test
    void queuesInitializationAndCloseWithoutWaitingForSwing() {
        final List<Runnable> swingTasks = new ArrayList<>();
        final AtomicInteger starterCalls = new AtomicInteger();

        final MascotPopup popup = new MascotPopup(
                MascotPalette::calmLight,
                swingTasks::add,
                starterCalls::incrementAndGet
        );

        assertThat(swingTasks).hasSize(1);
        assertThat(starterCalls).hasValue(1);

        popup.close();

        assertThat(swingTasks).hasSize(2);
    }
}
