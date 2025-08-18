package com.patres.alina.uidesktop.plugin.settings;

import com.patres.alina.uidesktop.ui.theme.ThemeManager;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.text.Font;
import javafx.util.StringConverter;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;
import org.kordamp.ikonli.bootstrapicons.BootstrapIconsIkonHandler;
import org.kordamp.ikonli.devicons.Devicons;
import org.kordamp.ikonli.devicons.DeviconsIkonHandler;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.feather.FeatherIkonHandler;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material2.Material2AL;
import org.kordamp.ikonli.material2.Material2ALIkonHandler;
import org.kordamp.ikonli.material2.Material2MZ;
import org.kordamp.ikonli.material2.Material2MZIkonHandler;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IconComboBox {

    private static final Map<Class<? extends Enum<?>>, String> IKON_FONT_MAP = Map.of(
            Material2AL.class, new Material2ALIkonHandler().getFontFamily(),
            Material2MZ.class, new Material2MZIkonHandler().getFontFamily(),
            Devicons.class, new DeviconsIkonHandler().getFontFamily(),
            Feather.class, new FeatherIkonHandler().getFontFamily(),
            BootstrapIcons.class, new BootstrapIconsIkonHandler().getFontFamily()
    );

    private static final List<ApplicationIcon> ITEMS = getIcons().stream()
            .map(ApplicationIcon::new)
            .collect(Collectors.toCollection(FXCollections::observableArrayList));

    public static final StringConverter<AutoCompleteComboBox.HideableItem<ApplicationIcon>> CONVERTER = new StringConverter<>() {
        @Override
        public String toString(AutoCompleteComboBox.HideableItem<ApplicationIcon> object) {
            return object != null ? object.getObject().displayedName() : null;
        }

        @Override
        public AutoCompleteComboBox.HideableItem<ApplicationIcon> fromString(final String string) {
            final ApplicationIcon foundBadge = ITEMS.stream()
                    .filter(ikon -> ikon.name().equals(string))
                    .findFirst()
                    .orElse(null);
            return new AutoCompleteComboBox.HideableItem<>(foundBadge, this);
        }
    };

    public static ComboBox<AutoCompleteComboBox.HideableItem<ApplicationIcon>> create() {
        final ComboBox<AutoCompleteComboBox.HideableItem<ApplicationIcon>> dropDown = createComboBox();
        configureComboBox(dropDown);
        return dropDown;
    }

    private static List<Ikon> getIcons() {
        return IKON_FONT_MAP.keySet().stream()
                .flatMap(key -> Arrays.stream(key.getEnumConstants()))
                .map(it -> (Ikon) it)
                .sorted(Comparator.comparing(Ikon::getDescription))
                .toList();
    }

    private static ComboBox<AutoCompleteComboBox.HideableItem<ApplicationIcon>> createComboBox() {
        return AutoCompleteComboBox.createComboBoxWithAutoCompletionSupport(ITEMS, CONVERTER);
    }

    private static void configureComboBox(final ComboBox<AutoCompleteComboBox.HideableItem<ApplicationIcon>> dropDown) {
        dropDown.setButtonCell(new IkonButtonCell());
        dropDown.setCellFactory(c -> new IkonCell());
        dropDown.getSelectionModel().selectFirst();
    }

    static class IkonCell extends ListCell<AutoCompleteComboBox.HideableItem<ApplicationIcon>> {
        @Override
        protected void updateItem(final AutoCompleteComboBox.HideableItem<ApplicationIcon> item, final boolean isEmpty) {
            super.updateItem(item, isEmpty);
            if (!isEmpty && item != null && item.getObject() != null) {
                final ApplicationIcon applicationIcon = item.getObject();
                final String fontFamily = IKON_FONT_MAP.get(applicationIcon.ikon().getClass());
                if (fontFamily != null) {
                    setFont(new Font(fontFamily, ThemeManager.DEFAULT_FONT_SIZE));
                }
                setText(applicationIcon.code() + " " + applicationIcon.displayedName()); // bootstrap icons do nto support lower cars font
            } else {
                clearCell();
            }
        }

        private void clearCell() {
            setGraphic(null);
            setText(null);
        }
    }

    static class IkonButtonCell extends ListCell<AutoCompleteComboBox.HideableItem<ApplicationIcon>> {
        @Override
        protected void updateItem(final AutoCompleteComboBox.HideableItem<ApplicationIcon> item, final boolean isEmpty) {
            super.updateItem(item, isEmpty);
            if (!isEmpty && item != null && item.getObject() != null) {
                final ApplicationIcon icon = item.getObject();
                setGraphic(new FontIcon(icon.ikon()));
                setText(icon.displayedName());
            } else {
                clearCell();
            }
        }

        private void clearCell() {
            setGraphic(null);
            setText(null);
        }
    }
}
