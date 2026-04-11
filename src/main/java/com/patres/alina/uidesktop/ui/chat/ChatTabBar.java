package com.patres.alina.uidesktop.ui.chat;

import com.patres.alina.common.thread.ChatThread;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.util.FxThreadRunner;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * A horizontal tab bar for managing multiple chat sessions.
 * Hidden when only one tab exists; shows scroll arrows when tabs overflow.
 */
public class ChatTabBar extends HBox {

    private static final double SCROLL_STEP = 150;

    private final HBox tabsContainer;
    private final ScrollPane scrollPane;
    private final Button addTabButton;
    private final Button scrollLeftButton;
    private final Button scrollRightButton;

    private final Map<String, ChatTabItem> tabs = new LinkedHashMap<>();
    private String activeTabId;

    private Consumer<String> onTabSelected;
    private Consumer<String> onTabClosed;
    private Runnable onNewTabRequested;

    public ChatTabBar() {
        super();
        getStyleClass().add("chat-tab-bar");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(0);

        // Scroll left arrow
        scrollLeftButton = new Button();
        scrollLeftButton.getStyleClass().addAll("chat-tab-scroll-button", "flat");
        scrollLeftButton.setGraphic(new FontIcon("mdal-chevron_left"));
        scrollLeftButton.setOnAction(_ -> scrollBy(-SCROLL_STEP));
        scrollLeftButton.setVisible(false);
        scrollLeftButton.setManaged(false);

        // Tabs inside a scrollable area
        tabsContainer = new HBox();
        tabsContainer.getStyleClass().add("chat-tab-bar-tabs");
        tabsContainer.setAlignment(Pos.CENTER_LEFT);
        tabsContainer.setSpacing(2);

        scrollPane = new ScrollPane(tabsContainer);
        scrollPane.getStyleClass().add("chat-tab-bar-scroll");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToHeight(true);
        scrollPane.setMaxHeight(32);
        scrollPane.setPrefHeight(32);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        // Scroll right arrow
        scrollRightButton = new Button();
        scrollRightButton.getStyleClass().addAll("chat-tab-scroll-button", "flat");
        scrollRightButton.setGraphic(new FontIcon("mdal-chevron_right"));
        scrollRightButton.setOnAction(_ -> scrollBy(SCROLL_STEP));
        scrollRightButton.setVisible(false);
        scrollRightButton.setManaged(false);

        // "+" button
        addTabButton = new Button();
        addTabButton.getStyleClass().addAll("chat-tab-add-button", "flat", "button-circle");
        addTabButton.setGraphic(new FontIcon("mdal-add"));
        addTabButton.setOnAction(_ -> {
            if (onNewTabRequested != null) {
                onNewTabRequested.run();
            }
        });
        Tooltip.install(addTabButton, new Tooltip(LanguageManager.getLanguageString("tab.new")));

        getChildren().addAll(scrollLeftButton, scrollPane, scrollRightButton, addTabButton);
        setPadding(new Insets(0, 4, 0, 4));

        // Listen for scroll position and layout changes to toggle arrows
        scrollPane.hvalueProperty().addListener((_, _, _) -> updateScrollArrows());
        tabsContainer.widthProperty().addListener((_, _, _) -> updateScrollArrows());
        scrollPane.viewportBoundsProperty().addListener((_, _, _) -> updateScrollArrows());

        // Start hidden (only 1 tab initially)
        setVisible(false);
        setManaged(false);
    }

    public void addTab(ChatThread chatThread, boolean activate) {
        if (tabs.containsKey(chatThread.id())) {
            if (activate) {
                selectTab(chatThread.id());
            }
            return;
        }

        ChatTabItem tabItem = new ChatTabItem(chatThread);
        tabItem.setOnMouseClicked(_ -> selectTab(chatThread.id()));
        tabItem.getCloseButton().setOnAction(_ -> requestCloseTab(chatThread.id()));

        tabs.put(chatThread.id(), tabItem);
        tabsContainer.getChildren().add(tabItem);

        if (activate) {
            selectTab(chatThread.id());
        }

        updateTabCloseButtonVisibility();
        updateBarVisibility();
    }

    public void removeTab(String threadId) {
        ChatTabItem item = tabs.remove(threadId);
        if (item != null) {
            tabsContainer.getChildren().remove(item);
        }
        updateTabCloseButtonVisibility();
        updateBarVisibility();
    }

    public void selectTab(String threadId) {
        if (!tabs.containsKey(threadId)) {
            return;
        }

        String previousActiveId = activeTabId;
        activeTabId = threadId;

        tabs.forEach((id, item) -> {
            if (id.equals(threadId)) {
                item.getStyleClass().removeAll("chat-tab-inactive");
                if (!item.getStyleClass().contains("chat-tab-active")) {
                    item.getStyleClass().add("chat-tab-active");
                }
            } else {
                item.getStyleClass().removeAll("chat-tab-active");
                if (!item.getStyleClass().contains("chat-tab-inactive")) {
                    item.getStyleClass().add("chat-tab-inactive");
                }
            }
        });

        if (onTabSelected != null && !threadId.equals(previousActiveId)) {
            onTabSelected.accept(threadId);
        }

        scrollToTab(threadId);
    }

    public Optional<String> getActiveTabId() {
        return Optional.ofNullable(activeTabId);
    }

    public boolean hasTab(String threadId) {
        return tabs.containsKey(threadId);
    }

    public int getTabCount() {
        return tabs.size();
    }

    public List<String> getTabIds() {
        return new ArrayList<>(tabs.keySet());
    }

