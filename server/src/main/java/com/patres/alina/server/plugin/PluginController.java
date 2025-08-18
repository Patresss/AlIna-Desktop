package com.patres.alina.server.plugin;

import com.patres.alina.common.plugin.PluginCreateRequest;
import com.patres.alina.common.plugin.PluginDetail;
import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/plugins")
public class PluginController {

    private final PluginService pluginService;

    public PluginController(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    @GetMapping
    public List<CardListItem> getPluginListItems() {
        return pluginService.getPluginListItems();
    }

    @GetMapping("/details/{pluginId}")
    public PluginDetail getPluginDetails(@PathVariable final String pluginId) {
        return pluginService.getPluginDetails(pluginId).orElse(null);
    }

    @PostMapping
    public String createPluginDetail(@RequestBody final PluginCreateRequest pluginCreateRequest) {
        return pluginService.createPluginDetail(pluginCreateRequest);
    }

    @PutMapping
    public String updatePluginDetail(@RequestBody final PluginDetail pluginDetail) {
        return pluginService.updatePluginDetail(pluginDetail);
    }

    @DeleteMapping("/{pluginId}")
    public void deleteChatThread(@PathVariable final String pluginId) {
        pluginService.deletePlugin(pluginId);
    }

    @PatchMapping("/state")
    public void updatePluginState(@RequestBody UpdateStateRequest updateStateRequest) {
        pluginService.updatePluginState(updateStateRequest);
    }

}
