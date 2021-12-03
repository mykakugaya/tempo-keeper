package com.example.tempokeeper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class RunStatsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener {
    // TEXTVIEWS DISPLAYING STATS
    private TextView txtDuration;
//    private TextView txtDistance;
    private TextView txtAvgSpeed;
    private TextView txtMaxSpeed;

    // FIREBASE
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String firebaseUser = mAuth.getCurrentUser().getUid();
    private String[] tempLst;
    private TextView txtHistory;
    ArrayList recentRoute;
    String recentDuration;

    private final FirebaseDatabase usersDB = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = usersDB.getReference().child("Users");

    // MAP
    private GoogleMap mMap;
    private PolylineOptions lineOptions;

    private ArrayList<Polyline> routesArray;
    private ArrayList<PolylineOptions> polylineOptArray;
    private ArrayList<LatLng> runningRoute;
    private ArrayList<LatLng> points;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_runstats);

        txtDuration = (TextView) findViewById(R.id.txtRunDuration);
        txtAvgSpeed = (TextView) findViewById(R.id.txtAvgSpeed);
        txtMaxSpeed = (TextView) findViewById(R.id.txtMaxSpeed);

        runningRoute = getIntent().getExtras().getParcelableArrayList("runningRoute");

        // Connect the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFinished);
        mapFragment.getMapAsync(this);

        // show the completed route on map - displayRoute() called in onMapReady()

    }

//    public void getRecentRoute(){
//        DatabaseReference myRef = dbRef.child(firebaseUser);
//        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()){
//                    String temp = String.valueOf(Long.valueOf(snapshot.child("Routes").getChildrenCount()));
//                    int LAST_INDEX = Integer.valueOf(temp)-1;
//                        recentRoute.add(String.valueOf(snapshot.child("Routes").child(String.valueOf(LAST_INDEX)).getValue()));
//                }
//                Log.d("data change", ""+recentRoute.get(0).getClass());
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.d("data change", error.toString());
//            }
//        });
//    }

    public void getRecentTime(){
        DatabaseReference myRef = dbRef.child(firebaseUser);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    String temp = String.valueOf(Long.valueOf(snapshot.child("Time").getChildrenCount()));
                    int LAST_INDEX = Integer.valueOf(temp)-1;
                    if(Integer.valueOf(temp) > 0) {
                        recentDuration = String.valueOf(snapshot.child("Time").child(String.valueOf(LAST_INDEX)).getValue());
                        txtDuration.setText("Duration: "+recentDuration);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    // MAPS
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void displayRoute() {
        //very bad long term bc we dont have runningRoute always
        points = runningRoute;

        PolylineOptions lineOptions = null;
        routesArray = new ArrayList<Polyline>();
        polylineOptArray = new ArrayList<PolylineOptions>();

        lineOptions = new PolylineOptions();

        // color code the polyline based on running speed
        ArrayList<int[]> colorArr = setRouteColors();

        for (int i=0; i+1<points.size(); i++) {
            // Get each consecutive pair of points, set color for line connecting them
            int[] rgbColor = colorArr.get(i);
            int r = rgbColor[0];
            int g = rgbColor[1];
            int b = rgbColor[2];
            Color curColor = Color.valueOf(r,g,b);
            lineOptions.add(points.get(i));
            lineOptions.add(points.get(i+1));
            lineOptions.color(curColor.toArgb());
            lineOptions.width(12);
            lineOptions.geodesic(true);
            lineOptions.clickable(true);
            mMap.addPolyline(lineOptions);

        }

    }

    public ArrayList<int[]> setRouteColors(){
        ArrayList<Double> distances = new ArrayList<>();
        double maxSpeed;
        double minSpeed;
        for (int i = 1;i<points.size();i++){
            distances.add(Math.abs(points.get(i).latitude) - Math.abs(points.get(i-1).latitude) +
                    Math.abs(points.get(i).longitude) - Math.abs(points.get(i-1).longitude));
        }
        maxSpeed = Collections.max(distances);
        // grade = 100
        minSpeed = Collections.min(distances);
        // grade = 0

        // array list of speed proportions
        // proportion = currentSpeed/maxSpeed
        ArrayList<Double> speedArray = new ArrayList<>();

        // Array list of rgb triplets for color coding
        ArrayList<int[]> colorsArray = new ArrayList<>();

        for (int j = 0;j<distances.size();j++){
            // proportion = (currentSpeed-minSpeed)/(maxSpeed-minSpeed)
            Double proportion = (distances.get(j)-minSpeed)/(maxSpeed-minSpeed);
            // add to speed array
            speedArray.add(proportion);

            // set the color of this proportion of the run based on relative speed
            // RED(255,0,0) = 0%
            // YELLOW(255,255,0) = 50%
            // GREEN(0,255,0) = 100%
            int[] rgbArr = new int[3];

            // red to yellow range
            if(proportion < 0.5) {
                int[] slowArr = {255,0,0};
                int secondIndex = (int) (proportion*255);
                slowArr[1] = secondIndex;
                rgbArr = slowArr;
            } else {    // yellow to green range
                int[] fastArr = {0,255,0};
                int firstIndex = (int) (255-(proportion*255));
                fastArr[0] = firstIndex;
                rgbArr = fastArr;
            }
            colorsArray.add(rgbArr);
        }

        // Colors array should now be full of rgb arr values (e.g. {0,255,76})
        return colorsArray;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        //use the intent route ArrayList to display the route of the run that just ended
        displayRoute();

        //retrieve the duration data from the firebase and show on txtView
        getRecentTime();
    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {

    }

//    @SuppressLint("MissingPermission")
//    private void enableMyLocation() {
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
//            mMap.setMyLocationEnabled(true);
//        } else {
//            ActivityCompat.requestPermissions(this, new String[]
//                            {Manifest.permission.ACCESS_FINE_LOCATION},
//                    9000);
//        }
//    }
}
