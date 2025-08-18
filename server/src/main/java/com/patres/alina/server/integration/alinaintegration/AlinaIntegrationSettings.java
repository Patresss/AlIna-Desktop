package com.patres.alina.server.integration.alinaintegration;

import com.patres.alina.common.card.State;
import com.patres.alina.server.integration.exception.IntegrationKeySettingsNotFoundException;
import com.patres.alina.server.integration.exception.IntegrationSettingsIsInvalidException;

import java.util.Map;
import java.util.Optional;

public abstract class AlinaIntegrationSettings {

    private final String id;
    private final State state;

    protected AlinaIntegrationSettings(String id, State state) {
        this.id = id;
        this.state = state;
    }

    public State getState() {
        return state;
    }

    public String getId() {
        return id;
    }

    public String getSettingsMandatoryStringValue(Map<String, Object> settings, String key) {
        return getSettingsMandatoryValue(settings, key).toString();
    }

    public int getSettingsMandatoryIntValue(Map<String, Object> settings, String key) {
        final String valueAsString = getSettingsMandatoryValue(settings, key).toString();
        try {
            return Integer.parseInt(valueAsString);
        } catch (NumberFormatException e) {
            throw new IntegrationSettingsIsInvalidException(key, "Cannot parse " + valueAsString + " to int");
        }
    }

    public Object getSettingsMandatoryValue(Map<String, Object> settings, String key) {
        Object value = settings.get(key);
        if (value == null || (value instanceof String valueString && valueString.isBlank()) ) {
            throw new IntegrationKeySettingsNotFoundException(key);
        }
        return value;
    }

    public Object getSettingsValue(Map<String, Object> settings, String key) {
        return settings.get(key);
    }

    public String getSettingsStringValue(Map<String, Object> settings, String key) {
        return Optional.ofNullable(getSettingsValue(settings, key))
                .map(Object::toString)
                .filter(it -> !it.isBlank())
                .orElse(null);
    }

}
