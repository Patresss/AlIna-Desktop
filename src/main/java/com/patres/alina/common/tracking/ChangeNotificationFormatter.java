package com.patres.alina.common.tracking;

import com.patres.alina.uidesktop.ui.language.LanguageManager;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Builds human-readable notification messages from a {@link ChangeReport}.
 * Messages are formatted as Markdown and split into Added / Modified / Removed sections.
 * <p>
 * Each item line shows: key (as a clickable link when a {@code url} field exists) followed
 * by the display name. The display name is expected to already carry the right per-section
 * formatting (e.g. "[repo] title" for PRs, "10:00 Meeting" for calendar events).
 */
public final class ChangeNotificationFormatter {

    private static final String URL_FIELD = "url";

    private ChangeNotificationFormatter() {
    }

    /**
     * Formats a change report for a single dashboard section into a notification message.
     *
     * @param section the dashboard section that changed
     * @param report  the detected changes
     * @return formatted Markdown string ready for {@code ChatNotificationEvent}
     */
    public static String format(final DashboardSection section, final ChangeReport report) {
        final String sectionTitle = LanguageManager.getLanguageString(section.titleKey());
        final StringBuilder sb = new StringBuilder();
        boolean firstCategory = true;

        if (!report.added().isEmpty()) {
            sb.append(formatCategoryHeader(sectionTitle, "dashboard.changes.added", firstCategory));
            firstCategory = false;
            for (final TrackableItem item : report.added()) {
                sb.append(formatAddedItem(item)).append("\n");
            }
        }

        if (!report.modified().isEmpty()) {
            sb.append(formatCategoryHeader(sectionTitle, "dashboard.changes.modified", firstCategory));
            firstCategory = false;
            for (final ChangeReport.ModifiedItem item : report.modified()) {
                sb.append(formatModifiedItem(item)).append("\n");
            }
        }

        if (!report.removed().isEmpty()) {
            sb.append(formatCategoryHeader(sectionTitle, "dashboard.changes.removed", firstCategory));
            for (final TrackableItem item : report.removed()) {
                sb.append(formatRemovedItem(item)).append("\n");
            }
        }

        return sb.toString().stripTrailing();
    }

    private static String formatCategoryHeader(final String sectionTitle, final String categoryKey, final boolean first) {
        final String category = LanguageManager.getLanguageString(categoryKey);
        if (first) {
            return "**" + sectionTitle + "** \u2014 " + category + "\n";
        }
        return "\n" + category + "\n";
    }

    private static String formatAddedItem(final TrackableItem item) {
        return "\u2022 " + formatKeyWithLink(item.key(), item.fields()) + " \u2014 " + item.displayName();
    }

    private static String formatModifiedItem(final ChangeReport.ModifiedItem item) {
        final StringBuilder sb = new StringBuilder();
        sb.append("\u2022 ").append(formatKeyWithLink(item.key(), item.changes())).append(" \u2014 ").append(item.displayName());
        final StringJoiner changes = new StringJoiner(", ");
        for (final Map.Entry<String, ChangeReport.FieldChange> entry : item.changes().entrySet()) {
            if (URL_FIELD.equals(entry.getKey())) {
                continue; // url is not a visible change
            }
            final String fieldLabel = LanguageManager.getLanguageStringOrDefault(
                    entry.getKey(), "dashboard.changes.field." + entry.getKey());
            changes.add(fieldLabel + ": " + entry.getValue().oldValue() + " \u2192 " + entry.getValue().newValue());
        }
        final String changesStr = changes.toString();
        if (!changesStr.isEmpty()) {
            sb.append(" (").append(changesStr).append(")");
        }
        return sb.toString();
    }

    private static String formatRemovedItem(final TrackableItem item) {
        return "\u2022 " + formatKeyWithLink(item.key(), item.fields()) + " \u2014 " + item.displayName();
    }

    /**
     * Renders the tracking key as a Markdown link when a {@code url} field is present,
     * otherwise as bold text.
     */
    private static String formatKeyWithLink(final String key, final Map<String, ?> fieldsOrChanges) {
        final Object urlValue = fieldsOrChanges.get(URL_FIELD);
        if (urlValue instanceof String url && !url.isEmpty()) {
            return "[**" + key + "**](" + url + ")";
        }
        if (urlValue instanceof ChangeReport.FieldChange fc && !fc.newValue().isEmpty()) {
            return "[**" + key + "**](" + fc.newValue() + ")";
        }
        return "**" + key + "**";
    }
}
