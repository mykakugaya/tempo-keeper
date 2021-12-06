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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tempokeeper.ListAdapters.PlayingTrackAdapter;
import com.example.tempokeeper.Model.Track;
import com.example.tempokeeper.PaceTracker.PedometerSettings;
import com.example.tempokeeper.PaceTracker.StepService;
import com.example.tempokeeper.SpotifyConnector.PlaybackService;
import com.example.tempokeeper.SpotifyConnector.TrackService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;


public class RunningActivity extends AppCompatActivity implements OnMapReadyCallback {
	private static final String TAG = "Pedometer";
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private PedometerSettings mPedometerSettings;

    private SeekBar sbTrackProgress;
    private TextView txtDuration;
    private TextView txtPlaybackPosition;
    private ImageButton btnPlay;
    private ImageButton btnPause;
    private ImageButton btnNext;
    private Button btnStart;
    private Button btnFinish;

    private Button btnStats;
    private TextView txtDistance;

    // PEDOMETER
    // values for the current number of steps and pace
    private int mStepValue;
    private int mPaceValue;

    // Array storing the previous pace values
    private int[] paceValues;    // stores up to 30 pace values at a time
    private int paceValuesIndex;    // index of paceValues array, start at 0
    private double lastPaceAverage;

    private StepService stepService;

    // true when the step service is quitting
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
    private String playlistName;

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


    // GOOGLE MAPS
    private GoogleMap mMap;
    private PolylineOptions lineOptions;

    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;

    ArrayList<LatLng> runningRoute = new ArrayList<LatLng>();
    boolean running;

    // FIREBASE
    private final FirebaseDatabase usersDB = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = usersDB.getReference().child("Users");

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String firebaseUser = mAuth.getCurrentUser().getUid();

    // used to save routes to user database
    private String[] routeLst;
    private String[] timeLst;
    private String[] dateLst;
    private String[] distLst;
    private String[] avgSpdLst;
    ArrayList points = null;

    // Start and Finish times of the run
    private long startTime;
    private long finishTime;
    private String duration;
    private int duration_ms;
    private String startDateTime;

    Double totalDistance = 0.0;


    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "[ACTIVITY] onCreate");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_running);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        rvTrack = (RecyclerView) findViewById(R.id.rvTrack);
        sbTrackProgress = (SeekBar) findViewById(R.id.sbTrackProgress);
        txtPlaybackPosition = (TextView) findViewById(R.id.txtPlaybackPosition);
        txtDuration = (TextView) findViewById(R.id.txtDuration);
        btnPlay = (ImageButton) findViewById(R.id.btnPlay2);
        btnPause = (ImageButton) findViewById(R.id.btnPause2);
        btnNext = (ImageButton) findViewById(R.id.btnNext2);
        btnFinish = (Button) findViewById(R.id.btnFinish);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStats = (Button) findViewById(R.id.btnStats);
        txtDistance = (TextView) findViewById(R.id.txtRunningDist);

        btnStart.setEnabled(false);
        btnFinish.setEnabled(false);
        btnStats.setEnabled(false);

        // Set track info recycler view to have linear layout and a fixed size
        rvTrack.setHasFixedSize(false);
        trackLayout = new LinearLayoutManager(this);
        rvTrack.setLayoutManager(trackLayout);

        // Set track progress seek bar minimum to 0
        sbTrackProgress.setMin(0);

        // Get sharedPreferences
        sharedPreferences = getSharedPreferences("SPOTIFY",0);

        // set user id at top of screen
        userId = sharedPreferences.getString("userId","User Not Found");

        // set playlist id and name
        playlistId = sharedPreferences.getString("curPlaylistId","");
        playlistName = sharedPreferences.getString("curPlaylistName","");

        // Initialize the step and pace values to 0
        mStepValue = 0;
        mPaceValue = 0;
        paceValues = new int[30];   // stores the last 30 pace readings in an array
        paceValuesIndex = 0;    // pace values index ranges from 0-29
        lastPaceAverage = 0;
        resetValues();

        mQuitting = false;
        mIsRunning = true;

        // Initialize the trackService and playbackService for Spotify playback
        tempoService = new TrackService(sharedPreferences);
        trackService = new TrackService(sharedPreferences);
        playbackService = new PlaybackService(this);

        Toast.makeText(RunningActivity.this, "Click \"Start Run\" to start your run!", Toast.LENGTH_LONG).show();

        // GET POLYLINE OPTIONS FROM BUNDLE
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(500);
        locationRequest.setMaxWaitTime(1000);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // This is the map polyline passed in to be displayed on the run map
        lineOptions = getIntent().getParcelableExtra("chosenRoute");

        // Connect map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map2);
        mapFragment.getMapAsync(this);


        // BUTTON ONCLICK

        // Finish button clicked, disable remote player
        // also set mQuitting to true so that it clears the step detector service
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                btnFinish.setEnabled(false);
                btnStats.setEnabled(true);
                // get the finish time
                finishTime = System.currentTimeMillis();
                duration_ms = Math.round(finishTime - startTime); // this is in ms

                if(duration_ms < 3600000) { // duration is less than an hr
                    // convert ms to mins:secs
                    int minutes = (duration_ms / 1000) / 60;
                    int seconds = (duration_ms / 1000) % 60;
                    if (seconds >= 10) {
                        duration = minutes + ":" + seconds;
                    } else {
                        duration = minutes + ":0" + seconds;
                    }
                } else {    // duration exceeds an hour
                    // convert ms to hrs:mins:secs
                    int seconds = (int) (duration_ms / 1000) % 60 ;
                    int minutes = (int) ((duration_ms / (1000*60)) % 60);
                    int hours = (int) ((duration_ms / (1000*60*60)) % 24);
                    String strMins = String.valueOf(minutes);
                    String strSecs = String.valueOf(seconds);
                    if (seconds < 10) {
                        strSecs = "0"+strSecs;
                    }
                    if(minutes < 10) {
                        strMins = "0"+strMins;
                    }
                    duration = hours+":"+strMins+":"+strSecs;
                }

                // pause playbackService and disable remote player
                playbackService.pause();

                // saves route to database
                saveRouteToDb();

                // reset pedometer values to 0
                resetValues();

                // Stop the pedometer step service
                unbindStepService();
                stopStepService();
                mQuitting = true;

                // Toast user that run is finished
                Toast.makeText(RunningActivity.this, "You have finished your run!", Toast.LENGTH_SHORT).show();

            }
        });

        // button to go to Stats for this run
        btnStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent finishIntent = new Intent(RunningActivity.this, RunStatsActivity.class);
                finishIntent.putExtra("runningRoute",runningRoute);
