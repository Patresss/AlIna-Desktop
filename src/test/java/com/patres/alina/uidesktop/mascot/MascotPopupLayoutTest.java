package com.patres.alina.uidesktop.mascot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MascotPopupLayoutTest {

    @Test
    void usesOneNotificationProfileForEveryState() {
        final MascotPopupLayout approval = MascotPopupLayout.forType(MascotNotificationType.APPROVAL);
        final MascotPopupLayout complete = MascotPopupLayout.forType(MascotNotificationType.COMPLETE);
        final MascotPopupLayout error = MascotPopupLayout.forType(MascotNotificationType.ERROR);

        assertThat(approval.width()).isEqualTo(468);
        assertThat(approval.height()).isEqualTo(184);
        assertThat(complete).isEqualTo(approval);
        assertThat(error).isEqualTo(approval);
        assertThat(approval.mascotIconSize()).isEqualTo(116);
    }
}
