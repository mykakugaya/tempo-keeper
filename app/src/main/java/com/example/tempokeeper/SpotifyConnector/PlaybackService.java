package com.example.tempokeeper.SpotifyConnector;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.tempokeeper.Model.Track;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import okhttp3.Call;
import okhttp3.OkHttpClient;

public class PlaybackService {
    // sharedPreferences with API call token
    private final SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    // Spotify variables:
    // Client Id and Redirect URI necessary for connection to remote app playback
    private static final String CLIENT_ID = "790375814d1945ffb9a1303df805d751";
    private static final String REDIRECT_URI = "com.example.tempokeeper://callback/";
    //
    public SpotifyAppRemote mSpotifyAppRemote;
    public ConnectionParams connectionParams;
    public Connector.ConnectionListener connectionListener;

    public Context mContext;

    // OkHTTPClient used to send http requests and read responses
    private Call mCall;
    private OkHttpClient mOkHttpClient = new OkHttpClient();

    // PlaybackService constructor
    public PlaybackService(Context context) {
        sharedPreferences = context.getSharedPreferences("SPOTIFY", 0);
        mContext = context;

        // connect the SpotifyAppRemote using connection parameters
        getSpotifyAppRemote();
    }

    // Connect to the remote player
    private void getSpotifyAppRemote() {
        // Set up connection parameters to include CLIENT_ID and REDIRECT_URI
        connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        connectionListener = new Connector.ConnectionListener() {
            // onConnected is triggered when a connection is made
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                // bind this.mSpotifyAppRemote to the new connection
                mSpotifyAppRemote = spotifyAppRemote;
                Log.d("PlaybackService", "Remote player connected");
            }

            public void onFailure(Throwable throwable) {
                // Something went wrong when attempting to connect, throw error
                Log.e("PlaybackService", throwable.getMessage(), throwable);
            }
        };

        // Make the connection to remote player
        SpotifyAppRemote.connect(mContext, connectionParams, connectionListener);

    }

    // Enable and disable spotifyAppRemote during onStart and onStop, respectively
    public void enableRemote() {
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        SpotifyAppRemote.connect(mContext, connectionParams, connectionListener);
    }

    public void disableRemote() {
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }


    public void getPlaybackProgress() {
        mSpotifyAppRemote.getPlayerApi().getPlayerState()
                /* Spotify PlayerState class:
                 * public PlayerState(Track track,
                   boolean isPaused,
                   float playbackSpeed,
                   long playbackPosition,       // gives us the position of the progressBar
                   PlayerOptions playbackOptions,
                   PlayerRestrictions playbackRestrictions) */
                .setResultCallback(playerState -> {
                    editor = sharedPreferences.edit();
                    long playbackPosition = playerState.playbackPosition;
                    editor.putLong("playbackPosition",playbackPosition);
                    editor.commit();
                })
                .setErrorCallback(throwable -> {
                    Log.e("PlaybackService", throwable.getMessage(), throwable);
                });
    }

    // was the player paused? if true, we call resume when play button is clicked
    public void checkIfPaused() {
        mSpotifyAppRemote.getPlayerApi().getPlayerState()
                /* Spotify PlayerState class:
                 * public PlayerState(Track track,
                   boolean isPaused,
                   float playbackSpeed,
                   long playbackPosition,       // gives us the position of the progressBar
                   PlayerOptions playbackOptions,
                   PlayerRestrictions playbackRestrictions) */
                .setResultCallback(playerState -> {
                    final boolean isPaused = playerState.isPaused;
                    editor = sharedPreferences.edit();
                    editor.putBoolean("paused",isPaused);
                    editor.commit();
                })
                .setErrorCallback(throwable -> {
                    Log.e("PlaybackService", throwable.getMessage(), throwable);
                });
    }


    // Spotify PlayerApi methods //
    // getPlayerApi() gets the Spotify app player so that we can interact remotely with it

    // Playback control methods
    // Play a specific track
    public void play(Track track) {
        String trackUri = "spotify:track:" + track.getId();
        mSpotifyAppRemote.getPlayerApi().play(trackUri);
    }

    // Resume the paused track
    public void resume() {
        mSpotifyAppRemote.getPlayerApi().resume();
    }

    // Pause the player
    public void pause() {
        mSpotifyAppRemote.getPlayerApi().pause();
    }

    // Skip to next track in queue
    public void nextTrack() {
        mSpotifyAppRemote.getPlayerApi().skipNext();
    }

    // Add a track to the playback queue
    public void queue(Track track) {
        String trackUri = "spotify:track:" + track.getId();
        mSpotifyAppRemote.getPlayerApi().queue(trackUri);
    }

    // Get the currently playing/paused track and save to sharedPreferences
    // This way we can keep track of what's playing in Spotify
    public void getPlayingTrack() {
        mSpotifyAppRemote.getPlayerApi().getPlayerState()
                /* Spotify PlayerState class:
                 * public PlayerState(Track track,      // gives us the current track being played
                   boolean isPaused,
                   float playbackSpeed,
                   long playbackPosition,
                   PlayerOptions playbackOptions,
                   PlayerRestrictions playbackRestrictions) */
                .setResultCallback(playerState -> {
                    editor = sharedPreferences.edit();
                    editor.putString("curTrack", playerState.track.name);
                    editor.commit();
                })
                .setErrorCallback(throwable -> {
                    Log.e("PlaybackService", throwable.getMessage(), throwable);
                });
    }

}
