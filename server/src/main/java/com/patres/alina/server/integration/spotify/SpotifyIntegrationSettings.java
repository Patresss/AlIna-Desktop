package com.patres.alina.server.integration.spotify;

import com.patres.alina.common.card.State;
import com.patres.alina.server.integration.Integration;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationSettings;

import java.util.Map;
import java.util.Optional;

public class SpotifyIntegrationSettings extends AlinaIntegrationSettings {

    final static String CLIENT_ID = "clientId";
    final static String CLIENT_SECRET = "clientSecret";
    final static String REFRESH_TOKE = "refreshToken";
    final static String REDIRECT_URL = "redirectUrl";

    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String redirectUrl;

    public SpotifyIntegrationSettings(Integration integration) {
        this(integration.id(), integration.state(), integration.integrationSettings());
    }

    public SpotifyIntegrationSettings(String id, State state, Map<String, Object> settings) {
        super(id, state);
        this.clientId = getSettingsMandatoryStringValue(settings, CLIENT_ID);
        this.clientSecret = getSettingsMandatoryStringValue(settings, CLIENT_SECRET);
        this.redirectUrl = getSettingsMandatoryStringValue(settings, REDIRECT_URL);
        this.refreshToken = getSettingsStringValue(settings, REFRESH_TOKE);
    }


    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public Optional<String> getRefreshToken() {
        return Optional.ofNullable(refreshToken);
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }
}