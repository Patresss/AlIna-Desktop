/* SPDX-License-Identifier: MIT */

package com.patres.alina.uidesktop.ui.theme;

import atlantafx.base.theme.*;
import com.patres.alina.uidesktop.Resources;
import com.patres.alina.uidesktop.common.event.ThemeEvent;
import com.patres.alina.common.event.bus.DefaultEventBus;
import com.patres.alina.uidesktop.ui.chat.HighlightJSTheme;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import java.util.Objects;
import java.util.Set;

public final class ThemeManager {


    public static final String DEFAULT_FONT_FAMILY_NAME = "Inter";
    public static final int DEFAULT_FONT_SIZE = 14;



    static final String[] APP_STYLESHEETS = new String[]{
            Resources.resolve("assets/styles/index.css")
    };
    static final Set<Class<? extends Theme>> PROJECT_THEMES = Set.of(
            PrimerLight.class,
            PrimerDark.class,
            NordLight.class,
            NordDark.class,
            CupertinoLight.class,
            CupertinoDark.class,
            Dracula.class
    );

    private static final PseudoClass DARK = PseudoClass.getPseudoClass("dark");
    private static final PseudoClass USER_CUSTOM = PseudoClass.getPseudoClass("user-custom");

    private final ThemeRepository repository = new ThemeRepository();

    private Scene scene;

    private SamplerTheme currentTheme = null;

    public ThemeRepository getRepository() {
        return repository;
    }

    public Scene getScene() {
        return scene;
    }

    // MUST BE SET ON STARTUP
    // (this is supposed to be a constructor arg, but since app don't use DI..., sorry)
    public void setScene(Scene scene) {
        this.scene = Objects.requireNonNull(scene);
    }

    public SamplerTheme getTheme() {
        return currentTheme;
    }

    public SamplerTheme getDefaultTheme() {
        return getRepository().getAll().getFirst();
    }

    public void setTheme(String theme) {
        setTheme(fromString(theme));
    }


    /**
     * See {@link SamplerTheme}.
     */
    public void setTheme(SamplerTheme theme) {
        if (theme != currentTheme) {
            Objects.requireNonNull(theme);

            if (currentTheme != null) {
                animateThemeChange(Duration.millis(750));
            }

            Application.setUserAgentStylesheet(Objects.requireNonNull(theme.getUserAgentStylesheet()));
            getScene().getStylesheets().setAll(theme.getAllStylesheets());
            getScene().getRoot().pseudoClassStateChanged(DARK, theme.isDarkMode());

            // remove user CSS customizations and reset accent on theme change
            resetCustomCSS();

            currentTheme = theme;
            DefaultEventBus.getInstance().publish(new ThemeEvent(ThemeEvent.EventType.THEME_CHANGE));
        }
    }


    private void animateThemeChange(Duration duration) {
        Image snapshot = scene.snapshot(null);
        Pane root = (Pane) scene.getRoot();

        ImageView imageView = new ImageView(snapshot);
        root.getChildren().add(imageView); // add snapshot on top

        var transition = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(imageView.opacityProperty(), 1, Interpolator.EASE_OUT)),
                new KeyFrame(duration, new KeyValue(imageView.opacityProperty(), 0, Interpolator.EASE_OUT))
        );
        transition.setOnFinished(e -> root.getChildren().remove(imageView));
        transition.play();
    }

    public void resetCustomCSS() {
        getScene().getRoot().pseudoClassStateChanged(USER_CUSTOM, false);
    }

    public SamplerTheme fromString(String themeName) {
        return getRepository().getAll().stream()
                .filter(t -> Objects.equals(themeName, t.getName()))
                .findFirst()
                .orElse(getDefaultTheme());
    }

    ///////////////////////////////////////////////////////////////////////////
    // Singleton                                                             //
    ///////////////////////////////////////////////////////////////////////////

    private ThemeManager() {
    }

    private static class InstanceHolder {

        private static final ThemeManager INSTANCE = new ThemeManager();
    }

    public static ThemeManager getInstance() {
        return InstanceHolder.INSTANCE;
    }
}
