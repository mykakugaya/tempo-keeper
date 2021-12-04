package com.example.tempokeeper;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tempokeeper.Model.Run;

import java.util.ArrayList;

public class ProfileActivity extends AppCompatActivity {

    private TextView txtSpotifyUser;
    private TextView txtUserName;
    private TextView txtUserEmail;

    private String spotifyUser;
    private String userName;
    private String userEmail;

    private SharedPreferences sharedPreferences;

    private ArrayList<Run> runHistory;

    private RecyclerView rvHistory;
    private RecyclerView.Adapter runAdapter;
    private RecyclerView.LayoutManager layoutManager;

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

        // Set recycler view to have linear layout and no fixed size
        rvHistory.setHasFixedSize(false);
        layoutManager = new LinearLayoutManager(this);
        rvHistory.setLayoutManager(layoutManager);
    }

//    runAdapter = new RunAdapter(runHistory, this);
//        runAdapter.notifyDataSetChanged();
//        rvHistory.setAdapter(runAdapter);
}
