package com.example.spotifydemo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;

import com.example.spotifydemo.SpotifyConnector.PlaybackService;

public class PlaybackActivity extends AppCompatActivity {
    PlaybackService playbackService;
    SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_playback);


    }
}
