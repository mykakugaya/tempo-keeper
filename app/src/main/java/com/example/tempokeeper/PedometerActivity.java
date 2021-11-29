/*
 *  Pedometer - Android App
 *  Copyright (C) 2009 Levente Bagi
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.example.tempokeeper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tempokeeper.ListAdapters.PlayingTrackAdapter;
import com.example.tempokeeper.Model.Track;
import com.example.tempokeeper.PaceTracker.PedometerSettings;
import com.example.tempokeeper.PaceTracker.StepService;
import com.example.tempokeeper.SpotifyConnector.PlaybackService;
import com.example.tempokeeper.SpotifyConnector.TrackService;

import java.util.ArrayList;
import java.util.Random;


public class PedometerActivity extends AppCompatActivity {
	private static final String TAG = "Pedometer";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private PedometerSettings mPedometerSettings;

    private TextView txtSpotifyUser;
    private TextView txtPlaylistName;

    // Where the current number of steps and pace will be displayed
    private TextView mStepValueView;
    private TextView mPaceValueView;

    private SeekBar sbTrackProgress;
    private TextView txtDuration;
    private TextView txtPlaybackPosition;
    private ImageButton btnPlay;
    private ImageButton btnPause;
    private ImageButton btnNext;
    private Button btnStart;
    private Button btnFinish;

    // PEDOMETER
    // values for the current number of steps and pace
    private int mStepValue;
    private int mPaceValue;

    // Array storing the previous pace values
    private int[] paceValues;    // stores up to 30 pace values at a time
    private int paceValuesIndex;    // index of paceValues array, start at 0
    private double lastPaceAverage;

    private StepService stepService;

    // true when the service is quitting
    private boolean mQuitting; // Set with finish button click
    
    // true when the service is running
    private boolean mIsRunning;

    // SPOTIFY
    // Handlers to run threads
    private Handler trackInfoHandler;
    private Handler progressHandler;

    // Spotify playback
    private String userId;
    private String playlistId;

    // track service instance to get playlist tracks
    private TrackService tempoService;  // used to fetch the tempo of currently playing song
    private TrackService trackService;  // used to store playlist tracks so that we can dynamically filter them
    private ArrayList<Track> playlistTracks;    // all tracks in selected playlist
    private ArrayList<Track> filteredTracks;

    // Remote playback variables
    private PlaybackService playbackService;
    private boolean isPaused;

    // rv and adapter to display a track's info while it is playing
    private ArrayList<Track> curTrackArray;
    private RecyclerView rvTrack;
    private RecyclerView.Adapter trackAdapter;
    private RecyclerView.LayoutManager trackLayout;
    
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "[ACTIVITY] onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pedometer);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        txtSpotifyUser = (TextView) findViewById(R.id.txtPedometerUser);
        txtPlaylistName = (TextView) findViewById(R.id.txtCurPlaylist);
        mStepValueView = (TextView) findViewById(R.id.txtSteps);
        mPaceValueView = (TextView) findViewById(R.id.txtPace);
        rvTrack = (RecyclerView) findViewById(R.id.rvTrack);
        sbTrackProgress = (SeekBar) findViewById(R.id.sbTrackProgress);
        txtPlaybackPosition = (TextView) findViewById(R.id.txtPlaybackPosition);
        txtDuration = (TextView) findViewById(R.id.txtDuration);
        btnPlay = (ImageButton) findViewById(R.id.btnPlay2);
        btnPause = (ImageButton) findViewById(R.id.btnPause2);
        btnNext = (ImageButton) findViewById(R.id.btnNext2);
        btnFinish = (Button) findViewById(R.id.btnFinish);
        btnStart = (Button) findViewById(R.id.btnStart);

        // Set track info recycler view to have linear layout and a fixed size
        rvTrack.setHasFixedSize(true);
        trackLayout = new LinearLayoutManager(this);
        rvTrack.setLayoutManager(trackLayout);

        // Set track progress seek bar minimum to 0
        sbTrackProgress.setMin(0);

        // Get sharedPreferences
        sharedPreferences = getSharedPreferences("SPOTIFY",0);

        // set user id at top of screen
        userId = sharedPreferences.getString("userId","User Not Found");
        txtSpotifyUser.setText("Spotify User: " + userId);

        playlistId = sharedPreferences.getString("curPlaylistId","");
        // set playlist name
        String playlistName = sharedPreferences.getString("curPlaylistName","");
        txtPlaylistName.setText("Playing: "+playlistName);

        // Initialize the step and pace values to 0
        mStepValue = 0;
        mPaceValue = 0;
        paceValues = new int[30];
        paceValuesIndex = 0;
        lastPaceAverage = 0;
        resetValues();

        mQuitting = false;

        // Initialize the trackService and playbackService for Spotify playback
        tempoService = new TrackService(sharedPreferences);
        trackService = new TrackService(sharedPreferences);
        playbackService = new PlaybackService(this);

        Toast.makeText(PedometerActivity.this, "Click \"Start Music\" to enable dynamic playback", Toast.LENGTH_LONG).show();

        // Gets the playlist tracks and starts running two threads
        // One consistently updates the progress bar
        // the other consistently updates the track info
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSpotify();
                Toast.makeText(PedometerActivity.this, "Dynamic playback started!", Toast.LENGTH_LONG).show();
            }
        });

        // Clicked finish button, disable remote player
        // also set mQuitting to true so that it clears the step detector service
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // pause playbackService and disable remote player
                playbackService.pause();
                playbackService.disableRemote();

                // reset pedometer values on screen to 0
                resetValues();
                // Stop the pedometer step service
                unbindStepService();
                stopStepService();
                mQuitting = true;

                // Toast user that run is finished
                Toast.makeText(PedometerActivity.this, "You have finished your run!", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Activity LifeCycle methods
    @Override
    protected void onStart() {
        Log.i(TAG, "[ACTIVITY] onStart");
        super.onStart();
        playbackService.enableRemote(); // PlaybackService
        Toast.makeText(PedometerActivity.this, "Remote Player Connected", Toast.LENGTH_SHORT).show();
    }

    // Start pedometer service onResume
    @Override
    protected void onResume() {
        Log.i(TAG, "[ACTIVITY] onResume");
        super.onResume();

        mPedometerSettings = new PedometerSettings(sharedPreferences);

        // Read from preferences if the service was running on the last onPause
        mIsRunning = mPedometerSettings.isServiceRunning();

        // Start the service if this is considered to be an application start (last onPause was long ago)
        if (!mIsRunning && mPedometerSettings.isNewStart()) {
            startStepService();
            bindStepService();
        } else if (mIsRunning) {
            bindStepService();
        }

        mPedometerSettings.clearServiceRunning();
    }

    // Stop pedometer service onPause
    @Override
    protected void onPause() {
        Log.i(TAG, "[ACTIVITY] onPause");
        if (mIsRunning) {
            unbindStepService();
            stopStepService();
        }
        if (mQuitting) {
            mPedometerSettings.saveServiceRunningWithNullTimestamp(mIsRunning);
        }
        else {
            mPedometerSettings.saveServiceRunningWithTimestamp(mIsRunning);
        }

        super.onPause();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "[ACTIVITY] onStop");
        playbackService.disableRemote();
        super.onStop();
    }

    protected void onDestroy() {
        Log.i(TAG, "[ACTIVITY] onDestroy");
        super.onDestroy();
    }
    
    protected void onRestart() {
        Log.i(TAG, "[ACTIVITY] onRestart");
        super.onRestart();
    }


    // PEDOMETER METHODS FIRST

    // Service Connection for the StepService
    // mConnection is used to bind the service to this context
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            stepService = ((StepService.StepBinder)service).getService();

            stepService.registerCallback(mCallback);
            stepService.reloadSettings();
            
        }

        public void onServiceDisconnected(ComponentName className) {
            stepService = null;
        }
    };
    

    // Starting step service - StepService is declared as a service in the manifest xml
    private void startStepService() {
        if (! mIsRunning) {
            Log.i(TAG, "[SERVICE] Start");
            mIsRunning = true;
            startService(new Intent(PedometerActivity.this,
                    StepService.class));
        }
    }

    // Must bind the service to this context before starting it
    private void bindStepService() {
        Log.i(TAG, "[SERVICE] Bind");
        bindService(new Intent(PedometerActivity.this,
                StepService.class), mConnection, Context.BIND_AUTO_CREATE + Context.BIND_DEBUG_UNBIND);
    }

    private void unbindStepService() {
        Log.i(TAG, "[SERVICE] Unbind");
        unbindService(mConnection);
    }
    
    private void stopStepService() {
        Log.i(TAG, "[SERVICE] Stop");
        if (stepService != null) {
            Log.i(TAG, "[SERVICE] stopService");
            stopService(new Intent(PedometerActivity.this,
                  StepService.class));
        }
        mIsRunning = false;
    }

    // Reset steps and pace values on screen to 0
    private void resetValues() {
        mStepValueView.setText("Steps: 0");
        mPaceValueView.setText("Pace (steps/min): 0");
        editor = sharedPreferences.edit();
        editor.putInt("steps", 0);
        editor.putInt("pace", 0);
        editor.commit();
    }

    // Callback interface for the stepService - when methods are called, use Handler to obtain message
    private StepService.ICallback mCallback = new StepService.ICallback() {
        // sendMessage allows you to enqueue a Message object containing a bundle of data
        /* public Message obtainMessage (int what, int arg1, int arg2) {...}
        * what: returned Message.what
        * arg1: returned Message.arg1 (etc.)
        */
        public void stepsChanged(int value) {
            mHandler.sendMessage(mHandler.obtainMessage(STEPS_MSG, value, 0));
        }
        public void paceChanged(int value) {

            mHandler.sendMessage(mHandler.obtainMessage(PACE_MSG, value, 0));
        }
    };

    // Integers assigned to indicate message for change in steps or pace
    private static final int STEPS_MSG = 1;
    private static final int PACE_MSG = 2;

    /*
    * A Handler allows you to send and process Message and Runnable objects associated with a thread's MessageQueue.
    *  Each Handler instance is associated with a single thread and that thread's message queue.
    *  When you create a new Handler it is bound to a Looper.
    *  It will deliver messages and runnables to that Looper's message queue and execute them on that Looper's thread.
    */
    Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        // bundle of data from sendMessage() is processed by the handleMessage(Message) method
        @Override
        public boolean handleMessage(@NonNull Message message) {
            // message.what was the first parameter in obtainMessage
            // Is this a message for a change in steps or pace?
            switch (message.what) {
                // For both cases, message.arg1 holds the new value for numSteps or pace
                case STEPS_MSG:
                    mStepValue = message.arg1;
                    mStepValueView.setText("Steps: " + mStepValue);
                    break;
                case PACE_MSG:
                    mPaceValue = message.arg1;

                    // Every time a new pace is received, add to array of pace values.
                    // Every 30 pace values, an average is calculated and compared to the previous
                    // average to determine if we need to change the playback BPM
                    addToPaceArray(mPaceValue);
                    if (mPaceValue <= 0) {
                        mPaceValueView.setText("0");
                    }
                    else {
                        mPaceValueView.setText("Pace (steps/min): " + mPaceValue);
                    }
                    break;
                default:
                    break;
            }
            return true;
        }
    });

    // Whenever a new pace value is received by the Handler, we will store this value in the pace array
    public void addToPaceArray(int pace) {
        // Add the new pace value to the paceValues array and increment the index value
        paceValues[paceValuesIndex] = pace;
        paceValuesIndex++;

        // Every 30 pace values that are added, we will calculate the average pace of the array
        if(paceValuesIndex == 29) {
            int sum = 0;
            for (int value : paceValues) {
                sum += value;
            }
            // Calculate the average
            double averagePace = Math.round(sum/30);

            // Compare this new average pace value to the last average pace value
            // If the two values have a difference of more than 10bpm,
            // we will trigger a change in track BPM and change the remotely playing track
            if(averagePace > lastPaceAverage+10 || averagePace < lastPaceAverage-10) {
                // Since there is more than 10bpm change in our average pace, play a new track
                // with a tempo similar to the newly acquired average pace
                dynamicTrackSelection(averagePace);
            }
            // set the average pace to last average pace, set index back to 0 (we will only
            // collect average pace value every 30 pace messages)
            lastPaceAverage = averagePace;
            paceValuesIndex = 0;
        }
    }


    // SPOTIFY METHODS NEXT

    // Start Spotify dynamic queueing by getting selected playlist tracks and starting the Spotify threads
    private void startSpotify() {
        // Get the selected playlist tracks with trackService
        // save tracks in playlistTracks array
        getAllTracks();

        // Enable play, pause, and next buttons
        enablePlaybackControls();

//        playbackService.play(playlistTracks.get(0));

        // Start running the spotify threads to update the currently playing track in the adapter
        // and also update the progress bar to track's current playback position
        // Each thread updates something every second
        setTrackInfoThread();   // update the track adapter showing track information
        setProgressThread();    // update the seek bar showing track playback progress
    }

    // Get all tracks in a playlist
    private void getAllTracks() {
        // first get the playlist track items
        // once the get request completes, this method also gets all track tempos
        trackService.getPlaylistTracks(playlistId);

        // set the playlist tracks to current tracks
        playlistTracks = trackService.getTracks();

    }

    // get the current run pace and filter the playlist tracks to include only
    // tracks within 10bpm of the run pace
    private void dynamicTrackSelection(double curTempo) {
        // if current run pace cannot be detected, curTempo is 0, so send toast
        if(curTempo == 0) {
            Toast.makeText(PedometerActivity.this, "Could not detect run pace", Toast.LENGTH_SHORT).show();
        } else {    // else, we have gotten the current run pace/tempo
            double minTempo = curTempo - 10;   // lower bound for BPM = target BPM - 10
            double maxTempo = curTempo + 10;   // upper bound for BPM = target BPM + 10

            // initialize the new array to hold the tracks filtered by BPM
            filteredTracks = new ArrayList<>();
            getAllTracks();
            // Loop over the current playlist tracks and add appropriate tempo tracks to filteredTracks
            int numTracks = playlistTracks.size();
            for (int i=0; i<numTracks; i++) {
                double trackTempo = playlistTracks.get(i).getTempo();
                // if current track tempo is > min and < max, add to filteredTracks
                if(trackTempo > minTempo && trackTempo < maxTempo) {
                    filteredTracks.add(playlistTracks.get(i));
                }
            }

            // Select a random song from the filtered tracks array, then play song
            Random rand = new Random();
            // get randomIndex: a random integer between 0 and filteredTracks.size()-1
            int randomIndex = rand.nextInt(filteredTracks.size());
            Track nextTrack = filteredTracks.get(randomIndex);
            playbackService.play(nextTrack);

            // Send a toast notifying the user of track change
            Toast.makeText(PedometerActivity.this, "Track changed to: "+nextTrack.getName()+" ("+nextTrack.getTempo()+" bpm)", Toast.LENGTH_SHORT).show();
        }
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
        // instantiate new curTrackArray and add the currently playing track to the array
        curTrackArray = new ArrayList<>();

        // use getPlayingTrack to get the currently playing track name
        playbackService.getPlayingTrack();
        String trackName = sharedPreferences.getString("curTrack","");

        Track newTrack = null;
        // Loop over playlist tracks to find track by name
        // Set the image URL and tempo of the playing track
        for (int i=0; i<playlistTracks.size(); i++) {
            if(playlistTracks.get(i).getName().equals(trackName)) {
                newTrack = playlistTracks.get(i);
                break;
            }
        }

        if(newTrack!=null) {
            curTrackArray.add(newTrack);    // add to array, which we then set to recyclerView

            // set the tracks of TrackService tempoService to this array of one item
            tempoService.setTracks(curTrackArray);

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
                txtDuration.setText(minutes+":"+seconds);
            } else {
                txtDuration.setText(minutes+":0"+seconds);
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
        int progress = Math.round(sharedPreferences.getLong("playbackPosition",0));
        // set progress in seekbar
        sbTrackProgress.setProgress(progress,true);

        // convert playback position to minutes:seconds (e.g. 1:31) so that we can display
        // to left of seek bar
        int minutes = (progress / 1000) / 60;
        int seconds = (progress / 1000) % 60;
        if(seconds>=10) {
            txtPlaybackPosition.setText(minutes+":"+seconds);
        } else {
            txtPlaybackPosition.setText(minutes+":0"+seconds);
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

                String trackName = sharedPreferences.getString("curTrack","");
                double trackTempo = 0;
                // Loop through the available tracks and find the current track by name
                // Then, get the track's tempo for the toast
                // Toast notifying the user of track and tempo change
                for (int i=0; i<playlistTracks.size(); i++) {
                    if(playlistTracks.get(i).getName().equals(trackName)) {
                        trackTempo = playlistTracks.get(i).getTempo();
                        break;
                    }
                }
                Toast.makeText(PedometerActivity.this, "Track changed to: "+trackName+" ("+trackTempo+" bpm)", Toast.LENGTH_SHORT).show();
            }
        });
    }
}