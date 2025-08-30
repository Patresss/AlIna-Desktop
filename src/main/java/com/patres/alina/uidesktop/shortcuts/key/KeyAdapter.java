package com.patres.alina.uidesktop.shortcuts.key;


import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.SwingKeyAdapter;

public class KeyAdapter extends SwingKeyAdapter {

    public Integer getKeyEvent(NativeKeyEvent nativeEvent) {
        return getJavaKeyEvent(nativeEvent).getKeyCode();
    }

}