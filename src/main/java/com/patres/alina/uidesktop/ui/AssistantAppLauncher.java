package com.patres.alina.uidesktop.ui;

import com.github.kwhat.jnativehook.NativeHookException;
import com.patres.alina.common.event.WorkspaceSettingsUpdatedEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.DefaultExceptionHandler;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.backend.BackendApi;
import com.patres.alina.uidesktop.shortcuts.listener.ShortcutKeyListener;
import com.patres.alina.uidesktop.ui.contextmenu.AppGlobalContextMenu;
import com.patres.alina.uidesktop.shortcuts.listener.CommandShortcutListener;
import com.patres.alina.uidesktop.ui.language.LanguageManager;
import com.patres.alina.uidesktop.ui.theme.ThemeManager;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;

public class AssistantAppLauncher {

    public static final int WIDTH = 760;

    public void start(Stage stage) throws NativeHookException {
        Thread.currentThread().setUncaughtExceptionHandler(new DefaultExceptionHandler(stage));

        LanguageManager.setLanguage(UI_SETTINGS.getSettings().language());

        var screenBounds = Screen.getPrimary().getVisualBounds();
        var screenHeight = screenBounds.getHeight();

        var root = new ApplicationWindow();

        var antialiasing = Platform.isSupported(ConditionalFeature.SCENE3D)
                ? SceneAntialiasing.BALANCED
                : SceneAntialiasing.DISABLED;

        var headerBar = new HeaderBar();
        var headerButtonBox = new ApplicationHeaderButtonBox(root);
        headerBar.setTrailing(headerButtonBox);
        headerBar.setLeading(new HeaderEventCountdown());
        HeaderBar.setPrefButtonHeight(stage, 100d);

        root.setTop(headerBar);
        root.setHeaderButtonBox(headerButtonBox);

        var sceneRoot = new StackPane(root);
        var scene = new Scene(sceneRoot, WIDTH, screenHeight, false, antialiasing);
        stage.initStyle(StageStyle.EXTENDED);

        var tm = ThemeManager.getInstance();
        tm.setScene(scene);
        tm.setTheme(UI_SETTINGS.getSettings().theme());

        scene.getStylesheets().addAll(Resources.resolve("assets/styles/index.css"));

        stage.setScene(scene);
        stage.setTitle("AlIna");
        loadIcons(stage);
        stage.setResizable(true);
        stage.setAlwaysOnTop(BackendApi.getWorkspaceSettings().keepWindowAlwaysOnTop());
        stage.setOnCloseRequest(t -> Platform.exit());

        DefaultEventBus.getInstance().subscribe(
                WorkspaceSettingsUpdatedEvent.class,
                event -> Platform.runLater(() ->
                        stage.setAlwaysOnTop(BackendApi.getWorkspaceSettings().keepWindowAlwaysOnTop())
                )
        );

        // register event listeners

        var sideExpandButton = new SideExpandButton();
        sideExpandButton.attach(stage, root, sceneRoot);

        // Register in-app keyboard shortcuts (Shift + Arrow keys)
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.isShiftDown()) {
                switch (event.getCode()) {
                    case UP -> {
                        root.collapseDashboard();
                        event.consume();
                    }
                    case DOWN -> {
                        root.expandDashboard();
                        event.consume();
                    }
                    case LEFT -> {
                        sideExpandButton.expand();
                        event.consume();
                    }
                    case RIGHT -> {
                        sideExpandButton.shrink();
                        event.consume();
                    }
                }
            }
        });

        Platform.runLater(() -> {
            stage.setX(screenBounds.getMinX() + screenBounds.getWidth() - WIDTH);
            stage.setY(screenBounds.getMinY());
            stage.show();
            stage.requestFocus();
        });

        AppGlobalContextMenu.init(root);
        CommandShortcutListener.init(root);
        ShortcutKeyListener.init();
    }

    private void loadIcons(Stage stage) {
        int iconSize = 16;
        while (iconSize <= 1024) {
            // we could use the square icons for Windows here
            stage.getIcons().add(new Image(Resources.getResourceAsStream("assets/icon-rounded-" + iconSize + ".png")));
            iconSize *= 2;
        }
    }
}
