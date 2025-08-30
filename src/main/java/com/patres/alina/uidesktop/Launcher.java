package com.patres.alina.uidesktop;

import com.github.kwhat.jnativehook.NativeHookException;
import com.patres.alina.uidesktop.ui.AssistantAppLauncher;
import javafx.application.Application;
import javafx.stage.Stage;

public class Launcher extends Application {


    public static final boolean IS_DEV_MODE = "DEV".equalsIgnoreCase(
            Resources.getPropertyOrEnv("atlantafx.mode", "ATLANTAFX_MODE")
    );

    public static void launchFxApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws NativeHookException {
        new AssistantAppLauncher().start(stage);
    }

}
