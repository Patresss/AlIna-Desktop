package com.patres.alina.common.tracking;

import java.util.*;

/**
 * Generic change detection engine.
 * Compares two ordered lists of {@link TrackableItem} by their keys
 * and produces a {@link ChangeReport} describing additions, removals,
 * and field-level modifications.
 */
public final class ChangeDetector {

    private ChangeDetector() {
    }

    /**
     * Detects changes between a previous and current list of trackable items.
     *
     * @param previous items from the last saved snapshot (may be empty on first run)
     * @param current  items from the latest dashboard refresh
     * @return a report of all detected changes
     */
    public static ChangeReport detect(final List<TrackableItem> previous, final List<TrackableItem> current) {
        final Map<String, TrackableItem> previousByKey = indexByKey(previous);
        final Map<String, TrackableItem> currentByKey = indexByKey(current);

        final List<TrackableItem> added = new ArrayList<>();
        final List<TrackableItem> removed = new ArrayList<>();
        final List<ChangeReport.ModifiedItem> modified = new ArrayList<>();

        // Detect added and modified items (iterate current to preserve order)
        for (final TrackableItem currentItem : current) {
            final TrackableItem previousItem = previousByKey.get(currentItem.key());
            if (previousItem == null) {
                added.add(currentItem);
            } else {
                detectFieldChanges(previousItem, currentItem).ifPresent(modified::add);
            }
        }

        // Detect removed items (iterate previous to preserve order)
        for (final TrackableItem previousItem : previous) {
            if (!currentByKey.containsKey(previousItem.key())) {
                removed.add(previousItem);
            }
        }

        return new ChangeReport(
                Collections.unmodifiableList(added),
                Collections.unmodifiableList(removed),
                Collections.unmodifiableList(modified)
        );
    }

    private static Optional<ChangeReport.ModifiedItem> detectFieldChanges(
            final TrackableItem previous,
            final TrackableItem current) {

        final Map<String, ChangeReport.FieldChange> changes = new LinkedHashMap<>();

        for (final Map.Entry<String, String> entry : current.fields().entrySet()) {
            final String fieldName = entry.getKey();
            final String currentValue = entry.getValue();
            final String previousValue = previous.fields().getOrDefault(fieldName, "");

            if (!Objects.equals(previousValue, currentValue)) {
                changes.put(fieldName, new ChangeReport.FieldChange(previousValue, currentValue));
            }
        }

        // Check for fields that existed before but are now absent
        for (final String fieldName : previous.fields().keySet()) {
            if (!current.fields().containsKey(fieldName)) {
                changes.put(fieldName, new ChangeReport.FieldChange(
                        previous.fields().get(fieldName), ""));
            }
        }

        if (changes.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ChangeReport.ModifiedItem(
                current.key(),
                current.displayName(),
                Collections.unmodifiableMap(changes)
        ));
    }

    private static Map<String, TrackableItem> indexByKey(final List<TrackableItem> items) {
        final Map<String, TrackableItem> index = new LinkedHashMap<>();
        for (final TrackableItem item : items) {
            index.put(item.key(), item);
        }
        return index;
    }
}
