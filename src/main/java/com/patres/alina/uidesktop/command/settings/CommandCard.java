package com.patres.alina.uidesktop.command.settings;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.command.CommandDetail;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.card.CardItem;
import com.patres.alina.uidesktop.ui.card.CardsPane;


public class CommandCard extends CardItem {

    public CommandCard(final CardListItem cardListItem,
                      final CardsPane cardsPane,
                      final ApplicationWindow applicationWindow) {
        super(cardListItem, cardsPane, applicationWindow);
    }

    @Override
    protected void updateState(UpdateStateRequest updateStateRequest) {
        BackendApi.updateCommandState(updateStateRequest);
    }

    @Override
    protected void deleteCard() {
        BackendApi.deleteCommand(cardListItem.id());
    }

    @Override
    protected String getEditI18nKey() {
        return "command.edit";
    }

    @Override
    protected String getDeleteI18nKey() {
        return "command.delete";
    }

    @Override
    protected ApplicationModalPaneContent createEditPane() {
        final CommandDetail commandDetails = BackendApi.getCommandDetails(cardListItem.id());
        return new CommandEditPane(applicationWindow::openCommands, commandDetails);
    }


}