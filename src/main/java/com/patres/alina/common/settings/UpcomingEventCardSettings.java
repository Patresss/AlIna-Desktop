package com.patres.alina.common.settings;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Content preferences for the upcoming/current Calendar event card. */
public record UpcomingEventCardSettings(
        boolean visible,
        int attendeePreviewLimit,
        int descriptionPreviewCharacters,
        boolean showAttachments
) {

    public static final int DEFAULT_ATTENDEE_PREVIEW_LIMIT = 4;
    public static final int MIN_ATTENDEE_PREVIEW_LIMIT = 1;
    public static final int MAX_ATTENDEE_PREVIEW_LIMIT = 12;
    public static final int DEFAULT_DESCRIPTION_PREVIEW_CHARACTERS = 240;
    public static final int MIN_DESCRIPTION_PREVIEW_CHARACTERS = 80;
    public static final int MAX_DESCRIPTION_PREVIEW_CHARACTERS = 2_000;

    public UpcomingEventCardSettings() {
        this(true, DEFAULT_ATTENDEE_PREVIEW_LIMIT, DEFAULT_DESCRIPTION_PREVIEW_CHARACTERS, true);
    }

    @JsonCreator
    public static UpcomingEventCardSettings fromJson(
            @JsonProperty("visible") final Boolean visible,
            @JsonProperty("attendeePreviewLimit") final Integer attendeePreviewLimit,
            @JsonProperty("descriptionPreviewCharacters") final Integer descriptionPreviewCharacters,
            @JsonProperty("showAttachments") final Boolean showAttachments
    ) {
        return new UpcomingEventCardSettings(
                visible == null || visible,
                attendeePreviewLimit == null ? DEFAULT_ATTENDEE_PREVIEW_LIMIT : attendeePreviewLimit,
                descriptionPreviewCharacters == null
                        ? DEFAULT_DESCRIPTION_PREVIEW_CHARACTERS
                        : descriptionPreviewCharacters,
                showAttachments == null || showAttachments
        );
    }

    public UpcomingEventCardSettings {
        attendeePreviewLimit = attendeePreviewLimit <= 0
                ? DEFAULT_ATTENDEE_PREVIEW_LIMIT
                : Math.clamp(attendeePreviewLimit, MIN_ATTENDEE_PREVIEW_LIMIT, MAX_ATTENDEE_PREVIEW_LIMIT);
        descriptionPreviewCharacters = descriptionPreviewCharacters <= 0
                ? DEFAULT_DESCRIPTION_PREVIEW_CHARACTERS
                : Math.clamp(
                        descriptionPreviewCharacters,
                        MIN_DESCRIPTION_PREVIEW_CHARACTERS,
                        MAX_DESCRIPTION_PREVIEW_CHARACTERS
                );
    }
}
