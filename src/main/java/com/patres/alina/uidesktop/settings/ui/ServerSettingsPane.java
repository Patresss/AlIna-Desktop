package com.patres.alina.uidesktop.settings.ui;

import atlantafx.base.theme.Styles;
import com.patres.alina.uidesktop.settings.ServerSettings;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;

import java.util.List;
import java.util.Optional;

import static com.patres.alina.uidesktop.settings.SettingsMangers.SERVER_SETTINGS;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;
import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTile;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableRegion;

public class ServerSettingsPane extends SettingsModalPaneContent {

    private TextField serverAddressTextField;
    private TextField serverPasswordTextField;

    private ServerSettings settings;

    public ServerSettingsPane(Runnable backFunction) {
        super(backFunction);
    }



    @Override
    protected void reset() {
        loadDataFromSettings();
        serverAddressTextField.setText(settings.serverAddress());
        serverPasswordTextField.setText(settings.serverPassword());
        // TODO add info that you need to restart the app to load server settings
    }

    @Override
    protected void save() {
        final String address = Optional.ofNullable(serverAddressTextField)
                .map(TextInputControl::getText)
                .orElse(null);
        final String password = Optional.ofNullable(serverPasswordTextField)
                .map(TextInputControl::getText)
                .orElse(null);
      SERVER_SETTINGS.saveDocument(new ServerSettings(address, password));
    }

    private void loadDataFromSettings() {
        settings = SERVER_SETTINGS.getSettings();
    }

    @Override
    protected List<Node> generateContent() {
        loadDataFromSettings();
        var header = createTextSeparator("settings.server.title", Styles.TITLE_3);

        var serverAddress = createTile(
                "settings.server.address.title",
                "settings.server.address.description"
        );

        serverAddressTextField = createResizableRegion(() -> new TextField(settings.serverAddress()), settingsBox);;
        serverAddress.setAction(serverAddressTextField);

        var serverPassword = createTile(
                "settings.server.password.title",
                "settings.server.password.description"
        );

        serverPasswordTextField = createResizableRegion(() -> new TextField(settings.serverPassword()), settingsBox);
        serverPassword.setAction(serverPasswordTextField);

        return List.of(header, serverAddress, serverPassword);
    }


}
