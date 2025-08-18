package com.patres.alina.server.plugin;

import com.patres.alina.common.plugin.PluginCreateRequest;
import com.patres.alina.common.plugin.PluginDetail;
import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.patres.alina.server.plugin.PluginMapper.*;

@Service
public class PluginService {

    private static final Logger logger = LoggerFactory.getLogger(PluginService.class);

    private final PluginRepository pluginRepository;

    public PluginService(final PluginRepository pluginRepository) {
        this.pluginRepository = pluginRepository;
    }

    public List<CardListItem> getPluginListItems() {
        final List<Plugin> name = pluginRepository.findAll(Sort.by(Sort.Order.desc("state"), Sort.Order.asc("name")));
        return toPluginListItems(name);
    }

    public Optional<PluginDetail> getPluginDetails(String pluginId) {
        return pluginRepository.findById(pluginId)
                .map(PluginMapper::toPluginDetails);
    }

    public String createPluginDetail(final PluginCreateRequest pluginCreateRequest) {
        logger.info("Creating a new plugin: {}", pluginCreateRequest);
        final Plugin plugin = toPlugin(pluginCreateRequest);
        final Plugin createdPlugin = pluginRepository.save(plugin);
        logger.info("Created the plugin with id=`{}`", createdPlugin.id());
        return createdPlugin.id();
    }

    public String updatePluginDetail(final PluginDetail pluginDetail) {
        final Plugin plugin = toPlugin(pluginDetail);
        final Plugin updatedPlugin = pluginRepository.save(plugin);
        return updatedPlugin.id();
    }

    public void deletePlugin(final String pluginId) {
        logger.info("Deleting plugin: '{}'", pluginId);
        pluginRepository.deleteById(pluginId);
    }

    public void updatePluginState(final UpdateStateRequest updateStateRequest) {
        logger.info("Setting plugin {} state to {}...", updateStateRequest.id(), updateStateRequest.state());
        pluginRepository.updateStateById(updateStateRequest.id(), updateStateRequest.state());
    }

}
