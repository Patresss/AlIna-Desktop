package com.patres.alina.common.settings;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public record ApplicationSettings(
        AssistantSettings assistantSettings,
        UiSettings uiSettings) {

    public ApplicationSettings() {
        this(new AssistantSettings(), new UiSettings());
    }



}
