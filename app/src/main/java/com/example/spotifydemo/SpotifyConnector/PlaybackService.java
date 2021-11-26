package com.example.spotifydemo.SpotifyConnector;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.spotifydemo.Model.Track;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import okhttp3.Call;
import okhttp3.OkHttpClient;

public class PlaybackService {
    // sharedPreferences with API call token
    private final SharedPreferences sharedPreferences;

    // Spotify variables:
    // Client Id and Redirect URI necessary for connection to remote app playback
    private static final String CLIENT_ID = "ddb62ea700424470a9ddab081ce13836";
    private static final String REDIRECT_URI = "com.example.spotifydemo://callback/";
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

    private void getSpotifyAppRemote() {
        connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        connectionListener = new Connector.ConnectionListener() {
            // onConnected is triggered when a connection is made
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                mSpotifyAppRemote = spotifyAppRemote;
                Log.d("PlaybackService", "Remote player connected");
            }

            public void onFailure(Throwable throwable) {
                // Something went wrong when attempting to connect, throw error
                Log.e("PlaybackService", throwable.getMessage(), throwable);
            }
        };

        SpotifyAppRemote.connect(mContext, connectionParams, connectionListener);

    }

    public void enableRemote() {
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
        SpotifyAppRemote.connect(mContext, connectionParams, connectionListener);
    }

    public void disableRemote() {
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    // Play a specific track
    public void play(Track track) {
        String trackUri = "spotify:track:" + track.getId();
        // getPlayerApi() gets the Spotify app player so that we can interact remotely with it
        mSpotifyAppRemote.getPlayerApi().play(trackUri);
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




}
