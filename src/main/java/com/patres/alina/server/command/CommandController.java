package com.patres.alina.server.command;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.command.CommandCreateRequest;
import com.patres.alina.common.command.CommandDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/commands")
public class CommandController {

    private final CommandFileService commandFileService;

    public CommandController(CommandFileService commandFileService) {
        this.commandFileService = commandFileService;
    }

    @GetMapping
    public List<CardListItem> getCommandListItems() {
        return commandFileService.getCommandListItems();
    }

    @GetMapping("/details/{commandId}")
    public CommandDetail getCommandDetails(@PathVariable final String commandId) {
        return commandFileService.getCommandDetails(commandId).orElse(null);
    }

    @PostMapping
    public String createCommandDetail(@RequestBody final CommandCreateRequest commandCreateRequest) {
        return commandFileService.createCommandDetail(commandCreateRequest);
    }

    @PutMapping
    public String updateCommandDetail(@RequestBody final CommandDetail commandDetail) {
        return commandFileService.updateCommandDetail(commandDetail);
    }

    @DeleteMapping("/{commandId}")
    public void deleteCommand(@PathVariable final String commandId) {
        commandFileService.deleteCommand(commandId);
    }

    @PatchMapping("/state")
    public void updateCommandState(@RequestBody UpdateStateRequest updateStateRequest) {
        commandFileService.updateCommandState(updateStateRequest);
    }

}
