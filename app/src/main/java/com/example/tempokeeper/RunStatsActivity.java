package com.example.tempokeeper;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class RunStatsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnPolylineClickListener {
    // TEXTVIEWS DISPLAYING STATS
    private TextView txtDate;
    private TextView txtDuration;
    private TextView txtDistance;
    private TextView txtAvgSpeed;
    private TextView txtMaxSpeed;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_runstats);

        txtDate = (TextView) findViewById(R.id.txtRunDate);
        txtDuration = (TextView) findViewById(R.id.txtRunDuration);
        txtDistance = (TextView) findViewById(R.id.txtDist);
        txtAvgSpeed = (TextView) findViewById(R.id.txtAvgSpeed);
        txtMaxSpeed = (TextView) findViewById(R.id.txtMaxSpeed);
        btnProfile = (Button) findViewById(R.id.btnProfile);

        btnProfile.setEnabled(false);

        // Get the route to display on map
//        runningRoute = new ArrayList<>();
//        getRecentRoute();   // gets runningRoute
        runningRoute = getIntent().getExtras().getParcelableArrayList("runningRoute");

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

    // gets the last ran route from user's database
//    public void getRecentRoute(){
//        DatabaseReference myRef = dbRef.child(firebaseUser);
//        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()){
//                    String temp = String.valueOf(Long.valueOf(snapshot.child("Routes").getChildrenCount()));
//
//                    int LAST_INDEX = Integer.valueOf(temp)-1;
//
//                    // get route in string form of "(lat,lng),(lat,lng),(lat,lng)"
//                    String strLatLng = String.valueOf(snapshot.child("Routes").child(String.valueOf(LAST_INDEX)).getValue()).replaceAll("lat/lng: ","").replaceAll("\\[", "").replaceAll("\\]", "");
//
//                    // convert string route to an Arraylist<LatLng>
//                    ArrayList<LatLng> newArr = new ArrayList<>();
//
//                    // strRouteArr has the str array form ["(lat,lng)","(lat,lng)","(lat,lng)"]
//                    String[] strRouteArr = strLatLng.split(", ");
//
//                    for (int j=0; j<strRouteArr.length; j++) {
//                        // replace the '(' and ',' in each "(lat,lng)" array item
//                        String[] latLngPair = strRouteArr[j].replaceAll("\\(", "").replaceAll("\\)","").split(",", 2);
//                        if(!latLngPair[0].equals("")&&!latLngPair[1].equals("")) {
//                            // parse float values for lat and lng
//                            float lat = parseFloat(latLngPair[0]);
//                            float lng = parseFloat(latLngPair[1]);
//                            // add new LatLng object to ArrayList<LatLng> curRoute
//                            newArr.add(new LatLng(lat, lng));
//                        }
//                    }
//                    runningRoute = newArr;
////                    btnLoadMap.setEnabled(true);
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.d("data change", error.toString());
//            }
//        });
//    }

    // Get last run information from the database
    public void getRunStats(){
        DatabaseReference myRef = dbRef.child(firebaseUser);
        myRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()){
                    int count = parseInt(String.valueOf(Long.valueOf(snapshot.child("Time").getChildrenCount())));
                    int LAST_INDEX = count-1;
                    if(count > 0) {
                        lastDuration = String.valueOf(snapshot.child("Time").child(String.valueOf(LAST_INDEX)).getValue());
                        txtDuration.setText("Duration: "+lastDuration);

                        lastDate = String.valueOf(snapshot.child("Date").child(String.valueOf(LAST_INDEX)).getValue());
                        txtDate.setText(lastDate);

                        lastDistance = String.valueOf(snapshot.child("Distance").child(String.valueOf(LAST_INDEX)).getValue());
                        txtDistance.setText("Distance: "+lastDistance+" miles");

                        lastAverageSpd = String.valueOf(snapshot.child("Average").child(String.valueOf(LAST_INDEX)).getValue());
                        txtAvgSpeed.setText("Average speed: "+lastAverageSpd+" MPH");
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
        points = runningRoute;

        lineOptions = null;
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

        // Show route on map by zooming in
        //marker for origin point
        MarkerOptions originMarker = new MarkerOptions();
        originMarker.position(runningRoute.get(0));
        originMarker.anchor((float) 0.5, (float) 0.5);
        originMarker.title("This is you");
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
        int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, 300);

        mMap.animateCamera(cu);

    }

    public ArrayList<int[]> setRouteColors(){
        ArrayList<Double> distances = new ArrayList<>();
        double maxSpeed;
        double minSpeed;
        for (int i = 1;i<points.size();i++){
            distances.add(distance(points.get(i).latitude, points.get(i-1).latitude, points.get(i).longitude, points.get(i-1).longitude));
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
    //for only using red/yellow/green for the color coating
//    if(proportion < 0.33) {
//        int[] slowArr = {255,0,0};
//        rgbArr = slowArr;
//    } else if(proportion > 0.33 && proportion <0.66) {    // yellow to green range
//        int[] medArr = {255,255,0};
//        rgbArr = medArr;
//    }
//        else{
//        int[] fastArr = {0,255,0};
//        rgbArr = fastArr;
//    }
//        colorsArray.add(rgbArr);
//}
//
//// Colors array should now be full of rgb arr values (e.g. {0,255,76})
//        System.out.println(colorsArray);
//                return colorsArray;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // display route on map
        displayRoute();

        //retrieve the duration data from the firebase and show on txtViews
        getRunStats();

        btnProfile.setEnabled(true);
    }

    @Override
    public void onPolylineClick(@NonNull Polyline polyline) {

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

            // user is now signed out
            Toast.makeText(getBaseContext(), "Signed out.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(RunStatsActivity.this, LoginActivity.class));
            finish();

            return true;
        }

        // default
        return super.onOptionsItemSelected(item);

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
