package com.patres.alina.server.command;

import com.patres.alina.common.card.UpdateStateRequest;
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
    public List<Command> getEnabledCommands() {
        return commandFileService.getCommands();
    }

    @GetMapping("/all")
    public List<Command> getAllCommands() {
        return commandFileService.getAllCommands();
    }

    @GetMapping("/{commandId}")
    public Command getCommand(@PathVariable final String commandId) {
        return commandFileService.findById(commandId).orElse(null);
    }

    @PostMapping
    public String createCommand(@RequestBody final Command command) {
        return commandFileService.create(command);
    }

    @PutMapping
    public String updateCommand(@RequestBody final Command command) {
        return commandFileService.update(command);
    }

    @DeleteMapping("/{commandId}")
    public void deleteCommand(@PathVariable final String commandId) {
        commandFileService.deleteCommand(commandId);
    }

    @PatchMapping("/state")
    public void updateCommandState(@RequestBody final UpdateStateRequest updateStateRequest) {
        commandFileService.updateCommandState(updateStateRequest);
    }

}
