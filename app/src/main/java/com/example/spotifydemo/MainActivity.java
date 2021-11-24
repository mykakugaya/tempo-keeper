package com.example.spotifydemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.spotifydemo.Model.User;
import com.example.spotifydemo.SpotifyConnector.PlaylistService;
import com.example.spotifydemo.SpotifyConnector.TrackService;
import com.example.spotifydemo.SpotifyConnector.UserService;

public class MainActivity extends AppCompatActivity {
    private TextView txtUser;
    private String userId;

    private PlaylistService playlistService;
    private User user;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtUser = (TextView) findViewById(R.id.txtUser);

//        reqQueue = Volley.newRequestQueue(this);
        sharedPref = this.getSharedPreferences("SPOTIFY", 0);
        user = UserService.getUser();
        txtUser.setText("Spotify User: " + user.id);

        playlistService = new PlaylistService(sharedPref);
        playlistService.getUserPlaylists();
    }
}