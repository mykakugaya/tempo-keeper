package com.example.spotifydemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.example.spotifydemo.ListAdapters.PlaylistAdapter;
import com.example.spotifydemo.Model.Playlist;
import com.example.spotifydemo.Model.User;
import com.example.spotifydemo.SpotifyConnector.PlaylistService;
import com.example.spotifydemo.SpotifyConnector.UserService;

import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {
    private TextView txtUser;
    private Button btnUserPlaylists;

    private String userId;  // current logged in user
    private SharedPreferences sharedPref;   // sharedPreferences for id and token

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

        txtUser = (TextView) findViewById(R.id.txtUser);
        rvPlaylists = (RecyclerView) findViewById(R.id.rvTracks);
        btnUserPlaylists = (Button) findViewById(R.id.btnGetUserPlaylists);

        // sharedPref has the token for API calls
        sharedPref = getSharedPreferences("SPOTIFY", 0);
        // get current user's Spotify id
        userId = sharedPref.getString("USERID", "User Not Found");
        txtUser.setText("Spotify User: " + userId);

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