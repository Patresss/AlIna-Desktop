package com.patres.alina.uidesktop.chat.thread.ui;

import atlantafx.base.theme.Styles;
import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.settings.ui.ApplicationModalPaneContent;
import com.patres.alina.uidesktop.ui.ApplicationWindow;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static com.patres.alina.uidesktop.ui.util.TranslatedComponentUtils.createTextSeparator;

public class ChatThreadHistoryPane extends ApplicationModalPaneContent {

    private final ApplicationWindow applicationWindow;

    private VBox cardsHolder;


    public ChatThreadHistoryPane(Runnable backFunction, ApplicationWindow applicationWindow) {
        super(backFunction);
        this.applicationWindow = applicationWindow;
    }

    @Override
    public void reload() {
        final List<ChatThread> chatThreads = BackendApi.getChatThreads();
        final List<ChatThreadHistoryCard> cards = chatThreads.stream()
                .map(chatThread -> new ChatThreadHistoryCard(chatThread, this))
                .toList();

        cardsHolder.getChildren().clear();
        cardsHolder.getChildren().addAll(cards);
    }

    @Override
    protected List<Node> generateContent() {
        final var header = createTextSeparator("chat.thread.history.title", Styles.TITLE_3);
        cardsHolder = createCardHolder();

        final List<Node> nodes = new ArrayList<>();
        nodes.add(header);
        nodes.add(cardsHolder);
        return nodes;
    }

    private static VBox createCardHolder() {
        final VBox cardsHolder = new VBox();
        cardsHolder.getStyleClass().add("card-holder");
        return cardsHolder;
    }

    public void selectChatThread(final ChatThread chatThread) {
        applicationWindow.openChatThread(chatThread);
    }

    public void removeChatThreadCard(final ChatThreadHistoryCard chatThread) {
        cardsHolder.getChildren().remove(chatThread);
        applicationWindow.getChatThread()
                .filter(currentChatThread -> currentChatThread.equals(chatThread.getChatThread()))
                .ifPresent(currentChatThread -> applicationWindow.createNewChatThread());
    }

}
