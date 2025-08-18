package com.patres.alina.common.settings;

import java.util.Objects;

public final class UiSettings {

    private String theme;
    private String language;

    public UiSettings(
            String theme,
            String language) {
        this.theme = theme;
        this.language = language;
    }

    public UiSettings() {
        this("Primer Light", "en");
    }


    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (UiSettings) obj;
        return Objects.equals(this.theme, that.theme) &&
                Objects.equals(this.language, that.language);
    }

    @Override
    public int hashCode() {
        return Objects.hash(theme, language);
    }

    @Override
    public String toString() {
        return "UiSettings[" +
                "theme=" + theme + ", " +
                "language=" + language + ']';
    }

}
