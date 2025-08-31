package com.patres.alina.uidesktop.command.settings;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.server.command.Command;

public class CardListItemMapper {

    public static CardListItem toCardListItem(final Command command) {
        return new CardListItem(
                command.id(),
                command.name(),
                command.description(),
                command.icon(),
                command.state()
        );
    }

}
