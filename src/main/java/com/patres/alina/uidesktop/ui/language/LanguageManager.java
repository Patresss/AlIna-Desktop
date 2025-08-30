/* SPDX-License-Identifier: MIT */

package com.patres.alina.uidesktop.ui.language;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class LanguageManager {

    private static final Logger logger = LoggerFactory.getLogger(LanguageManager.class);

    private static final ObjectProperty<Locale> locale = new SimpleObjectProperty<>(ApplicationLanguage.ENGLISH.getLocale());

    static {
        locale.addListener((observable, oldValue, newValue) -> Locale.setDefault(newValue));
    }


    public static void setLanguage(String language) {
        setLanguage(ApplicationLanguage.getApplicationLanguage(language));
    }


    public static void setLanguage(ApplicationLanguage language) {
        Locale newLocale = language.getLocale();
        locale.set(newLocale);
        Locale.setDefault(newLocale);
    }

    public static StringBinding createStringBinding(String key, Object... args) {
        return Bindings.createStringBinding(() -> getLanguageString(key, args), locale);
    }

    public static String getLanguageString(String key, Object... args) {
        try {
            return MessageFormat.format(getBundle().getString(key), args);
        } catch (Exception e) {
            logger.error("Cannot get Language String: key={}, locale={}", key, locale.get());
            return key;
        }
    }

    public static String getLanguageStringOrDefault(String defaultText, String key, Object... args) {
        if (key == null) {
            return null;
        }
        try {
            return MessageFormat.format(getBundle().getString(key), args);
        } catch (Exception e) {
            logger.warn("Cannot get Language String: key={}, locale={}. Default name will be used: {}", key, locale.get(), defaultText);
            return defaultText;
        }
    }

    public static ResourceBundle getBundle() {
        return ResourceBundle.getBundle("language/Bundle", locale.get());
    }
}