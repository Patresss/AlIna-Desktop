package com.patres.alina.uidesktop.settings.ui;

import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import com.patres.alina.uidesktop.settings.ShortcutKeysSettings;
import com.patres.alina.uidesktop.settings.UiSettings;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeyPane;
import com.patres.alina.uidesktop.shortcuts.key.ShortcutKeys;
import com.patres.alina.uidesktop.ui.language.ApplicationLanguage;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.theme.SamplerTheme;
import com.patres.alina.uidesktop.ui.theme.ThemeManager;
import com.patres.alina.uidesktop.ui.util.NotificationSound;
import com.patres.alina.uidesktop.ui.util.NotificationSoundPlayer;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTile;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableRegion;

public class UiSettingsPane extends SettingsModalPaneContent {

    private static final ThemeManager TM = ThemeManager.getInstance();

    private ChoiceBox<SamplerTheme> themeSelector;
    private ChoiceBox<ApplicationLanguage> applicationLanguageSelector;

    private ShortcutKeyPane focusShortcutKeyPane;
    private ShortcutKeyPane contextMenuShortcutKeyPane;
    private ToggleSwitch soundNotificationToggle;
    private ChoiceBox<NotificationSound> soundTypeSelector;

    private ToggleSwitch expandButtonToggle;
    private Spinner<Integer> expandWidthSpinner;
    private ToggleSwitch autoSplitToggle;

    private UiSettings uiSettings;

    public UiSettingsPane(Runnable backFunction) {
        super(backFunction);
    }

    @Override
    protected void reset() {
        loadDataFromSettings();
        themeSelector.setValue(TM.fromString(uiSettings.theme()));
        applicationLanguageSelector.setValue(ApplicationLanguage.getApplicationLanguage(uiSettings.language()));
        TM.setTheme(uiSettings.theme());
        focusShortcutKeyPane.setValues(uiSettings.shortcutKeysSettings().focusShortcutKeys());
        contextMenuShortcutKeyPane.setValues(uiSettings.shortcutKeysSettings().contextMenuShortcutKeys());
        soundNotificationToggle.setSelected(uiSettings.isSoundNotificationEnabled());
        soundTypeSelector.setValue(uiSettings.resolveNotificationSound());
        LanguageManager.setLanguage(uiSettings.language());
        expandButtonToggle.setSelected(uiSettings.isShowExpandButton());
        expandWidthSpinner.getValueFactory().setValue(uiSettings.resolveExpandWidth());
        autoSplitToggle.setSelected(uiSettings.isAutoSplitOnExpand());
    }

    @Override
    protected void save() {
        final String theme = Optional.ofNullable(themeSelector)
                .map(ChoiceBox::getValue)
                .map(SamplerTheme::getName)
                .orElse(null);
        final String language = Optional.ofNullable(applicationLanguageSelector)
                .map(ChoiceBox::getValue)
                .map(ApplicationLanguage::getLocale)
                .map(Locale::getLanguage)
                .orElse(null);

        final ShortcutKeys focusShortcutKeys = focusShortcutKeyPane.getShortcutKeys();
        final ShortcutKeys contextMenuShortcutKeys = contextMenuShortcutKeyPane.getShortcutKeys();
        final ShortcutKeysSettings shortcutKeysSettings = new ShortcutKeysSettings(focusShortcutKeys, contextMenuShortcutKeys);
        final boolean soundEnabled = soundNotificationToggle.isSelected();
        final NotificationSound selectedSound = soundTypeSelector.getValue();
        final String soundType = selectedSound != null ? selectedSound.name() : null;
        final boolean showExpandButton = expandButtonToggle.isSelected();
        final int expandWidth = expandWidthSpinner.getValue();
        final boolean autoSplitOnExpand = autoSplitToggle.isSelected();

        UI_SETTINGS.saveDocument(new UiSettings(theme, language, shortcutKeysSettings, soundEnabled, soundType,
                showExpandButton, expandWidth, autoSplitOnExpand));
    }

    private void loadDataFromSettings() {
        uiSettings = UI_SETTINGS.getSettings();
    }

