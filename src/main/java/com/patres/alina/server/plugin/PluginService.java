package com.patres.alina.server.plugin;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.plugin.PluginCreateRequest;
import com.patres.alina.common.plugin.PluginDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static com.patres.alina.server.plugin.PluginMapper.toPlugin;
import static com.patres.alina.server.plugin.PluginMapper.toPluginListItems;

@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "mongodb", matchIfMissing = true)
public class PluginService implements PluginServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(PluginService.class);

    private final PluginRepository pluginRepository;

    public PluginService(final PluginRepository pluginRepository
    ) {
        this.pluginRepository = pluginRepository;
    }

    @Override
    public List<CardListItem> getPluginListItems() {
        final List<Plugin> name = pluginRepository.findAll(Sort.by(Sort.Order.desc("state"), Sort.Order.asc("name")));
        return toPluginListItems(name);
    }

    @Override
    public Optional<PluginDetail> getPluginDetails(String pluginId) {
        return pluginRepository.findById(pluginId)
                .map(PluginMapper::toPluginDetails);
    }

    @Override
    public String createPluginDetail(final PluginCreateRequest pluginCreateRequest) {
        logger.info("Creating a new plugin: {}", pluginCreateRequest);
        final Plugin plugin = toPlugin(pluginCreateRequest);
        final Plugin createdPlugin = pluginRepository.save(plugin);
        logger.info("Created the plugin with id=`{}`", createdPlugin.id());
        return createdPlugin.id();
    }

    @Override
    public String updatePluginDetail(final PluginDetail pluginDetail) {
        final Plugin plugin = toPlugin(pluginDetail);
        final Plugin updatedPlugin = pluginRepository.save(plugin);
        return updatedPlugin.id();
    }

    @Override
    public void deletePlugin(final String pluginId) {
        logger.info("Deleting plugin: '{}'", pluginId);
        pluginRepository.deleteById(pluginId);
    }

    @Override
    public void updatePluginState(final UpdateStateRequest updateStateRequest) {
        logger.info("Setting plugin {} state to {}...", updateStateRequest.id(), updateStateRequest.state());
        pluginRepository.updateStateById(updateStateRequest.id(), updateStateRequest.state());
    }

}
