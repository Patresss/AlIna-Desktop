package com.patres.alina.uidesktop.integration.settings;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.integration.IntegrationDetails;
import com.patres.alina.common.plugin.PluginDetail;
import com.patres.alina.uidesktop.plugin.settings.PluginEditPane;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.card.CardItem;
import com.patres.alina.uidesktop.ui.card.CardsPane;

import static com.patres.alina.uidesktop.backend.AlinaRestApi.getAlinaRestClient;

public class IntegrationCard extends CardItem {

    public IntegrationCard(final CardListItem cardListItem,
                           final CardsPane cardsPane,
                           final ApplicationWindow applicationWindow) {
        super(cardListItem, cardsPane, applicationWindow);
    }

    @Override
    protected void updateState(UpdateStateRequest updateStateRequest) {
        getAlinaRestClient().updateIntegrationState(updateStateRequest);
    }

    @Override
    protected void deleteCard() {
        getAlinaRestClient().deleteIntegration(cardListItem.id());
    }

    @Override
    protected String getEditI18nKey() {
        return "integration.edit";
    }

    @Override
    protected String getDeleteI18nKey() {
        return "integration.delete";
    }

    @Override
    protected ApplicationModalPaneContent createEditPane() {
        final IntegrationDetails integration = getAlinaRestClient().getIntegrationById(cardListItem.id());
        return new IntegrationEditPane(applicationWindow::openIntegrations, integration);
    }





}