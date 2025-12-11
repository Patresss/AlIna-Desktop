package com.patres.alina.uidesktop.ui.util;

public interface ShortcutExecutor {

    int STEP_DELAY_MS = 60;
    int PRE_KEY_DELAY_MS = 80;
    int DEFAULT_HOLD_MS = 220;

    boolean sendCopy();
    boolean sendPaste();

}
