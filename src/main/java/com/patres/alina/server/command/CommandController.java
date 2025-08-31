package com.patres.alina.server.command;

import com.patres.alina.common.card.UpdateStateRequest;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CommandController {

    private final CommandFileService commandFileService;

    public CommandController(CommandFileService commandFileService) {
        this.commandFileService = commandFileService;
    }

    public List<Command> getEnabledCommands() {
        return commandFileService.getCommands();
    }

    public List<Command> getAllCommands() {
        return commandFileService.getAllCommands();
    }

    public Command getCommand(final String commandId) {
        return commandFileService.findById(commandId).orElse(null);
    }

    public String createCommand(final Command command) {
        return commandFileService.create(command);
    }

    public String updateCommand(final Command command) {
        return commandFileService.update(command);
    }

    public void deleteCommand(final String commandId) {
        commandFileService.deleteCommand(commandId);
    }

    public void updateCommandState(final UpdateStateRequest updateStateRequest) {
        commandFileService.updateCommandState(updateStateRequest);
    }

}
