package com.patres.alina.common.tracking;

import java.util.List;
import java.util.Map;

/**
 * Result of comparing two snapshots of a dashboard section.
 * Contains items that were added, removed, or modified between refreshes.
 *
 * @param added    items present in the new snapshot but absent from the previous one
 * @param removed  items present in the previous snapshot but absent from the new one
 * @param modified items present in both snapshots with at least one changed field
 */
public record ChangeReport(
        List<TrackableItem> added,
        List<TrackableItem> removed,
        List<ModifiedItem> modified
) {

    /**
     * An item whose tracked fields changed between snapshots.
     *
     * @param key         the stable tracking key of the item
     * @param displayName human-readable name for notifications
     * @param changes     map of field name to old/new value pair
     */
    public record ModifiedItem(
            String key,
            String displayName,
            Map<String, FieldChange> changes
    ) {
    }

    /**
     * A single field value change.
     *
     * @param oldValue the previous field value
     * @param newValue the current field value
     */
    public record FieldChange(
            String oldValue,
            String newValue
    ) {
    }

    public boolean hasChanges() {
        return !added.isEmpty() || !removed.isEmpty() || !modified.isEmpty();
    }
}
