package com.patres.alina.uidesktop.plugin.settings;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.plugin.PluginDetail;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.card.CardItem;
import com.patres.alina.uidesktop.ui.card.CardsPane;


public class PluginCard extends CardItem {

    public PluginCard(final CardListItem cardListItem,
                      final CardsPane cardsPane,
                      final ApplicationWindow applicationWindow) {
        super(cardListItem, cardsPane, applicationWindow);
    }

    @Override
    protected void updateState(UpdateStateRequest updateStateRequest) {
        BackendApi.updatePluginState(updateStateRequest);
    }

    @Override
    protected void deleteCard() {
        BackendApi.deletePlugin(cardListItem.id());
    }

    @Override
    protected String getEditI18nKey() {
        return "plugin.edit";
    }

    @Override
    protected String getDeleteI18nKey() {
        return "plugin.delete";
    }

    @Override
    protected ApplicationModalPaneContent createEditPane() {
        final PluginDetail pluginDetails = BackendApi.getPluginDetails(cardListItem.id());
        return new PluginEditPane(applicationWindow::openPlugins, pluginDetails);
    }


}