package com.example.spotifydemo.SpotifyConnector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import com.example.spotifydemo.PedometerActivity;

// Subscribe to Spotify broadcast notifications using BroadcastReceiver
// Documentation: https://developer.spotify.com/documentation/android/guides/android-media-notifications/
// Sends a broadcast message whenever some change is detected in the remote player
// In order to get broadcast messages, user must turn Device Broadcast Status to ON in the Spotify app’s settings

public class SpotifyBroadcastReceiver extends BroadcastReceiver {
    Context mContext;
    SharedPreferences sharedPreferences;

    // Constructor for broadcastReceiver class
    // Pass in the Activity context so that we can
    public SpotifyBroadcastReceiver(Context context) {
        mContext = context;
        sharedPreferences = mContext.getSharedPreferences("SPOTIFY",0);
    }

    // Event types that will be sent with an intent
    static final class BroadcastTypes {
        static final String SPOTIFY_PACKAGE = "com.spotify.music";
        static final String PLAYBACK_STATE_CHANGED = SPOTIFY_PACKAGE + ".playbackstatechanged";
        static final String QUEUE_CHANGED = SPOTIFY_PACKAGE + ".queuechanged";
        static final String METADATA_CHANGED = SPOTIFY_PACKAGE + ".metadatachanged";
    }

    // The Spotify app can posts sticky media broadcast notifications
    // that can be read by any app on the same Android device.
    // onReceive handles the intents sent with the broadcast action
    @Override
    public void onReceive(Context context, Intent intent) {
        // This is sent with all broadcasts, regardless of type. The value is taken from
        // System.currentTimeMillis(), which you can compare to in order to determine how
        // old the event is.
        long timeSentInMs = intent.getLongExtra("timeSent", 0L);

        String action = intent.getAction();

        // A metadata change intent is sent when a new track starts playing.
        // It uses the intent action com.spotify.music.metadatachanged
        if (action.equals(BroadcastTypes.METADATA_CHANGED)) {
            String trackId = intent.getStringExtra("id");
            String artistName = intent.getStringExtra("artist");
            String albumName = intent.getStringExtra("album");
            String trackName = intent.getStringExtra("track");
            int trackLengthInSec = intent.getIntExtra("length", 0);
            // Set currently playing track info in sharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("curTrack", trackId);
            editor.putString("curArtist", artistName);
            editor.putString("curAlbum", albumName);
            editor.putString("curTrackName", trackName);
            editor.putInt("curDuration", trackLengthInSec*1000);
            editor.commit();
        }
        // A playback state change is sent whenever the user presses play/pause,
        // or when seeking the track position.
        // It uses the intent action com.spotify.music.playbackstatechanged
        else if (action.equals(BroadcastTypes.PLAYBACK_STATE_CHANGED)) {
            boolean playing = intent.getBooleanExtra("playing", false);
            int positionInMs = intent.getIntExtra("playbackPosition", 0);
            // Set playing boolean and track playback position (ms) in sharedPreferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("playing", playing);
            editor.putInt("curPosition", positionInMs);
            editor.commit();
        }
        // A queue change is sent whenever the play queue is changed.
        // It uses the intent action com.spotify.music.queuechanged
        else if (action.equals(BroadcastTypes.QUEUE_CHANGED)) {
            // Sent only as a notification

        }
    }
}
