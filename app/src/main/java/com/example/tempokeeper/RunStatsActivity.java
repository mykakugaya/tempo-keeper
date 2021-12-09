package com.example.tempokeeper;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RunStatsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.SnapshotReadyCallback {
    // TEXTVIEWS DISPLAYING STATS
    private TextView txtDate;
    private TextView txtDuration;
    private TextView txtDistance;
    private TextView txtAvgSpeed;
    private Button btnProfile;


    // FIREBASE
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    private String firebaseUser = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
    private String lastDuration;
    private String lastDate;
    private String lastDistance;
    private String lastAverageSpd;

    private final FirebaseDatabase usersDB = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = usersDB.getReference().child("Users");

    // MAP
    private GoogleMap mMap;
    private PolylineOptions lineOptions;

    private ArrayList<Polyline> routesArray;
    private ArrayList<PolylineOptions> polylineOptArray;
    private ArrayList<LatLng> runningRoute;
    private ArrayList<LatLng> points;

    // Google Static Maps BitMap
    private byte[] imageInByte;
    private String[] imageArray;
    private String imgEncode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_runstats);

        txtDate = (TextView) findViewById(R.id.txtRunDate);
        txtDuration = (TextView) findViewById(R.id.txtRunDuration);
        txtDistance = (TextView) findViewById(R.id.txtDist);
        txtAvgSpeed = (TextView) findViewById(R.id.txtAvgSpeed);
        btnProfile = (Button) findViewById(R.id.btnProfile);

        btnProfile.setEnabled(false);

        // Get the route to display on map
        Bundle bundle = getIntent().getExtras();
        runningRoute = bundle.getParcelableArrayList("runningRoute");

        // Get the rest of the stats
        lastDate = bundle.getString("startDateTime","");
        lastDuration = bundle.getString("runDur","");
        lastDistance = bundle.getString("runDist","");
        lastAverageSpd = bundle.getString("runAvg","");

        // Connect the map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFinished);
        mapFragment.getMapAsync(this);

        // show the completed route on map
        // displayRoute() called in onMapReady(), displays the runningRoute

        // profile button
        btnProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent profileIntent = new Intent(RunStatsActivity.this, ProfileActivity.class);
                startActivity(profileIntent);
            }
        });
    }

    // Get last run information from the database
    public void setRunStats(){
        txtDate.setText(lastDate);
        txtDuration.setText("Duration: "+lastDuration);
        txtDistance.setText("Distance: "+lastDistance+" miles");
        txtAvgSpeed.setText("Avg. speed: "+lastAverageSpd+" MPH");
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // display route on map
        displayRoute();

        // set run stats from intent
        setRunStats();
        // Disables map gestures to ensure that the proper google static map image is saved
        mMap.getUiSettings().setAllGesturesEnabled(false);

        btnProfile.setEnabled(true);
    }


    // Display Colored Routes
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void displayRoute() {
        points = runningRoute; // Arraylist<lat/lng> of the route that the user just ran

        lineOptions = null;
        routesArray = new ArrayList<Polyline>();
        polylineOptArray = new ArrayList<PolylineOptions>();

        lineOptions = new PolylineOptions();

        // color code the polyline based on running speed
        ArrayList<Integer> colorArr = setRouteColors();

        for (int i=0; i+1<points.size(); i++) {
            // Get each consecutive pair of points, set color for line connecting them
            mMap.addPolyline(new PolylineOptions().add(points.get(i)).add(points.get(i+1)).color(colorArr.get(i)).width(12));
        }

        // Show route on map by zooming in
        //marker for origin point
        MarkerOptions originMarker = new MarkerOptions();
        originMarker.position(runningRoute.get(0));
        originMarker.anchor((float) 0.5, (float) 0.5);
        originMarker.title("This is your starting point");
        originMarker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        mMap.addMarker(originMarker);

        //marker for end point
        MarkerOptions endMarker = new MarkerOptions();
        endMarker.position(runningRoute.get(runningRoute.size()-1));
        endMarker.anchor((float) 0.5, (float) 0.5);
        endMarker.title("This is your end point");
        mMap.addMarker(endMarker);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(originMarker.getPosition());
        builder.include(endMarker.getPosition());
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
//        int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen for other screen sizes

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, 300);

        mMap.animateCamera(cu);

    }

    //Returns the ArrayList of RGB arrays for all the legs between the two points on the "points" array
    public ArrayList<Integer> setRouteColors(){
        ArrayList<Double> distances = new ArrayList<>();
        double maxSpeed;
        double minSpeed;

        //iterate over "points" array and calculate the distance between each
        //points and add it to the distance array
        for (int i = 1;i<points.size();i++){
            distances.add(distance(points.get(i).latitude, points.get(i-1).latitude, points.get(i).longitude, points.get(i-1).longitude));
        }
        maxSpeed = Collections.max(distances);
        minSpeed = Collections.min(distances);

        // array list of speed proportions
        // proportion = currentSpeed/maxSpeed
        ArrayList<Double> speedArray = new ArrayList<>();

        // Array list of color ints for color coding
        ArrayList<Integer> colorsArray = new ArrayList<>();

        for (int j = 0;j<distances.size();j++){
            // proportion = (currentSpeed-minSpeed)/(maxSpeed-minSpeed)
            Double proportion = (distances.get(j)-minSpeed)/(maxSpeed-minSpeed);
            // add to speed array
            speedArray.add(proportion);

            // set the color of this proportion of the run based on relative speed
            // red to yellow range
            if(proportion < 0.34) {
                colorsArray.add(Color.RED);
            } else if(proportion < 0.67) {     // yellow to green range
                colorsArray.add(Color.YELLOW);
            } else {
                colorsArray.add(Color.GREEN);
            }
        }

        // Colors array should now be full of color ints
        return colorsArray;
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
        //use 6371 for kilometers
        // for miles
        double r = 3956;

        // calculate the result
        return(c * r);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSnapshotReady(@Nullable Bitmap bitmap) {
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, boas);
        imageInByte = boas.toByteArray();
        imgEncode = Base64.getEncoder().encodeToString(imageInByte);
        saveImgtoDB();
    }


    // add the image snapshot of the Map to the DB
    public void saveImgtoDB(){
        //getting reference to Firebase DB
        DatabaseReference myRef = dbRef.child(firebaseUser);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //if there are other maps already
                if (snapshot.exists()){
                    String temp = String.valueOf(Long.valueOf(snapshot.child("Maps").getChildrenCount()));
                    if (Integer.valueOf(temp)>0) {  // if past runs exist, append this run
                        String[] imgArray = new String[Integer.valueOf(temp) + 1];
                        imageArray = imgArray;

                        // For each run, save the date, duration, distance, average speed
                        for (int i = 0; i < Integer.valueOf(temp); i++) {
                            String x = String.valueOf(i);
                            imageArray[i] = String.valueOf(snapshot.child("Maps").child(x).getValue());
                        }

                        // set new values to firebase database
                        imageArray[imageArray.length-1] = String.valueOf(imgEncode);
                        dbRef.child(firebaseUser).child("Maps").setValue(Arrays.asList(imageArray));
                    }
                    else{   // else, this is the first run completed
                        dbRef.child(firebaseUser).child("Maps").setValue(Arrays.asList(imgEncode));
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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
            Intent routeIntent = new Intent(RunStatsActivity.this, RouteFormActivity.class);
            startActivity(routeIntent);
            return true;
        }

        // go to user profile
        if (id == R.id.menuProfile) {
            Intent profileIntent = new Intent(RunStatsActivity.this, ProfileActivity.class);
            startActivity(profileIntent);
            return true;
        }

        // sign out of app
        if (id == R.id.menuSignOut) {
            FirebaseAuth.getInstance().signOut();

            // user is now signed out, show toast
            Toast.makeText(getBaseContext(), "Signed out.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(RunStatsActivity.this, LoginActivity.class));
            finish();

            return true;
        }

        // default
        return super.onOptionsItemSelected(item);

    }

    @Override
    protected void onPause() {
        // save a bitmap snapshot of the rendered map to db
        mMap.snapshot(this);
        super.onPause();
    }
}
