package com.patres.alina.uidesktop.plugin.settings;

import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.card.CardsPane;
import javafx.scene.control.Button;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;

public class PluginPane extends CardsPane {

    public PluginPane(Runnable backFunction, ApplicationWindow applicationWindow) {
        super(backFunction, applicationWindow);
    }

    @Override
    public Button createButton() {
        final PluginCreatePane createPluginDialog = new PluginCreatePane(() -> applicationWindow.openPlugins()); // be careful - method reference could break the code
        return createButton(Feather.PLUS_CIRCLE, e -> addPlugin(createPluginDialog));
    }

    @Override
    public List<PluginCard> createCards() {
        final var pluginListItems = BackendApi.getPluginListItems();
        return pluginListItems.stream()
                .map(pluginListItem -> new PluginCard(pluginListItem, this, applicationWindow))
                .toList();
    }

    @Override
    public String getHeaderInternalizedTitle() {
        return "plugin.title";
    }

    private void addPlugin(final PluginCreatePane dialog) {
        dialog.reload();
        modalPane.setPersistent(true);
        modalPane.show(dialog);
    }

}
