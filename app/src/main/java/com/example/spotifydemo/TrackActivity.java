package com.example.spotifydemo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spotifydemo.ListAdapters.PlaylistAdapter;
import com.example.spotifydemo.ListAdapters.TrackAdapter;
import com.example.spotifydemo.Model.Playlist;
import com.example.spotifydemo.Model.Track;
import com.example.spotifydemo.Model.User;
import com.example.spotifydemo.SpotifyConnector.PlaylistService;
import com.example.spotifydemo.SpotifyConnector.TrackService;
import com.example.spotifydemo.SpotifyConnector.UserService;

import java.util.ArrayList;

public class TrackActivity extends AppCompatActivity {

    private TextView txtSpotifyUser;
    private TextView txtSelectedPlaylist;
    private Button btnLoadTracks;

    private String userId;  // current logged in user
    private SharedPreferences sharedPref;   // sharedPreferences for id and token

    // track service instance to get playlist tracks
    private TrackService trackService;
    // Id and name of the selected playlist, passed in through the intent
    private String playlistId;
    private String playlistName;

    // adapter to create custom list of tracks
    private RecyclerView rvTracks;
    private RecyclerView.Adapter trackAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracks);

        txtSpotifyUser = (TextView) findViewById(R.id.txtSpotifyUser);
        txtSelectedPlaylist = (TextView) findViewById(R.id.txtSelectedPlaylist);
        btnLoadTracks = (Button) findViewById(R.id.btnLoadTracks);
        rvTracks = (RecyclerView) findViewById(R.id.rvTracks);

        // Get the playlist id passed in through the intent bundle
        Bundle bundle = getIntent().getExtras();
        playlistId = bundle.getString("playlistId");
        // get playlist name as well, display at top of screen
        playlistName = bundle.getString("playlistName");
        txtSelectedPlaylist.setText(playlistName);

        // sharedPref has the token for API calls
        sharedPref = getSharedPreferences("SPOTIFY", 0);
        userId = sharedPref.getString("USERID", "User Not Found");
        txtSpotifyUser.setText("Spotify User: " + userId);

        // Set recycler view to have linear layout and no fixed size
        rvTracks.setHasFixedSize(false);
        layoutManager = new LinearLayoutManager(this);
        rvTracks.setLayoutManager(layoutManager);

        // get an instance of Track Service
        // include sharedPreferences for API call token
        // and the playlistId of the selected playlist
        trackService = new TrackService(sharedPref);

        btnLoadTracks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // set the tracks in the playlist to recyclerView
                setPlaylistTracks();
            }
        });

    }

    // set the selected playlist's tracks in RecyclerView for tracks
    public void setPlaylistTracks() {
        // first get the playlist track items
        // this also gets all track tempos in the playlist
        trackService.getPlaylistTracks(playlistId);

        // create a TrackAdapter instance and set the adapter to display the playlist tracks
        trackAdapter = new TrackAdapter(trackService.getTracks(), this);
        trackAdapter.notifyDataSetChanged();
        rvTracks.setAdapter(trackAdapter);
    }

}
