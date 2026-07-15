package com.patres.alina.uidesktop.ui.dashboard;

import com.patres.alina.common.event.WorkspaceSettingsUpdatedEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.common.settings.DashboardCardId;
import com.patres.alina.common.settings.DashboardCardLayoutSettings;
import com.patres.alina.common.settings.DashboardLayoutSettings;
import com.patres.alina.common.settings.WorkspaceSettings;
import com.patres.alina.uidesktop.backend.BackendApi;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleExpression;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;

/**
 * Responsive command-center composition for all dashboard widgets.
 *
 * <p>At the normal application width it renders an aligned bento grid. In a
 * narrow split pane it falls back to one semantic column. Individual widgets
 * remain responsible only for their own content and collapse state.</p>
 */
public final class DashboardContainer extends VBox {

    private static final double GRID_GAP = 10;

    private final GridPane widgetGrid = new GridPane();
    private final FontIcon collapseIcon = new FontIcon(Feather.CHEVRON_UP);
    private final HBox collapseBar = new HBox(collapseIcon);

    private final MediaControlWidget mediaControlWidget;
    private final DashboardPane dashboardPane;
    private final GitHubWidget gitHubWidget;
    private final JiraWidget jiraWidget;
    private final GoogleCalendarWidget googleCalendarWidget;
    private final ObsidianWidget obsidianWidget;
    private final List<CardNode> cardNodes;

    private DashboardLayoutMode layoutMode;
    private boolean collapsed;
    private boolean layoutRefreshScheduled;
    private Runnable onCollapsedStateChanged;

    public DashboardContainer(MediaControlWidget mediaControlWidget,
                              DashboardPane dashboardPane,
                              GitHubWidget gitHubWidget,
                              JiraWidget jiraWidget,
                              GoogleCalendarWidget googleCalendarWidget,
                              ObsidianWidget obsidianWidget) {
        this.mediaControlWidget = mediaControlWidget;
        this.dashboardPane = dashboardPane;
        this.gitHubWidget = gitHubWidget;
        this.jiraWidget = jiraWidget;
        this.googleCalendarWidget = googleCalendarWidget;
        this.obsidianWidget = obsidianWidget;
        this.cardNodes = List.of(
                new CardNode(DashboardCardId.MUSIC, mediaControlWidget),
                new CardNode(DashboardCardId.TASKS, dashboardPane),
                new CardNode(DashboardCardId.CALENDAR, googleCalendarWidget),
                new CardNode(DashboardCardId.GITHUB, gitHubWidget),
                new CardNode(DashboardCardId.JIRA, jiraWidget),
                new CardNode(DashboardCardId.OBSIDIAN, obsidianWidget)
        );

        getStyleClass().add("workspace-dashboard-container");
        widgetGrid.getStyleClass().add("workspace-dashboard-grid");
        widgetGrid.setHgap(GRID_GAP);
        widgetGrid.setVgap(GRID_GAP);
        widgetGrid.setMaxWidth(Double.MAX_VALUE);

        configureCard(mediaControlWidget);
        configureCard(dashboardPane);
        configureCard(googleCalendarWidget);
        configureCard(gitHubWidget);
        configureCard(jiraWidget);
        configureCard(obsidianWidget);
        cardNodes.forEach(cardNode -> cardNode.node().managedProperty().addListener(
                (observable, oldValue, newValue) -> scheduleLayoutRefresh()
        ));

        collapseIcon.getStyleClass().add("workspace-collapse-bar-icon");
        collapseBar.getStyleClass().add("workspace-collapse-bar");
        collapseBar.setAlignment(Pos.CENTER);
        collapseBar.setOnMouseClicked(event -> toggleCollapsed());

        setMinWidth(0);
        getChildren().addAll(widgetGrid, collapseBar);
        widthProperty().addListener((observable, oldWidth, newWidth) ->
                applyResponsiveLayout(false)
        );
        applyResponsiveLayout(false);
        updateCollapsedState();

        DefaultEventBus.getInstance().subscribe(
                WorkspaceSettingsUpdatedEvent.class,
                event -> refreshVisibility()
        );
        refreshVisibility();
    }

    private void configureCard(Node card) {
        GridPane.setHgrow(card, Priority.ALWAYS);
        GridPane.setVgrow(card, Priority.NEVER);
        GridPane.setFillHeight(card, true);
        GridPane.setValignment(card, VPos.TOP);
        if (card instanceof javafx.scene.layout.Region region) {
            region.setMinWidth(0);
            region.setMaxWidth(Double.MAX_VALUE);
            region.setMaxHeight(Double.MAX_VALUE);
        }
    }

    private void applyResponsiveLayout(boolean force) {
        final DashboardLayoutSettings dashboardLayout = BackendApi.getWorkspaceSettings().dashboardLayout();
        final DashboardLayoutMode newMode = DashboardLayoutMode.forWidth(
                getWidth(),
                dashboardLayout.twoColumnBreakpoint()
        );
        applyLayout(newMode, dashboardLayout, force);
    }

