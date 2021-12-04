package com.example.tempokeeper;

import static java.lang.Integer.parseInt;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tempokeeper.ListAdapters.RunAdapter;
import com.example.tempokeeper.Model.Run;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;

public class ProfileActivity extends AppCompatActivity {

    private Button btnSignOut;
    private TextView txtSpotifyUser;
    private TextView txtUserName;
    private TextView txtUserEmail;

    private String spotifyUser;
    private String userName;
    private String userEmail;

    // sharedPreferences to get user's spotify id
    private SharedPreferences sharedPreferences;

    // Firebase
    private final FirebaseDatabase usersDB = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = usersDB.getReference().child("Users");

    // runs array to pass into recycler view of Run History
    private ArrayList<Run> runHistory;

    // lists to store route, duration, and date of each run
    private String[] routeLst;
    private String[] durLst;
    private String[] dateLst;

    // recycler view for run history
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

        // set the spotify userId
        spotifyUser = sharedPreferences.getString("userId","Spotify User Not Found.");
        txtSpotifyUser.setText(spotifyUser);

        // Set recycler view to have linear layout and no fixed size
        rvHistory.setHasFixedSize(false);
        layoutManager = new LinearLayoutManager(this);
        rvHistory.setLayoutManager(layoutManager);

        // set the user's email and name
        setUserInfo();

        // get run history info from database
        // set the array of Run objects to the run history recycler view
        setRunHistory();

        btnSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FirebaseAuth.getInstance().signOut();

                // user is now signed out
                startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
                finish();
            }
        });
    }

    private void setUserInfo() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()){
                    userName = String.valueOf(snapshot.child("name").child("0").getValue());
                    userEmail = String.valueOf(snapshot.child("username").child("0").getValue());
                    txtUserName.setText(userName);
                    txtUserEmail.setText(userEmail);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }

    // gets the run history data from firebase
    private void setRunHistory() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()){
                    int numRuns = parseInt(String.valueOf(Long.valueOf(snapshot.child("Routes").getChildrenCount())));
                    if (numRuns>0) {  // if past runs exist, append this run
                        String[] routesArray = new String[numRuns + 1];
                        String[] durationArray = new String[numRuns + 1];
                        String[] dateArray = new String[numRuns + 1];
                        routeLst = routesArray;
                        durLst = durationArray;
                        dateLst = dateArray;
                        // Get the route, duration, and date of each run
                        for (int i = 0; i < numRuns; i++) {
                            String x = String.valueOf(i);
                            routeLst[i] = String.valueOf(snapshot.child("Routes").child(x).getValue());
                            durLst[i] = String.valueOf(snapshot.child("Time").child(x).getValue());
                            dateLst[i] = String.valueOf(snapshot.child("Date").child(x).getValue());

                            // create new Run object with index, set all fields
                            Run newRun = new Run(i);
                            newRun.setDate(dateLst[i]);
                            newRun.setDuration(durLst[i]);
//                            newRun.setDistance();
//                            newRun.setAvgSpeed();
//                            newRun.setMaxSpeed();

                            runHistory.add(newRun);
                        }

                        runAdapter = new RunAdapter(runHistory, ProfileActivity.this);
                        runAdapter.notifyDataSetChanged();
                        rvHistory.setAdapter(runAdapter);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });
    }
}
