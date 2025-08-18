package com.patres.alina.uidesktop.ui;

import com.github.kwhat.jnativehook.NativeHookException;
import com.patres.alina.uidesktop.DefaultExceptionHandler;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.shortcuts.ShortcutKeyListener;
import com.patres.alina.uidesktop.ui.theme.ThemeManager;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;


import static com.patres.alina.uidesktop.settings.SettingsMangers.UI_SETTINGS;

public class AssistantAppLauncher {

    public static final double GOLDEN_RATIO = 1.618;
    public static final int WIDTH = 480;
    public static final int HIGH = (int) (WIDTH * GOLDEN_RATIO);

    public void start(Stage stage) throws NativeHookException {
        Thread.currentThread().setUncaughtExceptionHandler(new DefaultExceptionHandler(stage));


        var root = new ApplicationWindow();

        var antialiasing = Platform.isSupported(ConditionalFeature.SCENE3D)
                ? SceneAntialiasing.BALANCED
                : SceneAntialiasing.DISABLED;
        var scene = new Scene(root, WIDTH, HIGH, false, antialiasing);
        stage.initStyle(StageStyle.EXTENDED);

        var headerBar = new HeaderBar();
        headerBar.setTrailing(new ApplicationHeaderButtonBox(root));
        HeaderBar.setPrefButtonHeight(stage, 100d);


        root.setTop(headerBar);

        // TODO brzydkie
        var tm = ThemeManager.getInstance();
        tm.setScene(scene);
        tm.setTheme(UI_SETTINGS.getSettings().theme());

        scene.getStylesheets().addAll(Resources.resolve("assets/styles/index.css"));

        stage.setScene(scene);
        stage.setTitle(System.getProperty("app.name"));
        loadIcons(stage);
        stage.setResizable(true);
        stage.setOnCloseRequest(t -> Platform.exit());

        // register event listeners

        Platform.runLater(() -> {
            stage.show();
            stage.requestFocus();
        });

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
