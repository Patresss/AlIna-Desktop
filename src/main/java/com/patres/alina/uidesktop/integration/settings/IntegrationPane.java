package com.patres.alina.uidesktop.integration.settings;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.card.CardListItem;
import com.patres.alina.common.integration.IntegrationToAdd;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import com.patres.alina.uidesktop.ui.card.CardsPane;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

import static com.patres.alina.uidesktop.ui.language.LanguageManager.getLanguageStringOrDefault;
import static com.patres.alina.uidesktop.util.FontCreator.safetlyCreateFontIcon;

public class IntegrationPane extends CardsPane {

    public IntegrationPane(Runnable backFunction, ApplicationWindow applicationWindow) {
        super(backFunction, applicationWindow);
    }

    @Override
    public ButtonBase createButton() {
        final List<IntegrationToAdd> availableIntegrationToAdd = List.of(); // TODO AlinaRestApi.getAlinaRestClient().getAvailableIntegrationToAdd();
        final List<MenuItem> menuItems = availableIntegrationToAdd.stream()
                .map(this::createMenuItem)
                .toList();
        final MenuButton menuButton = new MenuButton(null, new FontIcon(Feather.PLUS_CIRCLE));
        menuButton.getStyleClass().addAll(Styles.BUTTON_CIRCLE, Styles.FLAT);
        menuButton.getItems().addAll(menuItems);
        return menuButton;
    }

    private MenuItem createMenuItem(final IntegrationToAdd integration) {
        final MenuItem rename = new MenuItem(getLanguageStringOrDefault(integration.defaultName(), "integration.type." + integration.integrationType()), safetlyCreateFontIcon(integration.icon()));
        rename.setOnAction(event -> applicationWindow.getAppModalPane().show(createIntegrationCreatePane(integration)));
        return rename;
    }

    private IntegrationCreatePane createIntegrationCreatePane(final IntegrationToAdd integration) {
        final IntegrationCreatePane integrationCreatePane = new IntegrationCreatePane(() -> applicationWindow.openIntegrations(), integration);
        integrationCreatePane.reload();
        return integrationCreatePane;
    }

    @Override
    public List<IntegrationCard> createCards() {
        final List<CardListItem> integrationListItems = List.of(); // TODO AlinaRestApi.getAlinaRestClient().getIntegrationListItem();
        return integrationListItems.stream()
                .map(integrationListItem -> new IntegrationCard(integrationListItem, this, applicationWindow))
                .toList();
    }

    @Override
    public String getHeaderInternalizedTitle() {
        return "integration.title";
    }

}