//                finishIntent.putExtra("startDateTime",startDateTime);
                startActivity(finishIntent);
            }
        });
    }

    // Activity LifeCycle methods
    @Override
    protected void onStart() {
        Log.i(TAG, "[ACTIVITY] onStart");
        super.onStart();
        playbackService.enableRemote(); // PlaybackService
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        } else {
            // you need to request permissions...
        }
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
            // disable step service
            unbindStepService();
//            stopStepService();
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
        // disable spotify threads
        trackInfoHandler.removeCallbacksAndMessages(null);
        progressHandler.removeCallbacksAndMessages(null);
        // disable remote player
        playbackService.disableRemote();
        // stop updating location
        stopLocationUpdates();
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

    // GOOGLE MAPS METHODS FIRST
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        //enables device location to be shown on the map
        enableMyLocation();
        Polyline polyline = mMap.addPolyline((lineOptions));
        btnStart.setEnabled(true);
        // Gets the playlist tracks and starts running two threads
        // One consistently updates the progress bar
        // the other consistently updates the track info
        btnStart.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                running = true;
                // once we start run, enable finish button and disable start + back to music buttons
                btnStart.setEnabled(false);
                btnFinish.setEnabled(true);

                // get the start time in ms (used to calculate run duration)
                startTime = System.currentTimeMillis();
                // also get the date of the run
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                startDateTime = dtf.format(LocalDateTime.now());
                // enable spotify dynamic playback and controls
                startSpotify();
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            ActivityCompat.requestPermissions(this, new String[]
                            {Manifest.permission.ACCESS_FINE_LOCATION},
                    9000);
        }
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            Log.d(TAG, "onLocationResult: " + locationResult.getLastLocation());
            if (mMap != null) {
                setUserLocationMarker(locationResult.getLastLocation());
            }
        }
    };

    private void setUserLocationMarker(Location location) {

        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (running == true){
            if (runningRoute.size()>1){
                int lastIndex = runningRoute.size()-1;
                totalDistance += distance(latLng.latitude, runningRoute.get(lastIndex).latitude, latLng.longitude, runningRoute.get(lastIndex).longitude);
                totalDistance = round(totalDistance, 3);
                txtDistance.setText(totalDistance.toString());
            }
            // stores the Lat/Lng values for each point along route ran
            runningRoute.add(latLng);
        }
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)      // Sets the center of the map to Mountain View
                .zoom(19)                   // Sets the zoom
                .bearing(location.getBearing())                // Sets the orientation of the camera to east
                .tilt(50)                   // Sets the tilt of the camera to 30 degrees
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }

    // add the completed route to firebase once the Finish Run button is clicked
    public void saveRouteToDb(){
        DatabaseReference myRef = dbRef.child(firebaseUser);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    String temp = String.valueOf(Long.valueOf(snapshot.child("Routes").getChildrenCount()));
                    if (Integer.valueOf(temp)>0) {  // if past runs exist, append this run
                        String[] routesArray = new String[Integer.valueOf(temp) + 1];
                        String[] durationArray = new String[Integer.valueOf(temp) + 1];
                        String[] dateArray = new String[Integer.valueOf(temp) + 1];
                        String[] distanceArray = new String[Integer.valueOf(temp) + 1];
                        String[] avgSpeedArray = new String[Integer.valueOf(temp) + 1];
                        routeLst = routesArray;
                        timeLst = durationArray;
                        dateLst = dateArray;
                        distLst = distanceArray;
                        avgSpdLst = avgSpeedArray;

                        // For each run, save the date, duration, distance, average speed
                        for (int i = 0; i < Integer.valueOf(temp); i++) {
                            String x = String.valueOf(i);
                            routeLst[i] = String.valueOf(snapshot.child("Routes").child(x).getValue());
                            timeLst[i] = String.valueOf(snapshot.child("Time").child(x).getValue());
                            dateLst[i] = String.valueOf(snapshot.child("Date").child(x).getValue());
                            distLst[i] = String.valueOf(snapshot.child("Distance").child(x).getValue());
                            avgSpdLst[i] = String.valueOf(snapshot.child("Average").child(x).getValue());
                        }
                        Log.d("Running Route",""+runningRoute.size());

                        // set new values to firebase database
                        routeLst[routeLst.length-1] = String.valueOf(runningRoute);
                        dbRef.child(firebaseUser).child("Routes").setValue(Arrays.asList(routeLst));

                        timeLst[timeLst.length-1] = duration;
                        dbRef.child(firebaseUser).child("Time").setValue(Arrays.asList(timeLst));

                        dateLst[dateLst.length-1] = startDateTime;
                        dbRef.child(firebaseUser).child("Date").setValue(Arrays.asList(dateLst));

                        distLst[distLst.length-1] = String.valueOf(totalDistance);
                        dbRef.child(firebaseUser).child("Distance").setValue(Arrays.asList(distLst));

                        avgSpdLst[avgSpdLst.length-1] = String.valueOf(Math.round(100*totalDistance/(double)(duration_ms/3600))/100);
                        dbRef.child(firebaseUser).child("Average").setValue(Arrays.asList(avgSpdLst));

                    }
                    else{   // else, this is the first run completed
                        dbRef.child(firebaseUser).child("Routes").setValue(Arrays.asList(String.valueOf(runningRoute)));
                        dbRef.child(firebaseUser).child("Time").setValue(Arrays.asList(duration));
                        dbRef.child(firebaseUser).child("Date").setValue(Arrays.asList(startDateTime));
                        dbRef.child(firebaseUser).child("Distance").setValue(Arrays.asList(totalDistance));
                        dbRef.child(firebaseUser).child("Average").setValue(Arrays.asList(Math.round(100*totalDistance/(double)(duration_ms/3600))/100));

                        Log.d("Running Route",""+runningRoute.size());
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    // PEDOMETER METHODS

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
            startService(new Intent(RunningActivity.this,
                    StepService.class));
        }
    }

    // Must bind the service to this context before starting it
    private void bindStepService() {
        Log.i(TAG, "[SERVICE] Bind");
        bindService(new Intent(RunningActivity.this,
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
            stopService(new Intent(RunningActivity.this,
                  StepService.class));
        }
        mIsRunning = false;
    }

    // Reset steps and pace values on screen to 0
    private void resetValues() {
//        mStepValueView.setText("Steps: 0");
//        mPaceValueView.setText("Pace (steps/min): 0");
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
            if (message.what == PACE_MSG) {
                // message.arg1 holds the new value for pace
                // pace readings start from the 5th step taken
                    mPaceValue = message.arg1;

                    // Every time a new pace is received, add to array of pace values.
                    // Every 30 pace values, an average is calculated and compared to the previous
                    // average to determine if we need to change the playback BPM
                    addToPaceArray(mPaceValue);
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
    @SuppressLint("ResourceAsColor")
    private void dynamicTrackSelection(double curTempo) {
        // if current run pace cannot be detected, curTempo is 0, so send toast
        if(curTempo == 0 || curTempo < 0) {
            Toast.makeText(RunningActivity.this, "Could not detect run pace", Toast.LENGTH_SHORT).show();
        } else {    // else, we have gotten the current run pace/tempo
            double minTempo = curTempo - 10;   // lower bound for BPM = target BPM - 10
            // EDGE CASE: if minTempo is negative, set to 0
            if(minTempo < 0) {
                minTempo = 0;
            }
            double maxTempo = curTempo + 10;   // upper bound for BPM = target BPM + 10

            // initialize the new array to hold the tracks filtered by BPM
            filteredTracks = new ArrayList<>();
//            getAllTracks();
            // Loop over the current playlist tracks and add appropriate tempo tracks to filteredTracks
            int numTracks = playlistTracks.size();
            for (int i=0; i<numTracks; i++) {
                double trackTempo = playlistTracks.get(i).getTempo();
                // if current track tempo is > min and < max, add to filteredTracks
                if(trackTempo > minTempo && trackTempo < maxTempo) {
                    filteredTracks.add(playlistTracks.get(i));
                }
            }
            // We have a filteredTracks array with tracks that meet the BPM criteria
            // We select a random track from this array to start playing
            if(filteredTracks.size() > 0) {
                // Select a random song from the filtered tracks array, then play song
                Random rand = new Random();
                // get randomIndex: a random integer between 0 and filteredTracks.size()-1
                int randomIndex = rand.nextInt(filteredTracks.size());
                Track nextTrack = filteredTracks.get(randomIndex);
                playbackService.play(nextTrack);

                // Send a toast notifying the user of track change
                Snackbar snackbar = Snackbar.make(sbTrackProgress, "Track changed to: "+nextTrack.getName()+" ("+nextTrack.getTempo()+" bpm)", Snackbar.LENGTH_SHORT);
                snackbar.getView().setBackgroundColor(R.color.colorPrimaryDark);
                snackbar.show();
//                Toast.makeText(RunningActivity.this, "Track changed to: "+nextTrack.getName()+" ("+nextTrack.getTempo()+" bpm)", Toast.LENGTH_SHORT).show();
            }
            // EDGE CASE: if filteredTracks is empty because no trackTempo matched the curTempo,
            // start playing a random song in the entire playlist
            else {
                Random rand = new Random();
                int index = rand.nextInt(playlistTracks.size());
                Track nextTrack = playlistTracks.get(index);
                playbackService.play(nextTrack);
                // Send a toast notifying the user of track change
                Snackbar snackbar = Snackbar.make(sbTrackProgress, "Track changed to: "+nextTrack.getName()+" ("+nextTrack.getTempo()+" bpm)", Snackbar.LENGTH_SHORT);
                snackbar.getView().setBackgroundColor(R.color.colorPrimaryDark);
                snackbar.show();
//                Toast.makeText(RunningActivity.this, "Track changed to: "+nextTrack.getName()+" ("+nextTrack.getTempo()+" bpm)", Toast.LENGTH_SHORT).show();
            }
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

        Track newTrack = null;
        // use getPlayingTrack to get the currently playing track name
        try {
            playbackService.getPlayingTrack();
             String trackName = sharedPreferences.getString("curTrack","");

            // Loop over playlist tracks to find track by name
            // Set the image URL and tempo of the playing track
            for (int i=0; i<playlistTracks.size(); i++) {
                if(playlistTracks.get(i).getName().equals(trackName)) {
                    newTrack = playlistTracks.get(i);
                    break;
                }
            }
        } catch (Exception e) {
            newTrack = playlistTracks.get(0);
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
            @SuppressLint("ResourceAsColor")
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
                Snackbar snackbar = Snackbar.make(sbTrackProgress, "Track changed to: "+trackName+" ("+trackTempo+" bpm)", Snackbar.LENGTH_SHORT);
                snackbar.getView().setBackgroundColor(R.color.colorPrimaryDark);
                snackbar.show();
//                Toast.makeText(RunningActivity.this, "Track changed to: "+trackName+" ("+trackTempo+" bpm)", Toast.LENGTH_SHORT).show();
            }
        });
    }
    //getting distance between two points using lat/long
    //source: https://www.geeksforgeeks.org/program-distance-two-points-earth/
    public static double distance(double lat1,
                                  double lat2, double lon1,
                                  double lon2)
    {

        // The math module contains a function
        // named toRadians which converts from
        // degrees to radians.
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in kilometers. Use 3956
        //use 6371 for kilmeters
        // for miles
        double r = 3956;

        // calculate the result
        return(c * r);
    }

    //for rounding a double to the second decimal point
    //source: https://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}