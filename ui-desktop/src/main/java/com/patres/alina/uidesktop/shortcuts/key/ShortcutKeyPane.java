package com.patres.alina.uidesktop.shortcuts.key;

import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ShortcutKeyPane {

    private static final int MAX_COMBOBOX = 3;

    private final GridPane pane;

    final List<ComboBox<KeyboardKey>> modeKeysBoxes = new ArrayList<>();
    final ComboBox<KeyboardKey> executeKeyComboBox = createKeyboardKeyComboBox(KeyboardKeyType.EXECUTE_TYPE);

    public ShortcutKeyPane() {
        this.pane = new GridPane();
    }

    public Node createPane() {
        for (int i = 0; i < MAX_COMBOBOX; i++) {
            ComboBox<KeyboardKey> comboBox = createKeyboardKeyComboBox(KeyboardKeyType.MODE_TYPE);
            modeKeysBoxes.add(comboBox);
        }

        for (int i = 1; i < modeKeysBoxes.size(); i++) {
            modeKeysBoxes.get(i).setVisible(false);
        }

        for (int i = 0; i < modeKeysBoxes.size() - 1; i++) {
            addComboBoxListeners(modeKeysBoxes.get(i), modeKeysBoxes.get(i + 1));
        }

        for (int i = 0; i < modeKeysBoxes.size(); i++) {
            pane.add(modeKeysBoxes.get(i), 0, i);
        }

        pane.add(executeKeyComboBox, 1, 0);

        pane.setHgap(2);
        pane.setVgap(10);

        return pane;
    }

    public void setValues(ShortcutKeys shortcutKeys) {
        final List<KeyboardKey> modeKeys = Optional.ofNullable(shortcutKeys)
                .map(ShortcutKeys::getModeKeys)
                .orElse(Collections.singletonList(null));
        for (ComboBox<KeyboardKey> modeKeysBox : modeKeysBoxes) {
            modeKeysBox.getSelectionModel().clearSelection();
        }

        for (int i = 0; i < modeKeysBoxes.size(); i++) {
            if (i <= modeKeys.size() - 1) {
                KeyboardKey keyboardKey = modeKeys.get(i);
                modeKeysBoxes.get(i).getSelectionModel().select(keyboardKey);
            }
        }

        final KeyboardKey executeKey = shortcutKeys == null ? null : shortcutKeys.getExecuteKey();
        executeKeyComboBox.setValue(executeKey);
    }

    public ShortcutKeys getShortcutKeys() {
        final List<KeyboardKey> modeKeys = modeKeysBoxes.stream()
                .map(it -> it.getSelectionModel().getSelectedItem())
                .toList();
        KeyboardKey executeKey = executeKeyComboBox.getSelectionModel().getSelectedItem();
        return new ShortcutKeys(modeKeys, executeKey);
    }


    private static @NotNull ComboBox<KeyboardKey> createKeyboardKeyComboBox(KeyboardKeyType type) {
        ComboBox<KeyboardKey> comboBox = new ComboBox<>();
        comboBox.setItems(FXCollections.observableArrayList(KeyboardKey.getValuesByType(type)));
        comboBox.getItems().addFirst(null);

        comboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(KeyboardKey keyboardKey) {
                if (keyboardKey == null) {
                    return "";
                }
                return keyboardKey.getKeyName();
            }

            @Override
            public KeyboardKey fromString(String string) {
                return Arrays.stream(KeyboardKey.values())
                        .filter(key -> key.getKeyName().equals(string))
                        .findFirst()
                        .orElse(null);
            }
        });
        return comboBox;
    }


    private void addComboBoxListeners(ComboBox<KeyboardKey> currentComboBox, ComboBox<KeyboardKey> nextComboBox) {
        currentComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                nextComboBox.setVisible(true);
            } else {
                nextComboBox.setVisible(false);
                nextComboBox.getSelectionModel().clearSelection();
            }
        });
    }


}
