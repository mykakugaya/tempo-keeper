package com.example.tempokeeper;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tempokeeper.ListAdapters.RunAdapter;
import com.example.tempokeeper.Model.Run;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Objects;

public class ProfileActivity extends AppCompatActivity {

    private Button btnLoadHistory;
    private TextView txtSpotifyUser;
    private TextView txtUserName;
    private TextView txtUserEmail;

    private String spotifyUser;
    private String userName;
    private String userEmail;

    // sharedPreferences to get user's spotify id
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    // Firebase
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String firebaseUser = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

    private final FirebaseDatabase usersDB = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = usersDB.getReference().child("Users");

    // runs array to pass into recycler view of Run History
    private ArrayList<Run> runHistory;

    // lists to store route, duration, and date of each run
    private String[] routeLst;
    private String[] durLst;
    private String[] dateLst;
    private String[] distLst;
    private String[] avgSpdLst;
    private String[] imgLst;

    // recycler view for run history
    private RecyclerView rvHistory;
    private RecyclerView.Adapter runAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        txtSpotifyUser = (TextView) findViewById(R.id.txtUserSpotify);
        txtUserName = (TextView) findViewById(R.id.txtUserName);
        txtUserEmail = (TextView) findViewById(R.id.txtUserEmail);
        btnLoadHistory = (Button) findViewById(R.id.btnReloadHistory);
        rvHistory = (RecyclerView) findViewById(R.id.rvHistory);

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

        // button to sign out
        btnLoadHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setRunHistory();
            }
        });
    }

    // sign out of Firebase
    private void signOut() {
        FirebaseAuth.getInstance().signOut();

        // user is now signed out, show toast
        Toast.makeText(getBaseContext(), "Signed out.", Toast.LENGTH_SHORT).show();
        // go to login activity
        startActivity(new Intent(ProfileActivity.this, LoginActivity.class));
        finish();
    }

    private void setUserInfo() {
        DatabaseReference myRef = dbRef.child(firebaseUser);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    userName = String.valueOf(snapshot.child("name").getValue());
                    userEmail = String.valueOf(snapshot.child("username").getValue());
                    txtUserName.setText(userName);
                    txtUserEmail.setText(userEmail);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

    }

    // gets the run history data from firebase
    private void setRunHistory() {
        // set runHistory array to new array list
        runHistory = new ArrayList<>();
        DatabaseReference myRef = dbRef.child(firebaseUser);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    int numRuns = parseInt(String.valueOf(Long.valueOf(snapshot.child("Routes").getChildrenCount())));
                    if (numRuns>0) {  // if past runs exist, append this run
                        String[] routesArray = new String[numRuns + 1];
                        String[] durationArray = new String[numRuns + 1];
                        String[] dateArray = new String[numRuns + 1];
                        String[] distanceArray = new String[numRuns + 1];
                        String[] avgSpeedArray = new String[numRuns + 1];
                        String[] imageArray = new String[numRuns + 1];

                        //new string arrays to store updated arrays
                        routeLst = routesArray;
                        durLst = durationArray;
                        dateLst = dateArray;
                        distLst = distanceArray;
                        avgSpdLst = avgSpeedArray;
                        imgLst = imageArray;

                        // Get the route, duration, and date of each run
                        for (int i = 0; i < numRuns; i++) {
                            String x = String.valueOf(i);
                            // get route in string form of "(lat,lng),(lat,lng),(lat,lng)"
                            routeLst[i] = String.valueOf(snapshot.child("Routes").child(x).getValue()).replaceAll("lat/lng: ","").replaceAll("\\[", "").replaceAll("\\]", "");
                            durLst[i] = String.valueOf(snapshot.child("Time").child(x).getValue());
                            dateLst[i] = String.valueOf(snapshot.child("Date").child(x).getValue());
                            distLst[i] = String.valueOf(snapshot.child("Distance").child(x).getValue());
                            avgSpdLst[i] = String.valueOf(snapshot.child("Average").child(x).getValue());
                            imgLst[i] = String.valueOf(snapshot.child("Maps").child(x).getValue());

                            // convert string route to an Arraylist<LatLng>
                            ArrayList<LatLng> curRoute = new ArrayList<>();

                            // strRouteArr has the str array form ["(lat,lng)","(lat,lng)","(lat,lng)"]
                            String[] strRouteArr = routeLst[i].split(", ");

                            for (int j=0; j<strRouteArr.length; j++) {
                                // replace the '(' and ',' in each "(lat,lng)" array item
                                String[] latLngPair = strRouteArr[j].replaceAll("\\(", "").replaceAll("\\)","").split(",", 2);
                                if(!latLngPair[0].equals("")&&!latLngPair[1].equals("")) {
                                    // parse float values for lat and lng
                                    float lat = parseFloat(latLngPair[0]);
                                    float lng = parseFloat(latLngPair[1]);
                                    // add new LatLng object to ArrayList<LatLng> curRoute
                                    curRoute.add(new LatLng(lat, lng));
                                }
                            }

                            // create new Run object with index, set all fields
                            Run newRun = new Run(i);
                            newRun.setDate(dateLst[i]);
                            newRun.setDuration(durLst[i]);
                            newRun.setRoute(curRoute);
                            newRun.setDistance(distLst[i]);
                            newRun.setAvgSpeed(avgSpdLst[i]);
                            newRun.setImage(imgLst[i]);

                            // add this Run to runHistory array
                            runHistory.add(newRun);
                        }

                        // set adapter containing the run history to the recycler view
                        runAdapter = new RunAdapter(reverseRunArray(runHistory), ProfileActivity.this);
                        runAdapter.notifyDataSetChanged();
                        rvHistory.setAdapter(runAdapter);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // Takes an arraylist as a parameter and returns
    // a reversed arraylist
    public ArrayList<Run> reverseRunArray(ArrayList<Run> runArray)
    {
        // Arraylist for storing reversed elements
        // this.revArrayList = alist;
        for (int i = 0; i < runArray.size() / 2; i++) {
            Run temp = runArray.get(i);
            runArray.set(i, runArray.get(runArray.size() - i - 1));
            runArray.set(runArray.size() - i - 1, temp);
        }

        // Return the reversed arraylist
        return runArray;
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
            Intent routeIntent = new Intent(ProfileActivity.this, RouteFormActivity.class);
            startActivity(routeIntent);
            return true;
        }

        // already on user profile
        if (id == R.id.menuProfile) {
            return true;
        }

        // sign out of app
        if (id == R.id.menuSignOut) {
            signOut();
            return true;
        }

        // default
        return super.onOptionsItemSelected(item);

    }
}
