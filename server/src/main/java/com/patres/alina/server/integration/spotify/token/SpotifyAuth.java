package com.patres.alina.server.integration.spotify.token;

import com.patres.alina.server.integration.spotify.exception.CannotRetrieveRefreshTokenException;
import org.apache.hc.core5.http.ParseException;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.io.IOException;
import java.net.URI;

public class SpotifyAuth {

    private final String clientId;
    private final String clientSecret;
    private final String integrationId;
    private final URI redirectUri;

    public SpotifyAuth(String clientId, String clientSecret, String redirectUrl, String integrationId) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.integrationId = integrationId;
        this.redirectUri = SpotifyHttpManager.makeUri(redirectUrl);
    }

    public String getAuthorizationCodeUri() {
        final SpotifyApi spotifyApi = getSpotifyApi();
        final AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyApi.authorizationCodeUri()
                .scope(
                        AuthorizationScope.USER_READ_PLAYBACK_STATE,
                        AuthorizationScope.USER_MODIFY_PLAYBACK_STATE,
                        AuthorizationScope.USER_READ_CURRENTLY_PLAYING,
                        AuthorizationScope.PLAYLIST_READ_PRIVATE
                )
                .show_dialog(true)
                .state(integrationId)
                .build();
        return authorizationCodeUriRequest.execute().toString();
    }

    public SpotifyApi getSpotifyApi() {
        return new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(redirectUri)
                .build();
    }

    public String getRefreshToken(final String code) {
        try {
            final SpotifyApi spotifyApi = getSpotifyApi();
            final AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
            final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
            return authorizationCodeCredentials.getRefreshToken();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new CannotRetrieveRefreshTokenException(e);
        }
    }

    public void updateToken(SpotifyApi spotifyApi) throws IOException, ParseException, SpotifyWebApiException {
        final AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyApi.authorizationCodeRefresh().build();
        final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();
        spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
    }

}