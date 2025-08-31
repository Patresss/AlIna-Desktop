package com.patres.alina.uidesktop.command.settings;

import com.patres.alina.common.card.CardListItem;
import com.patres.alina.server.command.Command;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.card.CardsPane;
import javafx.scene.control.Button;
import org.kordamp.ikonli.feather.Feather;

import java.util.List;

import static com.patres.alina.uidesktop.command.settings.CardListItemMapper.toCardListItem;

public class CommandPane extends CardsPane {

    public CommandPane(Runnable backFunction, ApplicationWindow applicationWindow) {
        super(backFunction, applicationWindow);
    }

    @Override
    public Button createButton() {
        final CommandCreatePane createCommandDialog = new CommandCreatePane(() -> applicationWindow.openCommands()); // be careful - method reference could break the code
        return createButton(Feather.PLUS_CIRCLE, e -> addCommand(createCommandDialog));
    }

    @Override
    public List<CommandCard> createCards() {
        final var commands = BackendApi.getCommands();
        return commands.stream()
                .map(CardListItemMapper::toCardListItem)
                .map(command -> new CommandCard(command, this, applicationWindow))
                .toList();
    }

    @Override
    public String getHeaderInternalizedTitle() {
        return "command.title";
    }

    private void addCommand(final CommandCreatePane dialog) {
        dialog.reload();
        modalPane.setPersistent(true);
        modalPane.show(dialog);
    }

}