    @Override
    protected List<Node> generateContent() {
        loadDataFromSettings();
        themeSelector = createResizableRegion(this::createThemeSelector, settingsBox);
        var header = createTextSeparator("settings.ui.title", Styles.TITLE_3);
        var theme = createTile(
                "settings.theme.title",
                "settings.theme.description"
        );
        theme.setAction(themeSelector);


        applicationLanguageSelector = createResizableRegion(this::createApplicationLanguageSelector, settingsBox);
        var language = createTile(
                "settings.language.title",
                "settings.language.description"
        );
        applicationLanguageSelector.getSelectionModel().selectLast();
        language.setAction(applicationLanguageSelector);

        focusShortcutKeyPane = new ShortcutKeyPane();
        var focusShortcut = createTile(
                "settings.shortcut.focus.title",
                "settings.shortcut.focus.description"
        );
        focusShortcut.setAction(focusShortcutKeyPane.createPane());

        contextMenuShortcutKeyPane = new ShortcutKeyPane();
        var contextMenuShortcut = createTile(
                "settings.shortcut.contextmenu.title",
                "settings.shortcut.contextmenu.description"
        );
        contextMenuShortcut.setAction(contextMenuShortcutKeyPane.createPane());

        soundNotificationToggle = new ToggleSwitch();
        soundNotificationToggle.setSelected(uiSettings.isSoundNotificationEnabled());
        var soundNotification = createTile(
                "settings.sound.notification.title",
                "settings.sound.notification.description"
        );
        soundNotification.setAction(soundNotificationToggle);

        soundTypeSelector = createResizableRegion(this::createSoundTypeSelector, settingsBox);
        var soundTypeTile = createTile(
                "settings.sound.type.title",
                "settings.sound.type.description"
        );
        var previewButton = new Button();
        previewButton.setGraphic(new FontIcon("fth-play"));
        previewButton.getStyleClass().addAll(Styles.BUTTON_ICON, Styles.FLAT);
        previewButton.setOnAction(_ -> {
            NotificationSound selected = soundTypeSelector.getValue();
            if (selected != null) {
                NotificationSoundPlayer.playPreview(selected);
            }
        });
        var soundActionBox = new HBox(6, soundTypeSelector, previewButton);
        soundActionBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        soundTypeTile.setAction(soundActionBox);

        // --- Expand button settings ---
        expandButtonToggle = new ToggleSwitch();
        expandButtonToggle.setSelected(uiSettings.isShowExpandButton());
        var expandButtonTile = createTile(
                "settings.expandButton.show.title",
                "settings.expandButton.show.description"
        );
        expandButtonTile.setAction(expandButtonToggle);

        expandWidthSpinner = new Spinner<>();
        expandWidthSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 3000, uiSettings.resolveExpandWidth(), 50));
        expandWidthSpinner.setEditable(true);
        expandWidthSpinner.setPrefWidth(110);
        var expandWidthTile = createTile(
                "settings.expandButton.width.title",
                "settings.expandButton.width.description"
        );
        expandWidthTile.setAction(expandWidthSpinner);

        autoSplitToggle = new ToggleSwitch();
        autoSplitToggle.setSelected(uiSettings.isAutoSplitOnExpand());
        var autoSplitTile = createTile(
                "settings.expandButton.autoSplit.title",
                "settings.expandButton.autoSplit.description"
        );
        autoSplitTile.setAction(autoSplitToggle);

        return List.of(
                header, theme, language,
                new Separator(),
                focusShortcut, contextMenuShortcut,
                new Separator(),
                soundNotification, soundTypeTile,
                new Separator(),
                expandButtonTile, expandWidthTile, autoSplitTile
        );
    }


    private ChoiceBox<SamplerTheme> createThemeSelector() {
        var choiceBox = new ChoiceBox<SamplerTheme>();

        var themes = TM.getRepository().getAll();
        choiceBox.getItems().setAll(themes);


        // must be after setting the initial value
        choiceBox.getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
            if (val != null && getScene() != null) {
                TM.setTheme(val);
            }
        });
        choiceBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SamplerTheme theme) {
                return theme != null ? theme.getName() : "";
            }

            @Override
            public SamplerTheme fromString(String themeName) {
                return TM.fromString(themeName);
            }
        });

        return choiceBox;
    }

    private ChoiceBox<ApplicationLanguage> createApplicationLanguageSelector() {
        var choiceBox = new ChoiceBox<ApplicationLanguage>();
        choiceBox.getItems().setAll(ApplicationLanguage.values());

        choiceBox.getSelectionModel().selectedItemProperty().addListener((obs, old, newValue) -> {
            if (newValue != null && getScene() != null) {
                LanguageManager.setLanguage(newValue);
            }
        });


        return choiceBox;
    }

    private ChoiceBox<NotificationSound> createSoundTypeSelector() {
        var choiceBox = new ChoiceBox<NotificationSound>();
        choiceBox.getItems().setAll(NotificationSound.values());
        choiceBox.setValue(uiSettings.resolveNotificationSound());
        return choiceBox;
    }

}
