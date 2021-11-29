package com.example.spotifydemo;

import static java.lang.Integer.parseInt;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.spotifydemo.ListAdapters.PlayingTrackAdapter;
import com.example.spotifydemo.ListAdapters.TrackAdapter;
import com.example.spotifydemo.Model.Track;
import com.example.spotifydemo.SpotifyConnector.PlaybackService;
import com.example.spotifydemo.SpotifyConnector.TrackService;

import java.util.ArrayList;


public class TrackActivity extends AppCompatActivity {

    private TextView txtSpotifyUser;
    private TextView txtSelectedPlaylist;
    private EditText edtTargetBpm;
    private Button btnFilterTempo;
    private SeekBar sbTrackProgress;
    private TextView txtTrackDuration;
    private TextView txtTrackPosition;

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

    // stores currently playing track
    private TrackService tempoService;
    private ArrayList<Track> playTrackArray;

    // Handlers for Spotify threads
    private Handler trackInfoHandler;
    private Handler progressHandler;

    // Remote playback variables
    private PlaybackService playbackService;
    private boolean isPaused;

    // Id and name of the selected playlist, passed in through the intent
    private String playlistId;
    private String playlistName;

    // adapter to hold currently playing track
    private RecyclerView rvTrack;
    private RecyclerView.Adapter trackAdapter;
    private RecyclerView.LayoutManager layoutManagerTrack;

