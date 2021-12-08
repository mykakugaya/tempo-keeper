package com.example.tempokeeper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tempokeeper.ListAdapters.PlaylistAdapter;
import com.example.tempokeeper.Model.Playlist;
import com.example.tempokeeper.SpotifyConnector.PlaylistService;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {
    private TextView txtUser;
    private Button btnUserPlaylists;
    private Button btnRun;
    private Button btnBack;

    private String userId;  // current logged in user
    private SharedPreferences sharedPref;   // sharedPreferences for id and token
    private SharedPreferences.Editor editor;

    // playlist service instance to get user playlists
    private PlaylistService playlistService;
    private ArrayList<Playlist> userPlaylists;

    // adapter to create custom list of playlists
    private RecyclerView rvPlaylists;
    private RecyclerView.Adapter playlistAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private PolylineOptions lineOptions;    // necessary for next running activity

    // necessary if we're going back to the route preview activity (from form)
    private String dest;
    private double targetDist;

    // variables necessary if we're going back to the past route preview act (from profile)
    private ArrayList<LatLng> runningRoute;
    private String date;
    private String duration;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlists);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        txtUser = (TextView) findViewById(R.id.txtUser);
        rvPlaylists = (RecyclerView) findViewById(R.id.rvQueue);
        btnUserPlaylists = (Button) findViewById(R.id.btnGetUserPlaylists);
        btnRun = (Button) findViewById(R.id.btnPedometerBpm);
        btnBack = (Button) findViewById(R.id.btnBackToPreview);

        // get the chosen route as a polyline
        lineOptions = getIntent().getParcelableExtra("chosenRoute");

        Bundle bundle = getIntent().getExtras();

        // if we came from RouteForm/RoutePreview activities, save destination for back btn
        try {
            dest = bundle.getString("destination");
            targetDist = bundle.getDouble("targetDist");
        } catch (Exception e) {
            dest = "";
            targetDist = 0;
        }

        // if we came from the ProfileActivity/Past Route Preview, save runningRoute for back btn
        try {
            runningRoute = bundle.getParcelableArrayList("pastRoute");
            date = bundle.getString("pastDate");
            duration = bundle.getString("pastDur");
        } catch (Exception f) {
            runningRoute = null;
            date = "";
            duration = "";
        }

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

        Snackbar snackbar = Snackbar.make(rvPlaylists, "Click \"Load Playlists\" then select a playlist", Snackbar.LENGTH_SHORT);
        snackbar.getView().setBackgroundColor(R.color.colorPrimaryDark);
        snackbar.show();

//        Toast.makeText(PlaylistActivity.this, "Click \"Load Playlists\" then select a playlist", Toast.LENGTH_SHORT).show();

        // if spotify user not found, disable buttons to prevent crashing
        if(userId.equals("User Not Found")) {
            btnUserPlaylists.setEnabled(false);
            btnRun.setEnabled(false);
        } else {
            btnUserPlaylists.setEnabled(true);
            btnRun.setEnabled(false);
        }

        // click the "My Playlists" button to generate a list of user's playlists
        btnUserPlaylists.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setUserPlaylists();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(runningRoute == null) {
                    // if runningRoute is null, we came from the RouteFormActivity preview
                    Intent previewIntent = new Intent(PlaylistActivity.this, RoutePreviewActivity.class);
                    previewIntent.putExtra("destination", dest);
                    previewIntent.putExtra("targetDist",targetDist);
                    startActivity(previewIntent);
                } else {
                    // if runningRoute exists, we came from the ProfileActivity preview
                    Intent profileIntent = new Intent(PlaylistActivity.this, PastRoutePreview.class);
                    profileIntent.putExtra("pastRoute", runningRoute);
                    profileIntent.putExtra("pastDate", date);
                    profileIntent.putExtra("pastDur", duration);
                    startActivity(profileIntent);
                }
            }
        });

        // If a playlist is selected and we want to dynamically queue songs of a similar BPM to our
        // running BPM
        // Whenever a significant change in run pace is detected, the queue of songs will change
        // and a song at the appropriate new tempo will be played
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(sharedPref.getString("curPlaylistName","").equals("")) {
                    // If user has not selected a playlist, toast
                    Snackbar snackbar = Snackbar.make(rvPlaylists, "Please select a playlist first.", Snackbar.LENGTH_SHORT);
                    snackbar.getView().setBackgroundColor(R.color.colorPrimaryDark);
                    snackbar.show();
//                    Toast.makeText(PlaylistActivity.this, "Please select a playlist first.", Toast.LENGTH_SHORT).show();
                } else {
                    // Go to PedometerActivity to start calculating the number of steps taken
                    // and your pace (in steps per minute)
                    Intent runningIntent = new Intent(PlaylistActivity.this, RunningActivity.class);
                    runningIntent.putExtra("chosenRoute", lineOptions);
                    startActivity(runningIntent);
                }
            }
        });

    }

    // set the user's playlists in RecyclerView for playlists
    @SuppressLint("ResourceAsColor")
    public void setUserPlaylists() {
        // first get the user's playlists
        playlistService.getUserPlaylists();

        // create a PlaylistAdapter instance and set the adapter to show playlists
        if(playlistService.getPlaylists()!=null) {
            playlistAdapter = new PlaylistAdapter(playlistService.getPlaylists(), this);
            playlistAdapter.notifyDataSetChanged();
            rvPlaylists.setAdapter(playlistAdapter);

            // enable go to run button
            btnRun.setEnabled(true);

            // Show snackbar with instructions to select a playlist then click Run button
            Snackbar snackbar = Snackbar.make(rvPlaylists, "Select a playlist then click \"Go to Run\"", Snackbar.LENGTH_SHORT);
            snackbar.getView().setBackgroundColor(R.color.colorPrimaryDark);
            snackbar.show();
//            Toast.makeText(PlaylistActivity.this, "Select a playlist then click \"Go to Run\"", Toast.LENGTH_SHORT).show();
        } else {
            Snackbar snackbar = Snackbar.make(rvPlaylists, "No user playlists. Try again.", Snackbar.LENGTH_SHORT);
            snackbar.getView().setBackgroundColor(R.color.colorPrimaryDark);
            snackbar.show();
        }
    }

    // MENU
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    // Handle menu clicks by the user
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // go to create route form
        if (id == R.id.menuRoute) {
            Intent routeIntent = new Intent(PlaylistActivity.this, RouteFormActivity.class);
            startActivity(routeIntent);
            return true;
        }

        // go to user profile
        if (id == R.id.menuProfile) {
            Intent profileIntent = new Intent(PlaylistActivity.this, ProfileActivity.class);
            startActivity(profileIntent);
            return true;
        }

        // sign out of app
        if (id == R.id.menuSignOut) {
            FirebaseAuth.getInstance().signOut();

            // user is now signed out, show toast
            Toast.makeText(getBaseContext(), "Signed out.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(PlaylistActivity.this, LoginActivity.class));
            finish();

            return true;
        }

        // default
        return super.onOptionsItemSelected(item);

    }
}