package com.patres.alina.server.command;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.command.CommandCreateRequest;
import com.patres.alina.common.command.CommandDetail;

import java.util.List;

public final class CommandMapper {

    public static CardListItem toCardListItem(final Command command) {
        return new CardListItem(
                command.id(),
                command.name(),
                command.description(),
                command.icon(),
                command.state()
        );
    }

    public static Command toCommand(final CommandCreateRequest commandCreateRequest) {
        return new Command(
                commandCreateRequest.name(),
                commandCreateRequest.description(),
                commandCreateRequest.systemPrompt(),
                commandCreateRequest.icon()
        );
    }

    public static Command toCommand(final CommandDetail commandDetail) {
        return new Command(
                commandDetail.id(),
                commandDetail.name(),
                commandDetail.description(),
                commandDetail.systemPrompt(),
                commandDetail.icon(),
                commandDetail.state()
        );
    }

    public static List<CardListItem> toCardListItems(final List<Command> commands) {
        return commands.stream()
                .map(CommandMapper::toCardListItem)
                .toList();
    }

    public static CommandDetail toCommandDetail(final Command command) {
        return new CommandDetail(
                command.id(),
                command.name(),
                command.description(),
                command.systemPrompt(),
                command.icon(),
                command.state()
        );
    }
}