package com.patres.alina.common.settings;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Responsive and per-card placement preferences for the dashboard. */
public record DashboardLayoutSettings(
        int twoColumnBreakpoint,
        Map<String, DashboardCardLayoutSettings> cards
) {

    public static final int DEFAULT_TWO_COLUMN_BREAKPOINT = 680;
    public static final int MIN_TWO_COLUMN_BREAKPOINT = 320;
    public static final int MAX_TWO_COLUMN_BREAKPOINT = 8192;
    public static final int MIN_CARD_ORDER = 1;
    public static final int MAX_CARD_ORDER = 9999;

    public DashboardLayoutSettings() {
        this(DEFAULT_TWO_COLUMN_BREAKPOINT, Map.of());
    }

    public DashboardLayoutSettings {
        twoColumnBreakpoint = normalizeBreakpoint(twoColumnBreakpoint);

        final Map<String, DashboardCardLayoutSettings> requested = cards == null ? Map.of() : cards;
        final Map<String, DashboardCardLayoutSettings> normalized = new LinkedHashMap<>();
        for (DashboardCardId cardId : DashboardCardId.values()) {
            normalized.put(cardId.key(), normalizeCard(cardId, requested.get(cardId.key())));
        }
        cards = Collections.unmodifiableMap(normalized);
    }

    public DashboardCardLayoutSettings card(DashboardCardId cardId) {
        return cards.get(cardId.key());
    }

    private static int normalizeBreakpoint(int breakpoint) {
        if (breakpoint <= 0) {
            return DEFAULT_TWO_COLUMN_BREAKPOINT;
        }
        return Math.clamp(breakpoint, MIN_TWO_COLUMN_BREAKPOINT, MAX_TWO_COLUMN_BREAKPOINT);
    }

    private static DashboardCardLayoutSettings normalizeCard(DashboardCardId cardId,
                                                              DashboardCardLayoutSettings requested) {
        if (requested == null) {
            return defaultCard(cardId);
        }
        final boolean canUseHalfWidth = requested.canUseHalfWidth() == null
                ? cardId.defaultCanUseHalfWidth()
                : requested.canUseHalfWidth();
        final int order = requested.order() == null
                ? cardId.defaultOrder()
                : Math.clamp(requested.order(), MIN_CARD_ORDER, MAX_CARD_ORDER);
        return new DashboardCardLayoutSettings(canUseHalfWidth, order);
    }

    private static DashboardCardLayoutSettings defaultCard(DashboardCardId cardId) {
        return new DashboardCardLayoutSettings(
                cardId.defaultCanUseHalfWidth(),
                cardId.defaultOrder()
        );
    }
}
