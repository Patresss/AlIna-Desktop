package com.patres.alina.common.tracking;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-serializable snapshot of all dashboard sections.
 * Each key is a {@link DashboardSection#id()} and each value is the list of
 * {@link TrackableItem} instances captured during the last refresh.
 */
public record DashboardStateSnapshot(
        Map<String, List<TrackableItem>> sections
) {

    public DashboardStateSnapshot() {
        this(new HashMap<>());
    }
}
