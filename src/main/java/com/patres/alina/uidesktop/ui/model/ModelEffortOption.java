package com.patres.alina.uidesktop.ui.model;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * A model-specific reasoning effort/variant option displayed by the desktop UI.
 */
public record ModelEffortOption(String value, String label) {

    private static final List<String> FALLBACK_VALUES = List.of("low", "medium", "high", "xhigh", "max");

    public ModelEffortOption {
        value = value == null ? "" : value.trim();
        label = label == null || label.isBlank() ? humanize(value) : label.trim();
    }

    @Override
    public String toString() {
        return label;
    }

    public static ModelEffortOption fromValue(final String value) {
        return new ModelEffortOption(value, humanize(value));
    }

    public static List<ModelEffortOption> fromValues(final List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(ModelEffortOption::fromValue)
                .distinct()
                .toList();
    }

    public static List<ModelEffortOption> choices(final String defaultLabel, final List<String> values) {
        final List<ModelEffortOption> supportedOptions = fromValues(values);
        final List<ModelEffortOption> options = supportedOptions.isEmpty()
                ? fromValues(FALLBACK_VALUES)
                : supportedOptions;
        return Stream.concat(
                Stream.of(new ModelEffortOption("", defaultLabel)),
                options.stream()
        ).toList();
    }

    public static ModelEffortOption select(final List<ModelEffortOption> options, final String preferredValue) {
        final String preferred = preferredValue == null ? "" : preferredValue.trim();
        return options.stream()
                .filter(option -> option.value().equalsIgnoreCase(preferred))
                .findFirst()
                .orElseGet(() -> fromValue(preferred));
    }

    public static String humanize(final String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "Default";
        }
        return switch (rawValue.trim().toLowerCase(Locale.ROOT)) {
            case "none" -> "None";
            case "minimal" -> "Minimal";
            case "low" -> "Light";
            case "medium" -> "Medium";
            case "high" -> "High";
            case "xhigh", "extra-high", "extra_high" -> "Extra High";
            case "max" -> "Max";
            case "ultra" -> "Ultra";
            default -> titleCase(rawValue.trim());
        };
    }

    private static String titleCase(final String value) {
        final StringBuilder result = new StringBuilder();
        for (final String part : value.split("[-_ ]+")) {
            if (part.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return result.isEmpty() ? value : result.toString();
    }
}
