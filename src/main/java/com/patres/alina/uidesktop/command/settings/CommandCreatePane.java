package com.patres.alina.uidesktop.command.settings;

import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.common.event.CommandUpdateEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import org.kordamp.ikonli.bootstrapicons.BootstrapIcons;

import static com.patres.alina.uidesktop.command.settings.IconComboBox.CONVERTER;

public class CommandCreatePane extends CommandSavePane {

    public CommandCreatePane(Runnable backFunction) {
        super(backFunction);
    }

    @Override
    public void reload() {
        commandNameTextField.clear();
        commandDescriptionTextArea.clear();
        commandSystemPromptTextArea.clear();
        iconComboBox.getSelectionModel().select(CONVERTER.fromString(BootstrapIcons.PLUG.getDescription()));
    }

    @Override
    protected void saveCommand() {
        createCommand();
    }

    private void createCommand() {
        Command command = new Command(
                commandNameTextField.getText(),
                commandDescriptionTextArea.getText(),
                commandSystemPromptTextArea.getText(),
                iconComboBox.getValue().getObject().name()
        );

        BackendApi.createCommand(command);
        backFunction.run();
        DefaultEventBus.getInstance().publish(new CommandUpdateEvent(CommandUpdateEvent.EventType.COMMAND_ADDED));
    }

}
