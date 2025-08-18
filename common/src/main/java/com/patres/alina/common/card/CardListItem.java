package com.patres.alina.common.card;

public record CardListItem(
        String id,
        String name,
        String description,
        String icon,
        State state
) {
}
