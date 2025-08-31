package com.patres.alina.server.plugin;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.plugin.PluginCreateRequest;
import com.patres.alina.common.plugin.PluginDetail;

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

    public static Command toCommand(final PluginCreateRequest pluginCreateRequest) {
        return new Command(
                pluginCreateRequest.name(),
                pluginCreateRequest.description(),
                pluginCreateRequest.systemPrompt(),
                pluginCreateRequest.icon()
        );
    }

    public static Command toCommand(final PluginDetail pluginDetail) {
        return new Command(
                pluginDetail.id(),
                pluginDetail.name(),
                pluginDetail.description(),
                pluginDetail.systemPrompt(),
                pluginDetail.icon(),
                pluginDetail.state()
        );
    }

    public static List<CardListItem> toCardListItems(final List<Command> commands) {
        return commands.stream()
                .map(CommandMapper::toCardListItem)
                .toList();
    }

    public static PluginDetail toPluginDetail(final Command command) {
        return new PluginDetail(
                command.id(),
                command.name(),
                command.description(),
                command.systemPrompt(),
                command.icon(),
                command.state()
        );
    }
}