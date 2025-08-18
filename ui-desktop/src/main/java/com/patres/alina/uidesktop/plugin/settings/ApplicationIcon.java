package com.patres.alina.uidesktop.plugin.settings;

import org.kordamp.ikonli.Ikon;

public record ApplicationIcon(Ikon ikon,
                              String code,
                              String name,
                              String displayedName) {


    public ApplicationIcon(Ikon ikon) {
        this(
                ikon,
                (char) ikon.getCode() + "",
                ikon.getDescription(),
                ikon.getDescription().toUpperCase().replaceAll("[^A-Z\\d]", " ")); // bootstrap font doesn't support lowercase and special characters
    }
}
