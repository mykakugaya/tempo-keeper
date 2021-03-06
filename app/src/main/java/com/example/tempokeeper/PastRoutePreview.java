package com.example.tempokeeper;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tempokeeper.Model.Run;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collections;

public class PastRoutePreview extends AppCompatActivity implements OnMapReadyCallback,  GoogleMap.OnPolylineClickListener,
        GoogleMap.OnInfoWindowClickListener{

    private TextView txtDate;
    private TextView txtDist;
    private TextView txtDur;
    private TextView txtAvgSpd;
    private Button btnBack;
    private Button btnRerun;

    private String date;
    private String duration;
    private String distance;
    private String avgSpd;

    // GOOGLE MAPS
    private GoogleMap mMap;
    private PolylineOptions lineOptions;    // send this to RunningActivity
    private ArrayList<LatLng> runningRoute;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_past_preview);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        txtDate = (TextView) findViewById(R.id.txtPastDate);
        txtDur = (TextView) findViewById(R.id.txtPastDur);
        txtDist = (TextView) findViewById(R.id.txtPastDist);
        txtAvgSpd = (TextView) findViewById(R.id.txtPastAvgSpd);
        btnBack = (Button) findViewById(R.id.btnBackProfile);
        btnRerun = (Button) findViewById(R.id.btnRerun);

        // Initialize map fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapPastRoute);
        mapFragment.getMapAsync(PastRoutePreview.this);

        // Get the run statistics from the last running activity
        Bundle bundle = getIntent().getExtras();
        runningRoute = bundle.getParcelableArrayList("pastRoute");

        date = bundle.getString("pastDate");
        txtDate.setText(date);

        duration = bundle.getString("pastDur");
        txtDur.setText("Duration: "+duration);

        distance = bundle.getString("pastDist");
        txtDist.setText("Distance: "+distance+" miles");

        avgSpd = bundle.getString("pastAvgSpd");
        txtAvgSpd.setText("Avg. Speed: "+avgSpd+" MPH");

        btnRerun.setEnabled(false);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent profileIntent = new Intent(PastRoutePreview.this, ProfileActivity.class);
                startActivity(profileIntent);
            }
        });

        btnRerun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent rerunIntent = new Intent(PastRoutePreview.this, PlaylistActivity.class);
                // add extras for the back button in PlaylistAct (comes back to this page)
                rerunIntent.putExtra("pastRoute", runningRoute);
                rerunIntent.putExtra("pastDate",date);
                rerunIntent.putExtra("pastDur",duration);

                rerunIntent.putExtra("chosenRoute", lineOptions);   // map route for RunningAct
                startActivity(rerunIntent);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnInfoWindowClickListener(PastRoutePreview.this);
        displayRoute(runningRoute);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void displayRoute(ArrayList<LatLng> runningRoute) {
        lineOptions = null;
        lineOptions = new PolylineOptions();

        // Commented out code is for future functionality to color-code the past route preview route

        // color code the polyline based on running speed
//        ArrayList<int[]> colorArr = setRouteColors();

//        for (int i=0; i+1<runningRoute.size(); i++) {
//            // Get each consecutive pair of runningRoute, set blue color for line connecting them
//            lineOptions.add(runningRoute.get(i)).add(runningRoute.get(i+1)).color(Color.BLUE).width(12);
//            mMap.addPolyline(lineOptions);
//        }

        // creates the polyline from the saved array of <lat,lng>
        lineOptions.addAll(runningRoute);
        lineOptions.color(Color.BLUE).width(12);
        mMap.addPolyline(lineOptions);

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

        // Sets camera to zoom into the starting and end point
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(originMarker.getPosition());
        builder.include(endMarker.getPosition());
        LatLngBounds bounds = builder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
//        int padding = (int) (width * 0.10); // offset from edges of the map 10% of screen

        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, 300);

        mMap.animateCamera(cu);

        btnRerun.setEnabled(true);
    }

    public ArrayList<int[]> setRouteColors(){
        ArrayList<Double> distances = new ArrayList<>();
        double maxSpeed;
        double minSpeed;
        for (int i = 1;i<runningRoute.size();i++){
            distances.add(Math.abs(runningRoute.get(i).latitude) - Math.abs(runningRoute.get(i-1).latitude) +
                    Math.abs(runningRoute.get(i).longitude) - Math.abs(runningRoute.get(i-1).longitude));
        }
        maxSpeed = Collections.max(distances);
        minSpeed = Collections.min(distances);

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

    @Override
    public void onInfoWindowClick(@NonNull Marker marker) {

    }

    // For future functionality
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
            Intent routeIntent = new Intent(PastRoutePreview.this, RouteFormActivity.class);
            startActivity(routeIntent);
            return true;
        }

        // go to user profile
        if (id == R.id.menuProfile) {
            Intent profileIntent = new Intent(PastRoutePreview.this, ProfileActivity.class);
            startActivity(profileIntent);
            return true;
        }

        // sign out of app
        if (id == R.id.menuSignOut) {
            FirebaseAuth.getInstance().signOut();

            // user is now signed out, show toast
            Toast.makeText(getBaseContext(), "Signed out.", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(PastRoutePreview.this, LoginActivity.class));
            finish();

            return true;
        }

        // default
        return super.onOptionsItemSelected(item);

    }
}
