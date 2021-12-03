package com.example.tempokeeper;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtSpotifyUser;
    private TextView txtUserName;
    private TextView txtUserEmail;

    private String spotifyUser;
    private String userName;
    private String userEmail;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        txtSpotifyUser = (TextView) findViewById(R.id.txtUserSpotify);
        txtUserName = (TextView) findViewById(R.id.txtUserName);
        txtUserEmail = (TextView) findViewById(R.id.txtUserEmail);

        sharedPreferences = getSharedPreferences("SPOTIFY",0);

        spotifyUser = sharedPreferences.getString("userId","Spotify User Not Found.");
        txtSpotifyUser.setText(spotifyUser);



    }
}
