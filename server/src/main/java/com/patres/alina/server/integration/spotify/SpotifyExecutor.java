package com.patres.alina.server.integration.spotify;

import com.patres.alina.server.integration.alinaintegration.AlinaIntegrationExecutor;
import com.patres.alina.server.integration.exception.CannotCreateIntegrationException;
import com.patres.alina.server.integration.spotify.exception.CannotExecuteSpotifyActionException;
import com.patres.alina.server.integration.spotify.exception.RefreshTokenIsNotFoundException;
import com.patres.alina.server.integration.spotify.pause.SpotifyEmptyFunctionRequest;
import com.patres.alina.server.integration.spotify.pause.SpotifySearchPlaylistFunctionRequest;
import com.patres.alina.server.integration.spotify.pause.SpotifyUriFunctionRequest;
import com.patres.alina.server.integration.spotify.token.SpotifyAuth;
import org.apache.hc.core5.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlaying;
import se.michaelthelin.spotify.model_objects.miscellaneous.CurrentlyPlayingContext;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.requests.data.player.PauseUsersPlaybackRequest;
import se.michaelthelin.spotify.requests.data.search.simplified.SearchPlaylistsRequest;

import java.io.IOException;

public class SpotifyExecutor extends AlinaIntegrationExecutor<SpotifyIntegrationSettings> {


    private static final Logger logger = LoggerFactory.getLogger(SpotifyExecutor.class);
    private final SpotifyApi spotifyApi;
    private final SpotifyAuth spotifyAuth;


    public SpotifyExecutor(final SpotifyIntegrationSettings settings) {
        super(settings);
        try {
            this.spotifyAuth = new SpotifyAuth(settings.getClientId(), settings.getClientSecret(), settings.getRedirectUrl(), settings.getId());
            this.spotifyApi = spotifyAuth.getSpotifyApi();
            String refreshToken = settings.getRefreshToken()
                    .orElseThrow(() -> new RefreshTokenIsNotFoundException(settings.getId()));
            this.spotifyApi.setRefreshToken(refreshToken);
        } catch (Exception e) {
            throw new CannotCreateIntegrationException("Cannot create spotify integration", e);
        }
    }

    public boolean pauseUsersPlayback(SpotifyEmptyFunctionRequest spotifyPauseTrackRequest) {
        try {
            logger.info("Spotify: pausing music");
            spotifyAuth.updateToken(spotifyApi);
            final PauseUsersPlaybackRequest pauseRequest = spotifyApi.pauseUsersPlayback().build();
            pauseRequest.execute();
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new CannotExecuteSpotifyActionException(e);
        }
    }


    public PlaylistSimplified searchPlaylists(SpotifySearchPlaylistFunctionRequest functionRequest) {
        try {
            logger.info("Spotify: searchPlaylists {}", functionRequest.searchQuery());
            spotifyAuth.updateToken(spotifyApi);
            SearchPlaylistsRequest request = spotifyApi.searchPlaylists(functionRequest.searchQuery()).build();
            return request.execute().getItems()[0];
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new CannotExecuteSpotifyActionException(e);
        }
    }

    public boolean startResumeUsersPlayback(SpotifyEmptyFunctionRequest functionRequest) {
        try {
            logger.info("Spotify: startResumeUsersPlayback");
            spotifyAuth.updateToken(spotifyApi);
            final var request = spotifyApi.startResumeUsersPlayback().build();
            request.execute();
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new CannotExecuteSpotifyActionException(e);
        }
    }

    public boolean startResumeUsersPlaybackWithUri(SpotifyUriFunctionRequest functionRequest) {
        try {
            logger.info("Spotify: startResumeUsersPlaybackWithUri: {}", functionRequest.uri());
            spotifyAuth.updateToken(spotifyApi);
            final var request = spotifyApi.startResumeUsersPlayback()
                    .context_uri(functionRequest.uri())
                    .build();
            request.execute();
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new CannotExecuteSpotifyActionException(e);
        }
    }

    public boolean skipUsersPlaybackToNextTrack(SpotifyEmptyFunctionRequest functionRequest) {
        try {
            logger.info("Spotify: skipUsersPlaybackToNextTrack");
            spotifyAuth.updateToken(spotifyApi);
            final var request = spotifyApi.skipUsersPlaybackToNextTrack().build();
            request.execute();
            return true;
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new CannotExecuteSpotifyActionException(e);
        }
    }

    public CurrentlyPlayingContext getInformationAboutUsersCurrentPlayback(SpotifyEmptyFunctionRequest functionRequest) {
        try {
            logger.info("Spotify: getInformationAboutUsersCurrentPlayback");
            spotifyAuth.updateToken(spotifyApi);
            final var request = spotifyApi.getInformationAboutUsersCurrentPlayback().build();
            return request.execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new CannotExecuteSpotifyActionException(e);
        }
    }

    public CurrentlyPlaying getUsersCurrentlyPlayingTrack(SpotifyEmptyFunctionRequest functionRequest) {
        try {
            logger.info("Spotify: getUsersCurrentlyPlayingTrack");
            spotifyAuth.updateToken(spotifyApi);
            final var request = spotifyApi.getUsersCurrentlyPlayingTrack().build();
            return request.execute();
        } catch (IOException | SpotifyWebApiException | ParseException e) {
            throw new CannotExecuteSpotifyActionException(e);
        }
    }

}