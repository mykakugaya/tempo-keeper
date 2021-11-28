package com.example.spotifydemo;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spotifydemo.ListAdapters.TrackAdapter;
import com.example.spotifydemo.Model.Track;
import com.example.spotifydemo.SpotifyConnector.PlaybackService;
import com.example.spotifydemo.SpotifyConnector.TrackService;

import java.io.IOException;
import java.util.ArrayList;


public class TrackActivity extends AppCompatActivity {

    private TextView txtSpotifyUser;
    private TextView txtSelectedPlaylist;
    private Button btnLoadTracks;
    private Button btnFilterTempo;

    // Playback control buttons
    private ImageButton btnPlay;
    private ImageButton btnPause;
    private ImageButton btnNext;

    private String userId;  // current logged in user
    private SharedPreferences sharedPref;   // sharedPreferences for id and token
    private SharedPreferences.Editor editor;

    // track service instance to get playlist tracks
    private TrackService trackService;
    private ArrayList<Track> currentTracks;
    private ArrayList<Track> filteredTracks;
    private String selectedTrackName;

    // Remote playback variables
    private PlaybackService playbackService;
    private boolean isPaused;
    private Track prevTrack;
    private Track curTrack;
    private Track nextTrack;


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

        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnPause = (ImageButton) findViewById(R.id.btnPause);
        btnNext = (ImageButton) findViewById(R.id.btnNext);

        isPaused = false;

        // sharedPref stores userId, API token, etc.
        sharedPref = getSharedPreferences("SPOTIFY", 0);
        userId = sharedPref.getString("userId", "User Not Found");
        txtSpotifyUser.setText("Spotify User: " + userId);

        // set the playlist name by getting it from sharedPreferences
        playlistId = sharedPref.getString("curPlaylistId","");
        playlistName = sharedPref.getString("curPlaylistName","");
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

        // Toast to user telling to click Load Tracks button
        Toast.makeText(TrackActivity.this, "Click \"Load Tracks\" to view playlist tracks", Toast.LENGTH_LONG).show();

        // Buttons
        // Load tracks of the selected playlist
        btnLoadTracks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // no selected track yet
                editor = sharedPref.edit();
                editor.putString("curTrack","");
                editor.commit();

                // set the tracks in the playlist to recyclerView
                setPlaylistTracks();

                // Instruct the user to select a track through a toast
                Toast.makeText(TrackActivity.this, "Select a track and click \"Filter by Tempo\"", Toast.LENGTH_LONG).show();
            }
        });

        // Filter tracks based on user's selected tempo
        btnFilterTempo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If user has selected a track/tempo,
                // filter the tracks by tempo and set to RecyclerView
                if(sharedPref.getFloat("curTempo",0) != 0) {
                    filterByTempo();

                    // Toast that the playback controls have been enabled
                    Toast.makeText(TrackActivity.this, "Songs have been filtered and queued! Click the play button.", Toast.LENGTH_LONG).show();

                    // Enable the play, pause, and next buttons for playback
                    enablePlaybackControls();
                }
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

        // set the playlist tracks to current tracks
        currentTracks = trackService.getTracks();
    }

    // triggered when filter by tempo button is clicked
    // get the selected tempo and filter the playlist tracks to include only
    // tracks within 10bpm of the selected tempo
    public void filterByTempo() {
        double selectedTempo = (double) sharedPref.getFloat("curTempo",0); // target BPM

        selectedTrackName = sharedPref.getString("curTrack","");     // the selected track to filter by

        // if no track is selected, make a toast notifying the user to click on a track
        if(selectedTempo == 0) {
            Toast.makeText(TrackActivity.this, "Tempo not selected.", Toast.LENGTH_LONG).show();
        } else {    // else, we have clicked a track (aka selected the target tempo)
            double minTempo = selectedTempo - 10;   // lower bound for BPM = target BPM - 10
            double maxTempo = selectedTempo + 10;   // upper bound for BPM = target BPM + 10

            // initialize the new array to hold the tracks filtered by BPM
            filteredTracks = new ArrayList<>();

            // Loop over the current playlist tracks and filter tracks by tempo
            int numTracks = trackService.getTracks().size();
            for (int i=0; i<numTracks; i++) {
                double trackTempo = currentTracks.get(i).getTempo();
                // if current track tempo is > min and < max, add to filteredTracks
                if(trackTempo > minTempo && trackTempo < maxTempo) {
                    filteredTracks.add(currentTracks.get(i));
                }

                // if the track's id matches the selected track saved in sharedPreferences,
                // set the currently playing track to the Track item
                if(currentTracks.get(i).getName().equals(selectedTrackName)) {
                    setCurTrack(currentTracks.get(i));
                }
            }

            // set the filtered tracks array to the TrackAdapter
            trackAdapter = new TrackAdapter(filteredTracks, this);
            trackAdapter.notifyDataSetChanged();
            // set the adapter to the RecyclerView list
            rvTracks.setAdapter(trackAdapter);

        }
    }

    // Enable the remote player when starting
    @Override
    protected void onStart() {
        super.onStart();
        playbackService.enableRemote(); // PlaybackService
    }

    // Disable the remote player when stopping
    @Override
    protected void onStop() {
        super.onStop();
        playbackService.disableRemote();
    }

    // set the current track whenever we change the playing track
    public void setCurTrack(Track track) {
        curTrack = track;
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
                // if previous track = current track, we have paused and now resuming playback
                if(isPaused) {
                    isPaused = false;
                    playbackService.resume();
                } else {
                    playbackService.play(curTrack);
                    playbackService.getPlayingTrack();
                }

            }
        });

        // Pause button pauses track playback
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isPaused = true;
                playbackService.getPlayingTrack();
                playbackService.pause();
            }
        });

        // Next button skips to next song in queue
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to next track and set new current track
                playbackService.nextTrack();
                playbackService.getPlayingTrack();
            }
        });
    }

}
