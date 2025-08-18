package com.patres.alina.uidesktop.plugin.settings;

import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.Event;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.ListView;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;

/*
 * https://stackoverflow.com/questions/19924852/autocomplete-combobox-in-javafx
 */
public class AutoCompleteComboBox {

    public static class HideableItem<T> {

        private final ObjectProperty<T> object = new SimpleObjectProperty<>();
        private final BooleanProperty hidden = new SimpleBooleanProperty();
        private StringConverter converter;

        public HideableItem(T object, StringConverter converter) {
            setConverter(converter);
            setObject(object);

        }

        private ObjectProperty<T> objectProperty() {
            return this.object;
        }

        public T getObject() {
            return this.objectProperty().get();
        }

        private void setObject(T object) {
            this.objectProperty().set(object);
        }

        private BooleanProperty hiddenProperty() {
            return this.hidden;
        }

        private boolean isHidden() {
            return this.hiddenProperty().get();
        }

        private void setHidden(boolean hidden) {
            this.hiddenProperty().set(hidden);
        }

        private void setConverter(StringConverter converter) {
            this.converter = converter;
        }

        @Override
        public String toString() {
            return getObject() == null ? null : converter.toString(this);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            HideableItem<?> that = (HideableItem<?>) o;
            return Objects.equals(object.getValue(), that.object.getValue());
        }

        @Override
        public int hashCode() {
            return Objects.hash(object.getValue());
        }
    }

    public static <T> ComboBox<HideableItem<T>> createComboBoxWithAutoCompletionSupport(List<T> items, StringConverter converter) {
        ObservableList<HideableItem<T>> hideableHideableItems = FXCollections.observableArrayList(hideableItem -> new Observable[]{hideableItem.hiddenProperty()});

        for (T item : items) {
            HideableItem<T> hideableItem = new HideableItem<>(item, converter);
            hideableHideableItems.add(hideableItem);
        }

        FilteredList<HideableItem<T>> filteredHideableItems = new FilteredList<>(hideableHideableItems, t -> !t.isHidden());

        ComboBox<HideableItem<T>> comboBox = new ComboBox<>();
        comboBox.setItems(filteredHideableItems);
        comboBox.setConverter(converter);

        ComboBoxListViewSkin<HideableItem<T>> comboBoxListViewSkin = new ComboBoxListViewSkin<HideableItem<T>>(comboBox);
        comboBoxListViewSkin.getPopupContent().addEventFilter(KeyEvent.ANY, (KeyEvent event) -> {
            if (event.getCode() == KeyCode.SPACE) {
                event.consume();
            }
        });
        comboBox.setSkin(comboBoxListViewSkin);

        @SuppressWarnings("unchecked") HideableItem<T>[] selectedItem = (HideableItem<T>[]) new HideableItem[1];

        comboBox.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (!comboBox.isShowing() || event.isShiftDown() || event.isControlDown()) {
                return;
            }
            comboBox.setEditable(true);
            comboBox.getEditor().clear();
        });

        comboBox.showingProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                @SuppressWarnings("unchecked") ListView<HideableItem> lv = (ListView<HideableItem>) ((ComboBoxListViewSkin<?>) comboBox.getSkin()).getPopupContent();

                Platform.runLater(() -> {
                    if (selectedItem[0] == null) // first use
                    {
                        double cellHeight = ((Control) lv.lookup(".list-cell")).getHeight();
                        lv.setFixedCellSize(cellHeight);
                    }
                });

                lv.scrollTo(comboBox.getValue());
            } else {

                HideableItem<T> value = comboBox.getValue();
                if (value != null) {
                    selectedItem[0] = value;
                }

                comboBox.setEditable(false);
                if (value != null) {
                    Platform.runLater(() -> {
                        comboBox.getSelectionModel().select(selectedItem[0]);
                        comboBox.setValue(selectedItem[0]);
                    });
                }

            }
        });

        comboBox.setOnHidden((Event event) -> {
            for (HideableItem item : hideableHideableItems) {
                item.setHidden(false);
            }
        });
        comboBox.valueProperty().addListener((ObservableValue<? extends HideableItem<T>> obs, HideableItem<T> oldValue, HideableItem<T> newValue) -> {
            if (newValue == null) {
                for (HideableItem item : hideableHideableItems) {
                    item.setHidden(false);
                }
            }
        });

        comboBox.getEditor().textProperty().addListener((ObservableValue<? extends String> obs, String oldValue, String newValue) -> {
            if (!comboBox.isShowing()) {
                return;
            }

            Platform.runLater(() -> {
                if (comboBox.getSelectionModel().getSelectedItem() == null) {
                    for (HideableItem item : hideableHideableItems) {
                        item.setHidden(!StringUtils.containsIgnoreCase(item.toString(), newValue));
                    }
                } else {

                    boolean validText = false;

                    for (HideableItem hideableItem : hideableHideableItems) {
                        if (hideableItem.toString().equals(newValue)) {
                            validText = true;
                            break;
                        }
                    }

                    if (!validText) {
                        comboBox.getSelectionModel().select(null);
                    }
                }
            });
        });

        return comboBox;
    }
}