    // adapter to create custom list of tracks
    private RecyclerView rvQueue;
    private RecyclerView.Adapter queueAdapter;
    private RecyclerView.LayoutManager layoutManagerQueue;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_tracks);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        txtSpotifyUser = (TextView) findViewById(R.id.txtSpotifyUser);
        txtSelectedPlaylist = (TextView) findViewById(R.id.txtSelectedPlaylist);
        edtTargetBpm = (EditText) findViewById(R.id.edtTargetBpm);
        btnFilterTempo = (Button) findViewById(R.id.btnFilterTempo);
        rvTrack = (RecyclerView) findViewById(R.id.rvTrack);
        sbTrackProgress = (SeekBar) findViewById(R.id.sbTrack);
        txtTrackDuration = (TextView) findViewById(R.id.txtTrackDuration);
        txtTrackPosition = (TextView) findViewById(R.id.txtPosition);
        btnPlay = (ImageButton) findViewById(R.id.btnPlay);
        btnPause = (ImageButton) findViewById(R.id.btnPause);
        btnNext = (ImageButton) findViewById(R.id.btnNext);
        rvQueue = (RecyclerView) findViewById(R.id.rvQueue);

        isPaused = false;

        // sharedPref stores userId, API token, etc.
        sharedPref = getSharedPreferences("SPOTIFY", 0);
        userId = sharedPref.getString("userId", "User Not Found");
        txtSpotifyUser.setText("Spotify User: " + userId);

        // set the playlist name by getting it from sharedPreferences
        playlistId = sharedPref.getString("curPlaylistId","");
        playlistName = sharedPref.getString("curPlaylistName","");
        txtSelectedPlaylist.setText("Playing: "+playlistName);

        // Set playing track info recycler view to have linear layout and a fixed size
        rvTrack.setHasFixedSize(true);
        layoutManagerTrack = new LinearLayoutManager(this);
        rvTrack.setLayoutManager(layoutManagerTrack);

        // Set recycler view to have linear layout and no fixed size
        rvQueue.setHasFixedSize(false);
        layoutManagerQueue = new LinearLayoutManager(this);
        rvQueue.setLayoutManager(layoutManagerQueue);

        // get an instance of Track Service
        // include sharedPreferences for API call token
        trackService = new TrackService(sharedPref);
        // tempoService is used to store currently playing track
        tempoService = new TrackService(sharedPref);

        // create playbackService, which controls the remote playback for Spotify
        playbackService = new PlaybackService(TrackActivity.this);

        // Set track progress seek bar minimum to 0
        sbTrackProgress.setMin(0);

        // no selected track yet
        editor = sharedPref.edit();
        editor.putString("curTrack","");
        editor.commit();

        // Set the selected playlist tracks in queue
        // Also enables playback controls (play, pause, next buttons)
        // Starts the two spotify threads needed to set currently playing track info
        // and to update the seekbar
        startSpotify();

        // Toast to user telling to click Load Tracks button
        Toast.makeText(TrackActivity.this, "Enter a target BPM then click \"Filter Tracks\"", Toast.LENGTH_LONG).show();

        // Filter tracks based on user's selected tempo
        btnFilterTempo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // If user has selected a track/tempo,
                // filter the tracks by tempo and set to RecyclerView
                if(sharedPref.getFloat("curTempo",0) != 0) {
                    filterByTempo();

                    // Toast that the playback controls have been enabled
                    Toast.makeText(TrackActivity.this, "Songs have been filtered and queued! Starting playback", Toast.LENGTH_LONG).show();
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

        // create a queueAdapter instance and set the adapter to display the playlist tracks
        queueAdapter = new TrackAdapter(trackService.getTracks(), this);
        queueAdapter.notifyDataSetChanged();
        rvQueue.setAdapter(queueAdapter);

        // set the playlist tracks to current tracks
        currentTracks = trackService.getTracks();
    }

    // triggered when filter by tempo button is clicked
    // get the selected tempo and filter the playlist tracks to include only
    // tracks within 10bpm of the selected tempo
    public void filterByTempo() {
        int selectedTempo = parseInt(edtTargetBpm.getText().toString()); // target BPM

        int minTempo = selectedTempo - 10;   // lower bound for BPM = target BPM - 10
        int maxTempo = selectedTempo + 10;   // upper bound for BPM = target BPM + 10

        // initialize the new array to hold the tracks filtered by BPM
        filteredTracks = new ArrayList<>();

        // Loop over the current playlist tracks and filter tracks by tempo
        int numTracks = currentTracks.size();
        for (int i=0; i<numTracks; i++) {
            double trackTempo = currentTracks.get(i).getTempo();
            // if current track tempo is > min and < max, add to filteredTracks
            if(trackTempo > minTempo && trackTempo < maxTempo) {
                filteredTracks.add(currentTracks.get(i));
            }
        }

        // set the filtered tracks array to the queueAdapter
        queueAdapter = new TrackAdapter(filteredTracks, this);
        queueAdapter.notifyDataSetChanged();
        // set the adapter to the RecyclerView list
        rvQueue.setAdapter(queueAdapter);
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

    private void startSpotify() {
        // Get the selected playlist tracks with trackService
        // save tracks in playlistTracks array
        setPlaylistTracks();

        // Enable play, pause, and next buttons
        enablePlaybackControls();

        // Start running the spotify threads to update the currently playing track in the adapter
        // and also update the progress bar to track's current playback position
        // Each thread updates something every second
        setTrackInfoThread();   // update the track adapter showing track information
        setProgressThread();    // update the seek bar showing track playback progress
    }

    // SPOTIFY THREADS
    // Run the setTrackInfo Thread - updates the track info every second
    private void setTrackInfoThread() {
        trackInfoHandler = new Handler();
        Runnable r = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void run() {
                setCurTrackInfo();
                trackInfoHandler.postDelayed(this, 1000);
            }
        };
        trackInfoHandler.postDelayed(r, 1000);
    }

    // Called every second to update the currently playing track info
    public void setCurTrackInfo() {
        // instantiate new playTrackArray and add the currently playing track to the array
        playTrackArray = new ArrayList<>();

        // use getPlayingTrack to get the currently playing track name
        playbackService.getPlayingTrack();
        String trackName = sharedPref.getString("curTrack","");

        Track newTrack = null;
        // Loop over playlist tracks to find track by name
        // Set the image URL and tempo of the playing track
        for (int i=0; i<currentTracks.size(); i++) {
            if(currentTracks.get(i).getName().equals(trackName)) {
                newTrack = currentTracks.get(i);
                break;
            }
        }

        if(newTrack!=null) {
            playTrackArray.add(newTrack);    // add to array, which we then set to recyclerView

            // set the tracks of TrackService tempoService to this array of one item
            tempoService.setTracks(playTrackArray);

            // set the current track array to the PlayingTrackAdapter
            trackAdapter = new PlayingTrackAdapter(tempoService.getTracks(), this);
            trackAdapter.notifyDataSetChanged();
            // set the adapter to the RecyclerView list
            rvTrack.setAdapter(trackAdapter);

            // set the track progress seek bar max to the duration of the track
            int trackDuration = newTrack.getDuration(); // this is the duration in ms
            sbTrackProgress.setMax(trackDuration);

            // convert duration to minutes:seconds (e.g. 1:31) so that we can display
            // to right of seekbar
            int minutes = (trackDuration / 1000) / 60;
            int seconds = (trackDuration / 1000) % 60;
            if(seconds>=10) {
                txtTrackDuration.setText(minutes+":"+seconds);
            } else {
                txtTrackDuration.setText(minutes+":0"+seconds);
            }
        }
    }

    // Run the setProgress Thread - updates the track progress bar every second
    private void setProgressThread() {
        progressHandler = new Handler();
        Runnable r = new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void run() {
                updatePlaybackProgress();
                progressHandler.postDelayed(this, 1000);
            }
        };
        progressHandler.postDelayed(r, 1000);
    }

    // Called every second to update the progress bar of the currently playing track
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void updatePlaybackProgress() {
        // Get the current player's playback position (progress of track being played)
        playbackService.getPlaybackProgress();  // saves playbackPosition in sharedPreferences
        // progress = playback position in ms
        int progress = Math.round(sharedPref.getLong("playbackPosition",0));
        // set progress in seekbar
        sbTrackProgress.setProgress(progress,true);

        // convert playback position to minutes:seconds (e.g. 1:31) so that we can display
        // to left of seek bar
        int minutes = (progress / 1000) / 60;
        int seconds = (progress / 1000) % 60;
        if(seconds>=10) {
            txtTrackPosition.setText(minutes+":"+seconds);
        } else {
            txtTrackPosition.setText(minutes+":0"+seconds);
        }
    }

    // SPOTIFY PLAY CONTROLS
    // Enable playback controls once playlist tracks have been fetched
    public void enablePlaybackControls() {
        // Play button resumes playing track.
        // This button will always be used to resume playback (not play track from the beginning),
        // since that is handled by dynamicTrackSelection
        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // if playback has been paused, now resuming playback
                if(isPaused) {
                    isPaused = false;
                    playbackService.resume();
                }
            }
        });

        // Pause button pauses track playback
        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isPaused = true;
                // getPlayingTrack gets the current PlayerState of the PlayerApi
                // and saves the currently playing track name in sharedPreferences under "curTrack"
                playbackService.getPlayingTrack();  // save the current track to sharedPref
                playbackService.pause();
            }
        });

        // Next button skips to next song in queue
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to next track and set new current track
                playbackService.nextTrack();
                // getPlayingTrack saves playing track name in sharedPref "curTrack"
                playbackService.getPlayingTrack();

                String trackName = sharedPref.getString("curTrack","");
                double trackTempo = 0;
                // Loop through the available tracks and find the current track by name
                // Then, get the track's tempo for the toast
                // Toast notifying the user of track and tempo change
                for (int i=0; i<currentTracks.size(); i++) {
                    if(currentTracks.get(i).getName().equals(trackName)) {
                        trackTempo = currentTracks.get(i).getTempo();
                        break;
                    }
                }
                Toast.makeText(TrackActivity.this, "Track changed to: "+trackName+" ("+trackTempo+" bpm)", Toast.LENGTH_LONG).show();
            }
        });
    }

}
