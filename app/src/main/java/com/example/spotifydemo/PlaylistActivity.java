package com.example.spotifydemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.spotifydemo.ListAdapters.PlaylistAdapter;
import com.example.spotifydemo.Model.Playlist;
import com.example.spotifydemo.Model.User;
import com.example.spotifydemo.SpotifyConnector.PlaylistService;
import com.example.spotifydemo.SpotifyConnector.UserService;

import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {
    private TextView txtUser;
    private Button btnUserPlaylists;
    private Button btnTargetBpm;
    private Button btnDynamicBpm;

    private String userId;  // current logged in user
    private SharedPreferences sharedPref;   // sharedPreferences for id and token
    private SharedPreferences.Editor editor;

    // playlist service instance to get user playlists
    private PlaylistService playlistService;
    private ArrayList<Playlist> userPlaylists;

    // track service instance to get playlist tracks

    // adapter to create custom list of playlists
    private RecyclerView rvPlaylists;
    private RecyclerView.Adapter playlistAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_playlists);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        txtUser = (TextView) findViewById(R.id.txtUser);
        rvPlaylists = (RecyclerView) findViewById(R.id.rvTracks);
        btnUserPlaylists = (Button) findViewById(R.id.btnGetUserPlaylists);
        btnTargetBpm = (Button) findViewById(R.id.btnTargetBpm);
        btnDynamicBpm = (Button) findViewById(R.id.btnPedometerBpm);

        // sharedPref has the token for API calls
        sharedPref = getSharedPreferences("SPOTIFY", 0);
        // get current user's Spotify id
        userId = sharedPref.getString("userId", "User Not Found");
        txtUser.setText("Spotify User: " + userId);

        editor = sharedPref.edit();
        editor.putString("curPlaylistName", "");
        editor.putString("curPlaylistId", "");
        editor.putFloat("curTempo", 0.0f);
        editor.putString("curTrack", "");
        editor.commit();

        // Set recycler view to have linear layout and no fixed size
        rvPlaylists.setHasFixedSize(false);
        layoutManager = new LinearLayoutManager(this);
        rvPlaylists.setLayoutManager(layoutManager);

        // get an instance of playlist service
        playlistService = new PlaylistService(sharedPref);

        // click the "My Playlists" button to generate a list of user's playlists
        btnUserPlaylists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setUserPlaylists();
            }
        });

        // If a playlist is selected/highlighted and we want to select a specific track
        // to base our target BPM on
        // Songs of similar BPM to the chosen song will be queued to play
        btnTargetBpm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(sharedPref.getString("curPlaylistName","").equals("")) {
                    // If user has not selected a playlist, toast
                    Toast.makeText(PlaylistActivity.this, "Please select a playlist first.", Toast.LENGTH_LONG).show();
                } else {
                    // Go to TrackActivity to see the list of tracks for the selected playlist
                    Intent trackIntent = new Intent(PlaylistActivity.this, TrackActivity.class);
                    startActivity(trackIntent);
                }
            }
        });

        // If a playlist is selected and we want to dynamically queue songs of a similar BPM to our
        // running BPM
        // Whenever a significant change in run pace is detected, the queue of songs will change
        // and a song at the appropriate new tempo will be played
        btnDynamicBpm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(sharedPref.getString("curPlaylistName","").equals("")) {
                    // If user has not selected a playlist, toast
                    Toast.makeText(PlaylistActivity.this, "Please select a playlist first.", Toast.LENGTH_LONG).show();
                } else {
                    // Go to PedometerActivity to start calculating the number of steps taken
                    // and your pace (in steps per minute)
                    Intent pedometerIntent = new Intent(PlaylistActivity.this, PedometerActivity.class);
                    startActivity(pedometerIntent);
                }
            }
        });

    }

    // set the user's playlists in RecyclerView for playlists
    public void setUserPlaylists() {
        // first get the user's playlists
        playlistService.getUserPlaylists();
        // create a PlaylistAdapter instance and set the adapter to show playlists
        playlistAdapter = new PlaylistAdapter(playlistService.getPlaylists(), this);
        playlistAdapter.notifyDataSetChanged();
        rvPlaylists.setAdapter(playlistAdapter);
    }
}