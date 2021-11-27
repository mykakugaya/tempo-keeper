package com.example.spotifydemo;

import static java.lang.Double.parseDouble;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spotifydemo.ListAdapters.PlaylistAdapter;
import com.example.spotifydemo.ListAdapters.TrackAdapter;
import com.example.spotifydemo.Model.Playlist;
import com.example.spotifydemo.Model.Track;
import com.example.spotifydemo.Model.User;
import com.example.spotifydemo.SpotifyConnector.PlaybackService;
import com.example.spotifydemo.SpotifyConnector.PlaylistService;
import com.example.spotifydemo.SpotifyConnector.TrackService;
import com.example.spotifydemo.SpotifyConnector.UserService;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import java.util.ArrayList;

public class TrackActivity extends AppCompatActivity {

    private TextView txtSpotifyUser;
    private TextView txtSelectedPlaylist;
    private Button btnLoadTracks;
    private Button btnFilterTempo;
    private FloatingActionButton btnPlay;
    private FloatingActionButton btnPause;
    private FloatingActionButton btnNext;

    private String userId;  // current logged in user
    private SharedPreferences sharedPref;   // sharedPreferences for id and token

    // track service instance to get playlist tracks
    private TrackService trackService;
    private ArrayList<Track> filteredTracks;
    private String selectedTrackId;

    // Remote playback variables
    private PlaybackService playbackService;
    private Track currentlyPlayingTrack;


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
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_tracks);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        txtSpotifyUser = (TextView) findViewById(R.id.txtSpotifyUser);
        txtSelectedPlaylist = (TextView) findViewById(R.id.txtSelectedPlaylist);
        btnLoadTracks = (Button) findViewById(R.id.btnLoadTracks);
        btnFilterTempo = (Button) findViewById(R.id.btnFilterTempo);
        rvTracks = (RecyclerView) findViewById(R.id.rvTracks);

        btnPlay = (FloatingActionButton) findViewById(R.id.btnPlay);
        btnPause = (FloatingActionButton) findViewById(R.id.btnPause);
        btnNext = (FloatingActionButton) findViewById(R.id.btnNext);

        // sharedPref stores userId, API token, etc.
        sharedPref = getSharedPreferences("SPOTIFY", 0);
        userId = sharedPref.getString("USERID", "User Not Found");
        txtSpotifyUser.setText("Spotify User: " + userId);

        // set the playlist name by getting it from sharedPreferences
        playlistId = sharedPref.getString("PLAYLISTID","");
        playlistName = sharedPref.getString("PLAYLISTNAME","");
        txtSelectedPlaylist.setText(playlistName);

        // Set recycler view to have linear layout and no fixed size
        rvTracks.setHasFixedSize(false);
        layoutManager = new LinearLayoutManager(this);
        rvTracks.setLayoutManager(layoutManager);

        // get an instance of Track Service
        // include sharedPreferences for API call token
        trackService = new TrackService(sharedPref);

        // create playbackService, which controls the remote playback for Spotify
        playbackService = new PlaybackService(TrackActivity.this);

        // Buttons
        // Load tracks of the selected playlist
        btnLoadTracks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // set the tracks in the playlist to recyclerView
                setPlaylistTracks();
            }
        });

        // Filter tracks based on user's selected tempo
        btnFilterTempo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // First, filter the tracks and set to RecyclerView
                filterByTempo();

                // Enable the play, pause, and next buttons for playback
                enablePlaybackControls();
            }
        });


    }

    // triggered when load tracks button is clicked
    // sets the selected playlist's tracks in the RecyclerView
    public void setPlaylistTracks() {
        // first get the playlist track items
        // once the get request completes, this method also gets all track tempos
        trackService.getPlaylistTracks(playlistId);

        // create a TrackAdapter instance and set the adapter to display the playlist tracks
        trackAdapter = new TrackAdapter(trackService.getTracks(), this);
        trackAdapter.notifyDataSetChanged();
        rvTracks.setAdapter(trackAdapter);
    }

    // triggered when filter by tempo button is clicked
    // get the selected tempo and filter the playlist tracks to include only
    // tracks within 10bpm of the selected tempo
    public void filterByTempo() {
        String strTempo = sharedPref.getString("TEMPO","");
        selectedTrackId = sharedPref.getString("TRACK","");
        // if no track is selected, make a toast notifying the user to click on a track
        if(strTempo.equals("")) {
            Toast.makeText(TrackActivity.this, "You must select a track.", Toast.LENGTH_LONG).show();
        } else {    // else, we have clicked a track (aka selected the target tempo)
            double selectedTempo = parseDouble(strTempo);   // the target BPM
            double minTempo = selectedTempo - 10;   // lower bound for BPM
            double maxTempo = selectedTempo + 10;   // upper bound for BPM

            // initialize the new array to hold the tracks filtered by BPM
            filteredTracks = new ArrayList<>();

            // Loop over the current playlist tracks and filter tracks by tempo
            ArrayList<Track> currentTracks = trackService.getTracks();
            int numTracks = trackService.getTracks().size();
            for (int i=0; i<numTracks; i++) {
                double trackTempo = currentTracks.get(i).getTempo();
                // if current track tempo is > min and < max, add to filteredTracks
                if(trackTempo > minTempo && trackTempo < maxTempo) {
                    filteredTracks.add(currentTracks.get(i));
                }

                // if the track's id matches the selected track saved in sharedPreferences,
                // set the currently playing track to the Track item
                if(currentTracks.get(i).getId().equals(selectedTrackId)) {
                    currentlyPlayingTrack = currentTracks.get(i);
                }
            }

            // set the filtered tracks array to the TrackAdapter
            trackAdapter = new TrackAdapter(filteredTracks, this);
            trackAdapter.notifyDataSetChanged();
            // set the adapter to the RecyclerView list
            rvTracks.setAdapter(trackAdapter);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        playbackService.enableRemote();
    }

    @Override
    protected void onStop() {
        super.onStop();
        playbackService.disableRemote();
    }

    // Enable playback controls once tracks have been filtered and a target BPM has been selected
    public void enablePlaybackControls() {
        for(int i=0; i<filteredTracks.size(); i++) {
            playbackService.queue(filteredTracks.get(i));
        }

        // Track play control buttons - enable once tracks have been filtered
        // Play button starts/resumes playing track
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playbackService.play(currentlyPlayingTrack);
            }
        });

        // Pause button pauses track playback
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                playbackService.pause();
            }
        });

        // Next button skips to next song in queue
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playbackService.nextTrack();
            }
        });
    }

}
