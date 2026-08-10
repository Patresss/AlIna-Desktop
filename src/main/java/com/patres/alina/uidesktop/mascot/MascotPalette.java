package com.patres.alina.uidesktop.mascot;

import java.awt.Color;

record MascotPalette(
        Color surface,
        Color subtleSurface,
        Color text,
        Color mutedText,
        Color border,
        Color accent,
        Color accentStrong,
        Color accentSoft,
        Color success,
        Color successSoft,
        Color danger,
        Color dangerSoft,
        boolean dark
) {

    static MascotPalette calmLight() {
        return new MascotPalette(
                color("#ffffff"),
                color("#f1f5f9"),
                color("#172033"),
                color("#64748b"),
                color("#cbd5e1"),
                color("#6d28d9"),
                color("#7c3aed"),
                color("#ede9fe"),
                color("#15803d"),
                color("#dcfce7"),
                color("#be123c"),
                color("#ffe4e6"),
                false
        );
    }

    static MascotPalette calmDark() {
        return new MascotPalette(
                color("#151d2d"),
                color("#111827"),
                color("#e5e7eb"),
                color("#94a3b8"),
                color("#334155"),
                color("#a78bfa"),
                color("#8b5cf6"),
                color("#2e2447"),
                color("#4ade80"),
                color("#12351f"),
                color("#fb7185"),
                color("#3d1822"),
                true
        );
    }

    Color shadow() {
        return dark ? new Color(0, 0, 0, 110) : new Color(15, 23, 42, 58);
    }

    Color primaryText() {
        final double luminance = 0.2126 * accentStrong.getRed()
                + 0.7152 * accentStrong.getGreen()
                + 0.0722 * accentStrong.getBlue();
        return luminance < 150 ? Color.WHITE : new Color(17, 24, 39);
    }

    private static Color color(final String value) {
        return new Color(Integer.parseInt(value.substring(1), 16));
    }
}
