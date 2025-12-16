package com.patres.alina.uidesktop.shortcuts.key;


import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;

import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum KeyboardKey {

    ENTER(KeyEvent.VK_ENTER),
    BACK_SPACE(KeyEvent.VK_BACK_SPACE),
    TAB(KeyEvent.VK_TAB),
    CANCEL(KeyEvent.VK_CANCEL),
    CLEAR(KeyEvent.VK_CLEAR),
    SHIFT(KeyEvent.VK_SHIFT, KeyboardKeyType.MODE_TYPE),
    CONTROL(KeyEvent.VK_CONTROL, KeyboardKeyType.MODE_TYPE),
    ALT(KeyEvent.VK_ALT, KeyboardKeyType.MODE_TYPE),
    PAUSE(KeyEvent.VK_PAUSE),
    CAPS_LOCK(KeyEvent.VK_CAPS_LOCK),
    ESCAPE(KeyEvent.VK_ESCAPE),
    SPACE(KeyEvent.VK_SPACE),
    PAGE_UP(KeyEvent.VK_PAGE_UP),
    PAGE_DOWN(KeyEvent.VK_PAGE_DOWN),
    END(KeyEvent.VK_END),
    HOME(KeyEvent.VK_HOME),

    LEFT(KeyEvent.VK_LEFT),
    UP(KeyEvent.VK_UP),
    RIGHT(KeyEvent.VK_RIGHT),
    DOWN(KeyEvent.VK_DOWN),

    COMMA(KeyEvent.VK_COMMA),
    MINUS(KeyEvent.VK_MINUS),
    PERIOD(KeyEvent.VK_PERIOD),
    SLASH(KeyEvent.VK_SLASH),

    KEY_0(KeyEvent.VK_0),
    KEY_1(KeyEvent.VK_1),
    KEY_2(KeyEvent.VK_2),
    KEY_3(KeyEvent.VK_3),
    KEY_4(KeyEvent.VK_4),
    KEY_5(KeyEvent.VK_5),
    KEY_6(KeyEvent.VK_6),
    KEY_7(KeyEvent.VK_7),
    KEY_8(KeyEvent.VK_8),
    KEY_9(KeyEvent.VK_9),

    SEMICOLON(KeyEvent.VK_SEMICOLON),
    EQUALS(KeyEvent.VK_EQUALS),

    A(KeyEvent.VK_A),
    B(KeyEvent.VK_B),
    C(KeyEvent.VK_C),
    D(KeyEvent.VK_D),
    E(KeyEvent.VK_E),
    F(KeyEvent.VK_F),
    G(KeyEvent.VK_G),
    H(KeyEvent.VK_H),
    I(KeyEvent.VK_I),
    J(KeyEvent.VK_J),
    K(KeyEvent.VK_K),
    L(KeyEvent.VK_L),
    M(KeyEvent.VK_M),
    N(KeyEvent.VK_N),
    O(KeyEvent.VK_O),
    P(KeyEvent.VK_P),
    Q(KeyEvent.VK_Q),
    R(KeyEvent.VK_R),
    S(KeyEvent.VK_S),
    T(KeyEvent.VK_T),
    U(KeyEvent.VK_U),
    V(KeyEvent.VK_V),
    W(KeyEvent.VK_W),
    X(KeyEvent.VK_X),
    Y(KeyEvent.VK_Y),
    Z(KeyEvent.VK_Z),

    OPEN_BRACKET(KeyEvent.VK_OPEN_BRACKET),
    BACK_SLASH(KeyEvent.VK_BACK_SLASH),
    CLOSE_BRACKET(KeyEvent.VK_CLOSE_BRACKET),

    NUMPAD0(KeyEvent.VK_NUMPAD0),
    NUMPAD1(KeyEvent.VK_NUMPAD1),
    NUMPAD2(KeyEvent.VK_NUMPAD2),
    NUMPAD3(KeyEvent.VK_NUMPAD3),
    NUMPAD4(KeyEvent.VK_NUMPAD4),
    NUMPAD5(KeyEvent.VK_NUMPAD5),
    NUMPAD6(KeyEvent.VK_NUMPAD6),
    NUMPAD7(KeyEvent.VK_NUMPAD7),
    NUMPAD8(KeyEvent.VK_NUMPAD8),
    NUMPAD9(KeyEvent.VK_NUMPAD9),
    MULTIPLY(KeyEvent.VK_MULTIPLY),
    ADD(KeyEvent.VK_ADD),

    SEPARATOR(KeyEvent.VK_SEPARATOR),

    SUBTRACT(KeyEvent.VK_SUBTRACT),
    DECIMAL(KeyEvent.VK_DECIMAL),
    DIVIDE(KeyEvent.VK_DIVIDE),
    DELETE(KeyEvent.VK_DELETE),
    NUM_LOCK(KeyEvent.VK_NUM_LOCK),
    SCROLL_LOCK(KeyEvent.VK_SCROLL_LOCK),

    F1(KeyEvent.VK_F1),
    F2(KeyEvent.VK_F2),
    F3(KeyEvent.VK_F3),
    F4(KeyEvent.VK_F4),
    F5(KeyEvent.VK_F5),
    F6(KeyEvent.VK_F6),
    F7(KeyEvent.VK_F7),
    F8(KeyEvent.VK_F8),
    F9(KeyEvent.VK_F9),
    F10(KeyEvent.VK_F10),
    F11(KeyEvent.VK_F11),
    F12(KeyEvent.VK_F12),

    PRINTSCREEN(KeyEvent.VK_PRINTSCREEN),
    INSERT(KeyEvent.VK_INSERT),
    HELP(KeyEvent.VK_HELP),
    META(KeyEvent.VK_META, KeyboardKeyType.MODE_TYPE),
    BACK_QUOTE(KeyEvent.VK_BACK_QUOTE),
    QUOTE(KeyEvent.VK_QUOTE),

    WINDOWS(KeyEvent.VK_WINDOWS, KeyboardKeyType.MODE_TYPE),
    CONTEXT_MENU(KeyEvent.VK_CONTEXT_MENU);

    private static final KeyAdapter keyAdapter = new KeyAdapter();
    private final Integer keyValue;
    private final String keyName;
    private final String macKeyName;
    private final KeyboardKeyType keyType;

    KeyboardKey(Integer keyValue) {
        this.keyValue = keyValue;
        this.keyName = KeyEvent.getKeyText(keyValue);
        this.macKeyName = null;
        this.keyType = KeyboardKeyType.EXECUTE_TYPE;
    }

    KeyboardKey(Integer keyValue, KeyboardKeyType keyType) {
        this.keyValue = keyValue;
        this.keyName = KeyEvent.getKeyText(keyValue);
        this.macKeyName = getMacOSKeyName(this);
        this.keyType = keyType;
    }

    public Integer getKeyValue() {
        return keyValue;
    }

    public String getKeyName() {
        if (isMacOS() && macKeyName != null) {
            return macKeyName;
        }
        return keyName;
    }

    private static boolean isMacOS() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac");
    }

    private static String getMacOSKeyName(KeyboardKey key) {
        return switch (key.name()) {
            case "META" -> "Command (⌘)";
            case "WINDOWS" -> "Command (⌘)";
            case "ALT" -> "Option (⌥)";
            case "CONTROL" -> "Control (^)";
            case "SHIFT" -> "Shift (⇧)";
            default -> null;
        };
    }

    public static Integer getKeyEvent(NativeKeyEvent nativeEvent) {
        return keyAdapter.getKeyEvent(nativeEvent);
    }

    public static Optional<KeyboardKey> getKeyEventAsKeyboardKey(NativeKeyEvent nativeEvent) {
        Integer keyValue = keyAdapter.getKeyEvent(nativeEvent);
        return findByKeyValue(keyValue);
    }

    private static Optional<KeyboardKey> findByKeyValue(Integer key) {
        return Arrays.stream(values())
                .filter(it -> it.getKeyValue().equals(key) )
                .findFirst();
    }

    public static String getKeysName(final List<KeyboardKey> keys) {
        if (keys == null) {
            return "";
        }
        return " [" + keys.stream()
                .map(KeyboardKey::getKeyName)
                .collect(Collectors.joining(" + ")) + "]";
    }


    public static List<KeyboardKey> getValuesByType(KeyboardKeyType keyType) {
        return Arrays.stream(values())
                .filter(keyboardKey -> keyboardKey.keyType == keyType)
                .toList();
    }

}