    private void applyLayout(DashboardLayoutMode newMode,
                             DashboardLayoutSettings dashboardLayout,
                             boolean force) {
        if (!force && layoutMode == newMode) {
            return;
        }
        layoutMode = newMode;
        widgetGrid.getChildren().clear();
        widgetGrid.getColumnConstraints().clear();

        if (newMode == DashboardLayoutMode.TWO_COLUMNS) {
            widgetGrid.getStyleClass().remove("workspace-dashboard-grid-single");
            if (!widgetGrid.getStyleClass().contains("workspace-dashboard-grid-wide")) {
                widgetGrid.getStyleClass().add("workspace-dashboard-grid-wide");
            }
            widgetGrid.getColumnConstraints().addAll(equalColumn(2), equalColumn(2));
        } else {
            widgetGrid.getStyleClass().remove("workspace-dashboard-grid-wide");
            if (!widgetGrid.getStyleClass().contains("workspace-dashboard-grid-single")) {
                widgetGrid.getStyleClass().add("workspace-dashboard-grid-single");
            }
            widgetGrid.getColumnConstraints().add(equalColumn(1));
        }

        final List<DashboardGridPlanner.Card> visibleCards = cardNodes.stream()
                .filter(cardNode -> cardNode.node().isManaged())
                .map(cardNode -> plannerCard(cardNode, dashboardLayout))
                .toList();
        for (DashboardGridPlanner.Placement placement : DashboardGridPlanner.plan(visibleCards, newMode)) {
            final Node card = nodeFor(placement.id());
            widgetGrid.add(
                    card,
                    placement.column(),
                    placement.row(),
                    placement.columnSpan(),
                    1
            );
        }
    }

    private DashboardGridPlanner.Card plannerCard(CardNode cardNode,
                                                   DashboardLayoutSettings dashboardLayout) {
        final DashboardCardLayoutSettings cardLayout = dashboardLayout.card(cardNode.id());
        return new DashboardGridPlanner.Card(
                cardNode.id(),
                Boolean.TRUE.equals(cardLayout.canUseHalfWidth()),
                cardLayout.order()
        );
    }

    private Node nodeFor(DashboardCardId cardId) {
        return cardNodes.stream()
                .filter(cardNode -> cardNode.id() == cardId)
                .map(CardNode::node)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown dashboard card: " + cardId));
    }

    private ColumnConstraints equalColumn(int columnCount) {
        return equalColumn(widgetGrid.widthProperty(), GRID_GAP, columnCount);
    }

    static ColumnConstraints equalColumn(DoubleExpression gridWidth, double gap, int columnCount) {
        final ColumnConstraints column = new ColumnConstraints();
        final var availableWidth = gridWidth.subtract(gap * (columnCount - 1));
        final var columnWidth = Bindings.max(0.0, availableWidth.divide(columnCount));

        // Bind both preferred and maximum width to the exact track size. HGrow
        // alone distributes space after child preferences, allowing a long
        // calendar or Jira row to make its column wider than its neighbour.
        column.setMinWidth(0);
        column.prefWidthProperty().bind(columnWidth);
        column.maxWidthProperty().bind(columnWidth);
        column.setHgrow(Priority.NEVER);
        column.setFillWidth(true);
        return column;
    }

    private void toggleCollapsed() {
        collapsed = !collapsed;
        updateCollapsedState();
    }

    public void collapse() {
        if (!collapsed) {
            collapsed = true;
            updateCollapsedState();
        }
    }

    public void expand() {
        if (collapsed) {
            collapsed = false;
            updateCollapsedState();
        }
    }

    private void updateCollapsedState() {
        widgetGrid.setManaged(!collapsed);
        widgetGrid.setVisible(!collapsed);
        collapseIcon.setIconCode(collapsed ? Feather.CHEVRON_DOWN : Feather.CHEVRON_UP);
        pseudoClassStateChanged(
                javafx.css.PseudoClass.getPseudoClass("collapsed"),
                collapsed
        );
        if (onCollapsedStateChanged != null) {
            onCollapsedStateChanged.run();
        }
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setOnCollapsedStateChanged(Runnable callback) {
        onCollapsedStateChanged = callback;
    }

    private void refreshVisibility() {
        Platform.runLater(() -> {
            final WorkspaceSettings settings = BackendApi.getWorkspaceSettings();
            final boolean show = settings.showDashboard();
            setManaged(show);
            setVisible(show);

            updateWidgetVisibility(mediaControlWidget, settings.showDashboardMusic());
            updateWidgetVisibility(dashboardPane, settings.showDashboardTasks());
            updateWidgetVisibility(gitHubWidget, settings.showDashboardGithub());
            updateWidgetVisibility(jiraWidget, settings.showDashboardJira());
            updateWidgetVisibility(googleCalendarWidget, settings.showDashboardCalendar());
            updateWidgetVisibility(obsidianWidget, settings.showDashboardObsidian());
            scheduleLayoutRefresh();
        });
    }

    private void scheduleLayoutRefresh() {
        if (layoutRefreshScheduled) {
            return;
        }
        layoutRefreshScheduled = true;
        Platform.runLater(() -> {
            layoutRefreshScheduled = false;
            applyResponsiveLayout(true);
        });
    }

    private void updateWidgetVisibility(Node widget, boolean visible) {
        widget.setManaged(visible);
        widget.setVisible(visible);
    }

    private record CardNode(DashboardCardId id, Node node) {
    }
}
