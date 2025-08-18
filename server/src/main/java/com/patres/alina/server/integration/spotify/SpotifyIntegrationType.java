package com.patres.alina.server.integration.spotify;

import com.patres.alina.common.field.FormField;
import com.patres.alina.common.field.FormFieldType;
import com.patres.alina.common.field.UiForm;
import com.patres.alina.server.integration.Integration;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationFunction;
import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationType;
import com.patres.alina.server.integration.spotify.pause.SpotifyEmptyFunctionRequest;
import com.patres.alina.server.integration.spotify.pause.SpotifySearchPlaylistFunctionRequest;
import com.patres.alina.server.integration.spotify.pause.SpotifyUriFunctionRequest;
import com.patres.alina.server.integration.spotify.token.SpotifyAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.patres.alina.server.integration.spotify.SpotifyIntegrationSettings.*;

@Component
public class SpotifyIntegrationType extends AlinaIntegrationType<SpotifyIntegrationSettings, SpotifyExecutor> {

    private static final Logger logger = LoggerFactory.getLogger(SpotifyIntegrationType.class);

    private static final String CALLBACK_URL = "/integrations/spotify/callback";

    private static final String INTEGRATION_TYPE_NAME = "spotify";
    private static final String INTEGRATION_DEFAULT_NAME_TO_DISPLAY = "Spotify";
    private static final String INTEGRATION_DESCRIPTION = "Manage Spotify: pause track/music";
    private static final String ICON = "fth-music";

    private static final UiForm UI_FORM = new UiForm(List.of(
            new FormField(CLIENT_ID, INTEGRATION_TYPE_NAME + ".clientId.name", "Client ID", null, null, true, FormFieldType.TEXT_FIELD),
            new FormField(CLIENT_SECRET, INTEGRATION_TYPE_NAME + ".clientSecret.name", "Client Secret", null, null, true, FormFieldType.TEXT_FIELD),
            new FormField(REDIRECT_URL, INTEGRATION_TYPE_NAME + ".redirectUrl.name", "Redirect URL", null, null, true, FormFieldType.SERVER_TEXT_FIELD, null, CALLBACK_URL),
            new FormField(REFRESH_TOKE, INTEGRATION_TYPE_NAME + ".refreshToken.name", "Refresh token", INTEGRATION_TYPE_NAME + ".refreshToken.description", "Leave the field empty if you want the application to redirect you to the login page. A token will be generated then.", false, FormFieldType.TEXT_FIELD)
    ));

    public SpotifyIntegrationType() {
        super(INTEGRATION_TYPE_NAME, INTEGRATION_DEFAULT_NAME_TO_DISPLAY, INTEGRATION_DESCRIPTION, ICON, UI_FORM,
                List.of(
                        new AlinaIntegrationFunction<>("pauseTrack", "Pause track/music/playback", SpotifyExecutor::pauseUsersPlayback, SpotifyEmptyFunctionRequest.class),
                        new AlinaIntegrationFunction<>("startResumeUsersPlayback", "Start Resume Users Playback", SpotifyExecutor::startResumeUsersPlayback, SpotifyEmptyFunctionRequest.class),
                        new AlinaIntegrationFunction<>("startResumeUsersPlaybackUri", "Start Resume specific Playback with uri", SpotifyExecutor::startResumeUsersPlaybackWithUri, SpotifyUriFunctionRequest.class),
                        new AlinaIntegrationFunction<>("searchPlaylists", "Search playlist and return uri", SpotifyExecutor::searchPlaylists, SpotifySearchPlaylistFunctionRequest.class),
                        new AlinaIntegrationFunction<>("skipUsersPlaybackToNextTrack", "skip Users Playback/music To Next Track. Next music", SpotifyExecutor::skipUsersPlaybackToNextTrack, SpotifyEmptyFunctionRequest.class),
                        new AlinaIntegrationFunction<>("getUsersCurrentPlayback", "get Information About Users Current Playback/Playlist", SpotifyExecutor::getInformationAboutUsersCurrentPlayback, SpotifyEmptyFunctionRequest.class),
                        new AlinaIntegrationFunction<>("getUsersCurrentlyPlayingTrack", "get Users Currently Playing Track/music", SpotifyExecutor::getUsersCurrentlyPlayingTrack, SpotifyEmptyFunctionRequest.class)
                ));
    }

    @Override
    public SpotifyExecutor createExecutor(SpotifyIntegrationSettings settings) {
        return new SpotifyExecutor(settings);
    }

    @Override
    public SpotifyIntegrationSettings createSettings(final Integration integration) {
        final Map<String, Object> settings = integration.integrationSettings();
        return new SpotifyIntegrationSettings(integration.id(), integration.state(), settings);
    }

    @Override
    public Optional<String> getAuthLink(final Integration integration) {
        final SpotifyIntegrationSettings settings = createSettings(integration);
        if (settings.getRefreshToken().isPresent()) {
            return Optional.empty();
        }
        final SpotifyAuth spotifyAuth = new SpotifyAuth(settings.getClientId(), settings.getClientSecret(), settings.getRedirectUrl(), integration.id());
        return Optional.ofNullable(spotifyAuth.getAuthorizationCodeUri());
    }

    @Override
    public Optional<String> getAuthCode(Integration integration) {
        final SpotifyIntegrationSettings settings = createSettings(integration);
        return settings.getRefreshToken();
    }

    @Override
    public void setAuth(Integration integration, String authCode) {
        final SpotifyIntegrationSettings settings = createSettings(integration);
        final SpotifyAuth spotifyAuth = new SpotifyAuth(settings.getClientId(), settings.getClientSecret(), settings.getRedirectUrl(), integration.id());
        final String refreshToken = spotifyAuth.getRefreshToken(authCode);
        integration.integrationSettings().put(REFRESH_TOKE, refreshToken);
    }
}
