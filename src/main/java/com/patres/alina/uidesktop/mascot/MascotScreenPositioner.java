package com.patres.alina.uidesktop.mascot;

import java.util.List;

final class MascotScreenPositioner {

    private MascotScreenPositioner() {
    }

    static Position bottomRight(final List<ScreenBounds> screens,
                                final double cursorX,
                                final double cursorY,
                                final double popupWidth,
                                final double popupHeight,
                                final double margin) {
        if (screens == null || screens.isEmpty()) {
            return new Position(margin, margin);
        }
        final ScreenBounds screen = screens.stream()
                .filter(candidate -> candidate.contains(cursorX, cursorY))
                .findFirst()
                .orElse(screens.getFirst());
        final double x = clamp(
                screen.minX() + screen.width() - popupWidth - margin,
                screen.minX(),
                screen.minX() + Math.max(0, screen.width() - popupWidth)
        );
        final double y = clamp(
                screen.minY() + screen.height() - popupHeight - margin,
                screen.minY(),
                screen.minY() + Math.max(0, screen.height() - popupHeight)
        );
        return new Position(x, y);
    }

    private static double clamp(final double value, final double minimum, final double maximum) {
        return Math.max(minimum, Math.min(value, maximum));
    }

    record ScreenBounds(double minX, double minY, double width, double height) {
        boolean contains(final double x, final double y) {
            return x >= minX && x < minX + width && y >= minY && y < minY + height;
        }
    }

    record Position(double x, double y) {
    }
}