    public void updateTabName(String threadId, String name) {
        ChatTabItem item = tabs.get(threadId);
        if (item != null) {
            FxThreadRunner.run(() -> item.setTabName(name));
        }
    }

    public void setOnTabSelected(Consumer<String> onTabSelected) {
        this.onTabSelected = onTabSelected;
    }

    public void setOnTabClosed(Consumer<String> onTabClosed) {
        this.onTabClosed = onTabClosed;
    }

    public void setOnNewTabRequested(Runnable onNewTabRequested) {
        this.onNewTabRequested = onNewTabRequested;
    }

    // ─── Visibility ──────────────────────────────

    /**
     * Hide the entire tab bar when there is only one tab.
     * Show it as soon as there are two or more.
     */
    private void updateBarVisibility() {
        boolean show = tabs.size() > 1;
        setVisible(show);
        setManaged(show);
    }

    // ─── Scroll helpers ──────────────────────────

    private void scrollBy(double pixels) {
        double contentWidth = tabsContainer.getWidth();
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        if (contentWidth <= viewportWidth) {
            return;
        }
        double maxScroll = contentWidth - viewportWidth;
        double currentPixel = scrollPane.getHvalue() * maxScroll;
        double target = Math.max(0, Math.min(maxScroll, currentPixel + pixels));
        scrollPane.setHvalue(target / maxScroll);
    }

    private void scrollToTab(String threadId) {
        ChatTabItem item = tabs.get(threadId);
        if (item == null) {
            return;
        }
        FxThreadRunner.run(() -> {
            double contentWidth = tabsContainer.getWidth();
            double viewportWidth = scrollPane.getViewportBounds().getWidth();
            if (contentWidth <= viewportWidth) {
                return;
            }
            double maxScroll = contentWidth - viewportWidth;
            double itemLeft = item.getBoundsInParent().getMinX();
            double itemRight = item.getBoundsInParent().getMaxX();
            double visibleLeft = scrollPane.getHvalue() * maxScroll;
            double visibleRight = visibleLeft + viewportWidth;

            if (itemLeft < visibleLeft) {
                scrollPane.setHvalue(itemLeft / maxScroll);
            } else if (itemRight > visibleRight) {
                scrollPane.setHvalue((itemRight - viewportWidth) / maxScroll);
            }
        });
    }

    private void updateScrollArrows() {
        double contentWidth = tabsContainer.getWidth();
        double viewportWidth = scrollPane.getViewportBounds().getWidth();
        boolean overflows = contentWidth > viewportWidth + 1; // +1 for rounding

        if (!overflows) {
            scrollLeftButton.setVisible(false);
            scrollLeftButton.setManaged(false);
            scrollRightButton.setVisible(false);
            scrollRightButton.setManaged(false);
            return;
        }

        double hvalue = scrollPane.getHvalue();
        boolean canScrollLeft = hvalue > 0.001;
        boolean canScrollRight = hvalue < 0.999;

        scrollLeftButton.setVisible(canScrollLeft);
        scrollLeftButton.setManaged(canScrollLeft);
        scrollRightButton.setVisible(canScrollRight);
        scrollRightButton.setManaged(canScrollRight);
    }

    // ─── Close / internal ────────────────────────

    private void requestCloseTab(String threadId) {
        if (tabs.size() <= 1) {
            return;
        }
        if (onTabClosed != null) {
            onTabClosed.accept(threadId);
        }
    }

    private void updateTabCloseButtonVisibility() {
        boolean canClose = tabs.size() > 1;
        tabs.values().forEach(item -> item.setClosable(canClose));
    }

    /**
     * A single tab item in the tab bar.
     * Close button is only revealed on hover for a clean look.
     */
    static class ChatTabItem extends HBox {

        private final Label nameLabel;
        private final Button closeButton;
        private boolean closable = true;

        ChatTabItem(ChatThread chatThread) {
            super();
            getStyleClass().addAll("chat-tab-item", "chat-tab-inactive");
            setAlignment(Pos.CENTER);
            setSpacing(0);

            nameLabel = new Label(formatTabName(chatThread.name()));
            nameLabel.getStyleClass().add("chat-tab-name");
            nameLabel.setMaxWidth(110);
            nameLabel.setEllipsisString("\u2026");
            nameLabel.setMinWidth(0);

            closeButton = new Button();
            closeButton.getStyleClass().addAll("chat-tab-close-button");
            closeButton.setGraphic(new FontIcon("mdal-close"));
            Tooltip.install(closeButton, new Tooltip(LanguageManager.getLanguageString("tab.close")));
            closeButton.setOpacity(0);
            closeButton.setMouseTransparent(true);

            getChildren().addAll(nameLabel, closeButton);

            // Show close button on hover
            setOnMouseEntered(_ -> {
                if (closable) {
                    closeButton.setOpacity(1);
                    closeButton.setMouseTransparent(false);
                }
            });
            setOnMouseExited(_ -> {
                closeButton.setOpacity(0);
                closeButton.setMouseTransparent(true);
            });
        }

        void setTabName(String name) {
            nameLabel.setText(formatTabName(name));
        }

        Button getCloseButton() {
            return closeButton;
        }

        void setClosable(boolean closable) {
            this.closable = closable;
            if (!closable) {
                closeButton.setOpacity(0);
                closeButton.setMouseTransparent(true);
            }
        }

        private static String formatTabName(String name) {
            if (name == null || name.isBlank()) {
                return LanguageManager.getLanguageString("tab.defaultName");
            }
            if (name.length() > 30) {
                return name.substring(0, 30) + "\u2026";
            }
            return name;
        }
    }
}
