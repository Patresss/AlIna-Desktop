package com.patres.alina.server.plugin;

import com.patres.alina.common.plugin.PluginCreateRequest;
import com.patres.alina.common.plugin.PluginDetail;
import com.patres.alina.common.card.CardListItem;

import java.util.List;

public final class PluginMapper {

    public static CardListItem toPluginListItem(final Plugin plugin) {
        return new CardListItem(
                plugin.id(),
                plugin.name(),
                plugin.description(),
                plugin.icon(),
                plugin.state()
        );
    }

    public static Plugin toPlugin(final PluginCreateRequest pluginCreateRequest) {
        return new Plugin(
                pluginCreateRequest.name(),
                pluginCreateRequest.description(),
                pluginCreateRequest.systemPrompt(),
                pluginCreateRequest.icon()
        );
    }

    public static Plugin toPlugin(final PluginDetail pluginDetail) {
        return new Plugin(
                pluginDetail.id(),
                pluginDetail.name(),
                pluginDetail.description(),
                pluginDetail.systemPrompt(),
                pluginDetail.icon(),
                pluginDetail.state()
        );
    }

    public static List<CardListItem> toPluginListItems(final List<Plugin> plugins) {
        return plugins.stream()
                .map(PluginMapper::toPluginListItem)
                .toList();
    }

    public static PluginDetail toPluginDetails(final Plugin plugin) {
        return new PluginDetail(
                plugin.id(),
                plugin.name(),
                plugin.description(),
                plugin.systemPrompt(),
                plugin.icon(),
                plugin.state()
        );
    }

}
