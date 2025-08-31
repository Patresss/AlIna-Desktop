package com.patres.alina.uidesktop.integration.settings;

import com.patres.alina.common.field.FormField;
import com.patres.alina.common.integration.IntegrationToSave;
import com.patres.alina.common.integration.exception.IntegrationFieldIsMandatoryException;
import com.patres.alina.uidesktop.integration.settings.exception.CannotSaveIntegrationDueToAuthLink;
import com.patres.alina.uidesktop.command.settings.ApplicationIcon;
import com.patres.alina.uidesktop.command.settings.AutoCompleteComboBox;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.ui.atlantafx.CustomTile;
import com.patres.alina.uidesktop.ui.util.NodeWithValue;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.kordamp.ikonli.feather.Feather;

import java.awt.*;
import java.net.URI;
import java.util.*;
import java.util.List;

import static com.patres.alina.uidesktop.settings.SettingsMangers.SERVER_SETTINGS;
import static com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString;
import static com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageStringOrDefault;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableTextArea;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableTextField;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

public abstract class IntegrationSavePane<T extends IntegrationToSave> extends ApplicationModalPaneContent {

    public static final String INTEGRATION_I18N_PREFIX = "integration.";
    protected TextField nameTextField;
    protected TextField typeTextField;
    protected TextArea descriptionTextArea;
    protected TextArea defaultDescriptionTextArea;
    protected ComboBox<AutoCompleteComboBox.HideableItem<ApplicationIcon>> iconComboBox;
    protected T integrationToSave;

    protected final List<CustomTile> settingTiles;
    protected final Map<String, Object> settings;

    public IntegrationSavePane(Runnable backFunction, T integrationToSave) {
        super(backFunction);
        this.integrationToSave = integrationToSave;
        this.settingTiles = integrationToSave.form().formFields().stream()
                .map(this::createTile)
                .toList();
        this.settings = integrationToSave.form().formFields().stream()
                .collect(HashMap::new, (m, v) -> m.put(v.id(), v.value()), HashMap::putAll); // To avoid NPE when value is null

        initSettingsForm();
    }

    @FXML
    public void initialize() {
        super.initialize();
        final Button saveButton = createButton(Feather.SAVE, e -> saveIntegration());
        buttonBar.getButtons().add(saveButton);
    }


    @Override
    public void reload() {
        typeTextField.setText(integrationToSave.integrationType());
        nameTextField.setText(integrationToSave.name());
        descriptionTextArea.setText(integrationToSave.description());
        defaultDescriptionTextArea.setText(integrationToSave.defaultDescription());
        settingTiles.forEach(this::setSettingToTile);
    }

    @Override
    protected List<Node> generateContent() {
        var typeTile = new CustomTile(
                getLanguageString("integration.id.type"),
                null
        );
        typeTextField = createResizableTextField(settingsBox);
        typeTile.setAction(typeTextField);
        typeTextField.setEditable(false);

        var nameTile = new CustomTile(
                getLanguageString("integration.name.title"),
                null
        );
        nameTextField = createResizableTextField(settingsBox);
        nameTile.setAction(nameTextField);

        var descriptionTile = new CustomTile(
                getLanguageString("integration.description.title"),
                getLanguageString("integration.description.description")
        );
        descriptionTextArea = createResizableTextArea(settingsBox);
        descriptionTile.setAction(descriptionTextArea);

        var defaultDescriptionTile = new CustomTile(
                getLanguageString("integration.default-description.title"),
                null
        );
        defaultDescriptionTextArea = createResizableTextArea(settingsBox);
        defaultDescriptionTile.setAction(defaultDescriptionTextArea);
        defaultDescriptionTextArea.setEditable(false);

        return List.of(typeTile, nameTile, descriptionTile, defaultDescriptionTile, new Separator());
    }

    protected abstract void saveIntegration();

    protected void openWebpage(String urlString) {
        try {
            URI uri = new URI(urlString);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(uri);
            } else {
                throw new CannotSaveIntegrationDueToAuthLink(urlString, "Desktop is not supported");
            }
        } catch (Exception e) {
            throw new CannotSaveIntegrationDueToAuthLink(urlString, e);
        }
    }

    private void initSettingsForm() {
        final List<Node> nodes = new ArrayList<>(settingTiles);
        nodes.add(new Separator());
        settingsBox.getChildren().addAll(nodes);
    }

    private CustomTile createTile(FormField formField) {
        final NodeWithValue formNode = createNodeByFormField(formField);
        var tile = new CustomTile(
                formField.id(),
                getLanguageStringOrDefault(formField.defaultEnglishName(), INTEGRATION_I18N_PREFIX + formField.nameCode()),
                getLanguageStringOrDefault(formField.defaultEnglishDescription(), INTEGRATION_I18N_PREFIX + formField.descriptionCode()),
                formNode.valueSupplier(),
                formNode.valueConsumer(),
                formField.mandatory()
        );
        tile.setAction(formNode.node());
        return tile;
    }

    private NodeWithValue createNodeByFormField(FormField formField) {
        return switch (formField.formFieldType()) {
            case TEXT_FIELD -> {
                TextField node = createResizableTextField(settingsBox);
                yield new NodeWithValue(node, node::getText, (value) -> node.setText(value == null ? "" : value));
            }
            case TEXT_AREA -> {
                TextArea node = createResizableTextArea(settingsBox);
                yield new NodeWithValue(node, node::getText, (value) -> node.setText(value == null ? "" : value));
            }
            case SERVER_TEXT_FIELD -> {
                TextField node = createResizableTextField(settingsBox);
                node.setDisable(true);
                final String serverUrl;
                if (formField.value() == null) {
                    serverUrl = SERVER_SETTINGS.getSettings().serverAddress() + Optional.ofNullable(formField.initValue()).map(Object::toString).orElse("");
                } else {
                    serverUrl = formField.value().toString();
                }
                yield new NodeWithValue(node, node::getText, (value) -> node.setText(serverUrl));
            }
        };
    }

    private void setSettingToTile(final CustomTile tile) {
        final String value = Optional.ofNullable(settings.get(tile.getFormId()))
                .map(Object::toString)
                .orElse(null);
        tile.setFormValue(value);
    }

    protected Map<String, Object> getIntegrationSettings() {
        settingTiles.stream()
                .filter(setting -> setting.isMandatory() && isBlank(setting.getFormValue()))
                .findFirst()
                .ifPresent(tile -> {
                    throw new IntegrationFieldIsMandatoryException(tile.getFormId());
                });
        return settingTiles.stream()
                .collect(toMap(
                        CustomTile::getFormId,
                        CustomTile::getFormValue
                ));
    }
}
