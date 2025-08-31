package com.patres.alina.uidesktop.command.settings;

import atlantafx.base.controls.ToggleSwitch;
import com.patres.alina.common.command.CommandDetail;
import com.patres.alina.common.card.State;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.common.event.CommandUpdateEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.ui.atlantafx.CustomTile;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.stream.Stream;

import static com.patres.alina.uidesktop.command.settings.IconComboBox.CONVERTER;
import static com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString;
import static com.patres.alina.uidesktop.util.ui.ResizableNodeUtils.createResizableTextField;

public class CommandEditPane extends CommandSavePane {

    private final CommandDetail commandDetail;

    private TextField commandIdTextField;
    private ToggleSwitch stateToggleSwitch;

    public CommandEditPane(Runnable backFunction, CommandDetail commandDetail) {
        super(backFunction);
        this.commandDetail = commandDetail;
        reload();
    }

    @Override
    protected void saveCommand() {
        editCommand();
    }

    private void editCommand() {
        final CommandDetail commandEditRequest = new CommandDetail(
                commandDetail.id(),
                commandNameTextField.getText(),
                commandDescriptionTextArea.getText(),
                commandSystemPromptTextArea.getText(),
                iconComboBox.getValue().getObject().name(),
                stateToggleSwitch.isSelected() ? State.ENABLED : State.DISABLED
        );
        BackendApi.updateCommandDetail(commandEditRequest);
        backFunction.run();

        DefaultEventBus.getInstance().publish(new CommandUpdateEvent(CommandUpdateEvent.EventType.COMMAND_UPDATED));
    }

    @Override
    protected List<Node> generateContent() {
        var commandIdTile = new CustomTile(
                getLanguageString("command.id.title"),
                null
        );
        commandIdTextField = createResizableTextField(settingsBox);
        commandIdTile.setAction(commandIdTextField);
        commandIdTextField.setEditable(false);

        var stateTile = new CustomTile(
                getLanguageString("command.state.title"),
                null
        );
        stateToggleSwitch = new ToggleSwitch();
        stateTile.setAction(stateToggleSwitch);
        stateTile.setActionHandler(stateToggleSwitch::fire);


        final List<Node> commandContent = super.generateContent();
        final List<Node> editCommandContent = List.of(commandIdTile, stateTile, new Separator());
        return Stream.concat(editCommandContent.stream(), commandContent.stream()).toList();
    }

    @Override
    public void reload() {
        commandIdTextField.setText(commandDetail.id());
        stateToggleSwitch.setSelected(commandDetail.state() == State.ENABLED);
        commandNameTextField.setText(commandDetail.name());
        commandDescriptionTextArea.setText(commandDetail.description());
        commandSystemPromptTextArea.setText(commandDetail.systemPrompt());
        iconComboBox.getSelectionModel().select(CONVERTER.fromString(commandDetail.icon()));
    }

}
