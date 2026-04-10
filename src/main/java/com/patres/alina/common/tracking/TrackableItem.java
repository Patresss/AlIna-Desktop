package com.patres.alina.common.tracking;

import java.util.Map;

/**
 * Normalized representation of a dashboard item for change tracking.
 * Each item has a unique key (used for identity across refreshes),
 * a human-readable display name, and a map of trackable field values.
 *
 * @param key         unique identifier within a section (e.g. Jira key, PR number, event summary)
 * @param displayName human-readable name for notification messages
 * @param fields      named field values to compare between snapshots (e.g. "status" -> "In Progress")
 */
public record TrackableItem(
        String key,
        String displayName,
        Map<String, String> fields
) {
}
