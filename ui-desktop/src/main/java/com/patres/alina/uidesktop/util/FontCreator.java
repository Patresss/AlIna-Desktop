package com.patres.alina.uidesktop.util;

import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FontCreator {

    private static final Logger logger = LoggerFactory.getLogger(FontCreator.class);

    public static FontIcon safetlyCreateFontIcon(final String iconCode) {
        try {
            return new FontIcon(iconCode);
        } catch (final Exception e) {
            logger.error("Cannot create font icon", e);
            return null;
        }
    }
}
