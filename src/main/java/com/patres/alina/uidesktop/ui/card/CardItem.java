package com.patres.alina.uidesktop.ui.card;

import atlantafx.base.controls.Tile;
import atlantafx.base.controls.ToggleSwitch;
import atlantafx.base.theme.Styles;
import atlantafx.base.theme.Tweaks;
import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.card.UpdateStateRequest;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.common.event.CommandUpdateEvent;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.atlantafx.CustomTile;
import javafx.geometry.Pos;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.patres.alina.common.card.State.ENABLED;
import static com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageString;
import static com.patres.alina.uidesktop.util.StringUtils.truncateStringToDisplay;

public abstract class CardItem extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(CardItem.class);

    private static final int MAX_NUMBER_OF_CHARACTERS = 128;

    protected final CardListItem cardListItem;
    protected final ApplicationWindow applicationWindow;
    private final CardsPane cardsPane;

    public CardItem(final CardListItem cardListItem,
                    final CardsPane cardsPane,
                    final ApplicationWindow applicationWindow) {
        this.cardListItem = cardListItem;
        this.cardsPane = cardsPane;
        this.applicationWindow = applicationWindow;
        initializeCard();
    }

    protected abstract void updateState(UpdateStateRequest updateStateRequest);

    protected abstract String getEditI18nKey();

    protected abstract String getDeleteI18nKey();

    protected abstract void deleteCard();

    protected abstract ApplicationModalPaneContent createEditPane();

    private void initializeCard() {
        final Tile tile = createTile();
        getChildren().addAll(tile, new Separator());
        setCardStyle();
    }

    private Tile createTile() {
        final FontIcon fontIcon = Optional.ofNullable(cardListItem.icon())
                .map(CardItem::getFontIcon)
                .orElse(null);

        final Tile tile = new CustomTile(
                truncateStringToDisplay(cardListItem.name(), MAX_NUMBER_OF_CHARACTERS),
                truncateStringToDisplay(cardListItem.description(), MAX_NUMBER_OF_CHARACTERS),
                fontIcon);
        tile.setAction(createActionNode());
        return tile;
    }

    private static FontIcon getFontIcon(String iconCode) {
        try {
            return new FontIcon(iconCode);
        } catch (Exception e) {
            logger.error("Cannot find icon {}", iconCode);
            return null;
        }
    }

    private HBox createActionNode() {
        final HBox hBox = new HBox();
        final ToggleSwitch stateToggleSwitch = new ToggleSwitch();
        stateToggleSwitch.setSelected(cardListItem.state() == ENABLED);
        stateToggleSwitch.selectedProperty().addListener(((observable, oldValue, newValue) -> {
            final UpdateStateRequest updateStateRequest = new UpdateStateRequest(cardListItem.id(), newValue);
            updateState(updateStateRequest);
        }));
        hBox.getChildren().addAll(
                stateToggleSwitch,
                createSettingsButton()
        );
        hBox.setAlignment(Pos.CENTER);
        return hBox;
    }

    private MenuButton createSettingsButton() {
        final MenuButton settingsButton = new MenuButton(null, new FontIcon(Feather.MORE_VERTICAL));
        settingsButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT, Tweaks.NO_ARROW);
        settingsButton.getItems().addAll(createEditMenuItem(), createDeleteMenuItem());
        return settingsButton;
    }

    private MenuItem createEditMenuItem() {
        final MenuItem menuItem = new MenuItem(getLanguageString(getEditI18nKey()), new FontIcon(Feather.EDIT));
        menuItem.setOnAction(e -> editPlugin());
        return menuItem;
    }

    private void editPlugin() {
        applicationWindow.getAppModalPane().show(createEditPane());
    }

    private MenuItem createDeleteMenuItem() {
        final MenuItem delete = new MenuItem(getLanguageString(getDeleteI18nKey()), new FontIcon(Feather.TRASH));
        delete.setOnAction(e -> deletePlugin());
        return delete;
    }

    private void deletePlugin() {
        cardsPane.deletePluginCard(this);
        deleteCard();
        DefaultEventBus.getInstance().publish(new CommandUpdateEvent(CommandUpdateEvent.EventType.COMMAND_DELETED));
    }

    private void setCardStyle() {
        getStyleClass().add(Styles.INTERACTIVE);
        setPrefWidth(Integer.MAX_VALUE);
        setMaxWidth(Integer.MAX_VALUE);
    }

}