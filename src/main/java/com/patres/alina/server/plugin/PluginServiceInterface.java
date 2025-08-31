package com.patres.alina.server.plugin;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.plugin.PluginCreateRequest;
import com.patres.alina.common.plugin.PluginDetail;

import java.util.List;
import java.util.Optional;

public interface PluginServiceInterface {
    
    List<CardListItem> getPluginListItems();
    
    Optional<PluginDetail> getPluginDetails(String pluginId);
    
    String createPluginDetail(PluginCreateRequest pluginCreateRequest);
    
    String updatePluginDetail(PluginDetail pluginDetail);
    
    void deletePlugin(String pluginId);
    
    void updatePluginState(UpdateStateRequest updateStateRequest);
}