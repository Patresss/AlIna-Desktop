package com.patres.alina.uidesktop.ui.language;

import java.util.Arrays;
import java.util.Locale;

public enum ApplicationLanguage {

    ENGLISH(new Locale("en"), "English/English"),
    POLISH(new Locale("pl"), "Polski/Polish");

    private final Locale locale;
    private final String languageName;

    ApplicationLanguage(Locale locale, String languageName) {
        this.locale = locale;
        this.languageName = languageName;
    }

    public Locale getLocale() {
        return locale;
    }

    public String getLanguageName() {
        return languageName;
    }

    public static ApplicationLanguage getApplicationLanguage(String locale) {
        return Arrays.stream(values())
                .filter(applicationLanguage -> applicationLanguage.locale.getLanguage().equals(locale))
                .findFirst()
                .orElse(ENGLISH);
    }
}
